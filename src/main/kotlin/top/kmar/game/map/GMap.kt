package top.kmar.game.map

import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
import top.kmar.game.utils.GTimer
import top.kmar.game.utils.Point2D
import top.kmar.game.utils.TaskManager
import top.kmar.game.utils.updateWith
import java.io.Closeable
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BooleanSupplier
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * 游戏地图，存储和地图相关的所有数据，同时负责地图的打印。
 *
 * 有关打印更多的信息请查阅：[ConsolePrinter]
 *
 * @author 空梦
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class GMap private constructor(
    /** 横向字符数量 */
    val width: Int,
    /** 纵向字符数量 */
    val height: Int
) : Closeable {

    private val entities = MapLayout(this)
    private val taskManager = TaskManager(5)
    private val reusableTaskManager = TaskManager(5)
    @Volatile
    private var closed = false
    @Volatile
    private var stopped = false
    /** 每次渲染前的清图操作 */
    var clear: () -> Unit = { ConsolePrinter.quickClear() }
    @Volatile
    var fps = 0
        private set

    /** 所有实体 */
    val allEntity: Stream<GEntity>
        get() = entities.allEntities
    /** 所有可视实体 */
    val visibleEntity: Stream<GEntity>
        get() = entities.visibleEntities
    /** 所有可碰撞实体 */
    val collisibleEntity: Stream<GEntity>
        get() = entities.collisibleEntities

    /** 检查指定实体与地图中其它实体是否存在碰撞 */
    fun checkCollision(from: GEntity): Stream<GEntity> {
        if (!from.collisible) return Stream.empty()
        val bound = from.bound
        val collision = from.getCollision(0, 0, from.width, from.height)
            .map { Point2D(it.x + from.x, it.y + from.y) }
            .collect(Collectors.toSet())
        return collisibleEntity
            .filter { it != from }
            .filter { entity ->
                val itBound = entity.bound
                if (!bound.hasIntersection(itBound)) return@filter false
                val rect = bound.intersect(entity.bound).mapToEntity(entity)
                entity.getCollision(rect)
                    .map { Point2D(it.x + entity.x, it.y + entity.y) }
                    .anyMatch { it in collision }
            }
    }

    /** 放置一个实体 */
    fun putEntity(entity: GEntity, layout: Int) {
        entities.add(entity, layout)
    }

    private var renderLastTime = 0L
    private var frameCount = 0

    /** 渲染所有实体 */
    fun render() {
        require(!closed) { "当前 GMap 已经被关闭，无法执行动作" }
        clear()
        taskManager.runTaskList(BEFORE_RENDER)
        reusableTaskManager.runTaskListNoRemove(BEFORE_RENDER)
        visibleEntity.forEach {
            val graphics = SafeGraphics(this, it.x, it.y, it.width, it.height, ConsolePrinter.index)
            it.render(graphics)
        }
        taskManager.runTaskList(AFTER_RENDER)
        reusableTaskManager.runTaskListNoRemove(AFTER_RENDER)
        ConsolePrinter.flush()
        ++frameCount
        val time = System.currentTimeMillis()
        if (time - renderLastTime > 1000) {
            fps = frameCount
            renderLastTime = time
            frameCount = 0
        }
    }

    /**
     * 添加一个普通任务
     * @param flag 执行的时机
     * @param task 要执行的任务
     */
    fun appendTask(flag: Int, task: Runnable) {
        taskManager.add(flag, task)
    }

    /**
     * 添加一个循环任务（执行后不会被删除）
     * @param flag 执行的时机
     * @param task 要执行的任务
     */
    fun appendReusableTask(flag: Int, task: Runnable) {
        reusableTaskManager.add(flag, task)
    }

    /** 删除一个循环任务 */
    fun removeReusableTask(flag: Int, task: Runnable) {
        reusableTaskManager.removeTask(flag, task)
    }

    private val pauseFlag = AtomicInteger()

    /**
     * 让引擎接管所有时序控制。
     *
     * 该函数会启动三个线程，分别为：
     *
     * 1. 逻辑线程 - 用于执行逻辑任务
     * 2. 渲染线程 - 用于执行渲染任务
     * 3. 事件线程 - 用于监听和发布事件
     *
     * 每一次逻辑循环执行顺序如下（调用该函数后，用户不应当再手动调用下列函数）：
     *
     * 1. [update]
     * 2. [logicCondition]
     *
     * 该函数会阻塞调用线程，直到逻辑线程和渲染线程执行完毕
     *
     * @param eventInterval 事件监听的时间间隔
     * @param logicInterval 逻辑执行的时间间隔
     * @param renderInterval 渲染执行时间间隔
     * @param logicCondition 判断是否继续执行程序，返回 false 后会终止所有任务并退出当前函数
     */
    fun start(eventInterval: Long, logicInterval: Long, renderInterval: Long, logicCondition: BooleanSupplier) {
        require(eventInterval >= 0 && logicInterval >= 0 && renderInterval >= 0) {
            "eventInterval[$eventInterval]、logicInterval[$logicInterval] 和 renderInterval[$renderInterval] 均应大于 0"
        }
        require(!closed) { "当前 GMap 已经被关闭，无法执行动作" }
        require(!runFlag) { "不允许重复启动内置的逻辑控制" }
        runFlag = true
        val eventTimer = GTimer()
        eventTimer.startNonFixed("Event Thread", eventInterval, true) {
            if (pauseFlag.get() and PAUSE_EVENT != 0) return@startNonFixed
            EventListener.pushButtonEvent()
            EventListener.pushMouseLocationEvent()
        }
        val logicTimer = GTimer()
        logicTimer.start("Logic Thread", logicInterval, false) logic@{
            if (pauseFlag.get() and PAUSE_LOGIC != 0) return@logic
            update(it)
            entities.sync()
            if (stopped || !logicCondition.asBoolean) {
                logicTimer.cancel()
            } else {
                taskManager.runTaskList(AFTER_LOGIC)
                reusableTaskManager.runTaskListNoRemove(AFTER_LOGIC)
            }
        }
        val renderTimer = GTimer()
        renderTimer.start("Render Thread", renderInterval, false) render@{
            if (pauseFlag.get() and PAUSE_RENDER != 0) return@render
            render()
        }
        while (true) {
            Thread.sleep(1000)
            if (!(logicTimer.alive && renderTimer.alive)) {
                logicTimer.cancel()
                renderTimer.cancel()
                eventTimer.cancel()
                break
            }
        }
    }

    /** 暂停所有线程（线程安全） */
    fun pauseAll() {
        pauseFlag.updateWith { it or PAUSE_LOGIC or PAUSE_EVENT or PAUSE_RENDER }
    }

    /** 继续执行所有线程（线程安全） */
    fun continueAll() {
        pauseFlag.updateWith { it and (PAUSE_LOGIC or PAUSE_EVENT or PAUSE_RENDER).inv() }
    }

    /** 暂停逻辑线程（线程安全） */
    fun pauseLogic() {
        pauseFlag.updateWith { it or PAUSE_LOGIC }
    }

    /** 继续执行逻辑线程（线程安全） */
    fun continueLogic() {
        pauseFlag.updateWith { it and PAUSE_LOGIC.inv() }
    }

    /** 暂停渲染线程（线程安全） */
    fun pauseRender() {
        pauseFlag.updateWith { it or PAUSE_RENDER }
    }

    /** 继续执行渲染线程（线程安全） */
    fun continueRender() {
        pauseFlag.updateWith { it and PAUSE_RENDER.inv() }
    }

    /** 暂停事件线程（线程安全） */
    fun pauseEvent() {
        pauseFlag.updateWith { it or PAUSE_EVENT }
    }

    /** 继续执行事件线程（线程安全） */
    fun continueEvent() {
        pauseFlag.updateWith { it and PAUSE_EVENT.inv() }
    }

    /**
     * 终止通过 [start] 启动的所有任务
     *
     * 该函数是线程安全的。
     */
    fun interrupt() {
        stopped = true
    }

    /**
     * 更新地图中的信息。
     *
     * 该函数会调用所有存活实体的 [GEntity.update] 函数，并移除所有非存活实体。
     *
     * @param time 距离上一次执行的时间间隔（ms）
     */
    fun update(time: Long) {
        require(!closed) { "当前 GMap 已经被关闭，无法执行动作" }
        taskManager.runTaskList(BEFORE_UPDATE)
        reusableTaskManager.runTaskListNoRemove(BEFORE_UPDATE)
        allEntity.forEach { it.update(this, time) }
        taskManager.runTaskList(AFTER_UPDATE)
        reusableTaskManager.runTaskListNoRemove(AFTER_UPDATE)
    }

    /** 关闭当前 map */
    override fun close() {
        stopped = true
        closed = true
        val itor = Builder.list.iterator()
        while (itor.hasNext()) {
            val item = itor.next().get()
            if (item == null) itor.remove()
            else if (item == this) {
                itor.remove()
                break
            }
        }
    }

    companion object {

        /** [update] 执行前调用 */
        const val BEFORE_UPDATE = 0
        /** [update] 执行后调用 */
        const val AFTER_UPDATE = 1
        /** [render] 执行前调用（在渲染线程执行） */
        const val BEFORE_RENDER = 2
        /** [render] 执行后调用（在渲染线程执行） */
        const val AFTER_RENDER = 3
        /** 在整个逻辑循环执行完毕并确定继续下一次逻辑循环后调用 */
        const val AFTER_LOGIC = 4

        private const val PAUSE_LOGIC = 0b1
        private const val PAUSE_EVENT = 0b10
        private const val PAUSE_RENDER = 0b100

        @JvmStatic
        @Volatile
        var runFlag = false
            private set

    }

    /**
     * [GMap] 的构造器
     */
    object Builder {

        /** 横向格数 */
        @JvmStatic
        var width: Int = -1
            set(value) {
                require(field == -1) { "不能重复初始化属性" }
                require(value > 0) { "宽度应该大于 0" }
                field = value
            }
        /** 纵向格数 */
        @JvmStatic
        var height: Int = -1
            set(value) {
                require(field == -1) { "不能重复初始化属性" }
                require(value > 0) { "高度应该大于 0" }
                field = value
            }
        /** 字体宽度 */
        @JvmStatic
        var fontWidth: Int = -1
            set(value) {
                require(field == -1) { "不能重复初始化属性" }
                require(value > 0) { "宽度应该大于 0" }
                field = value
            }
        /** 缓存数量 */
        @JvmStatic
        var cache: Int = -1
            set(value) {
                require(field == -1) { "不能重复初始化属性" }
                require(value > 0) { "缓存数量应该大于 0" }
                field = value
            }
        /** 是否忽略控制键 */
        @JvmStatic
        var ignoreClose: Boolean? = null
            set(value) {
                require(field == null) { "不能重复初始化属性" }
                require(value != null) { "值不应当为 NULL" }
                field = value
            }
        /** DLL 路径 */
        @JvmStatic
        var file: File? = null
            set(value) {
                require(field == null) { "不能重复初始化属性" }
                require(value != null) { "值不应当为 NULL" }
                field = value
            }

        @JvmStatic
        private var flag = true
        @JvmStatic
        internal val list = LinkedList<WeakReference<GMap>>()

        @JvmStatic
        fun build(): GMap {
            require(width != -1) { "宽度属性未初始化" }
            require(height != -1) { "高度属性未初始化" }
            if (fontWidth == -1) fontWidth = 10
            if (cache == -10) cache = 2
            if (ignoreClose == null) ignoreClose = false
            if (file == null) file = File("utils.dll")
            if (flag) {
                flag = false
                ConsolePrinter.init(width, height, fontWidth, cache, ignoreClose!!, file!!)
            }
            list.removeIf { it.get() == null }
            val map = GMap(width, height)
            list.add(WeakReference(map))
            return map
        }

        /**
         * 销毁控制台，只有当所有 map 都被虚拟机回收或 close 后可以进行销毁
         * @return 是否销毁成功
         */
        @JvmStatic
        fun dispose(): Boolean {
            list.removeIf { it.get() == null }
            return if (list.isEmpty()) {
                ConsolePrinter.dispose()
                flag = true
                true
            } else {
                System.gc()
                list.removeIf { it.get() == null }
                if (list.isEmpty()) {
                    ConsolePrinter.dispose()
                    flag = true
                    true
                } else false
            }
        }

    }

    /**
     * [Builder] 的 Java 优化版本
     */
    internal object BuilderJava {

        fun setWidth(value: Int): BuilderJava {
            Builder.width = value
            return this
        }

        fun setHeight(value: Int): BuilderJava {
            Builder.height = value
            return this
        }

        fun setCache(value: Int): BuilderJava {
            Builder.cache = value
            return this
        }

        fun setFontWidth(value: Int): BuilderJava {
            Builder.fontWidth = value
            return this
        }

        fun setIgnoreClose(value: Boolean): BuilderJava {
            Builder.ignoreClose = value
            return this
        }

        fun setFile(file: File): BuilderJava {
            Builder.file = file
            return this
        }

        fun build(): GMap = Builder.build()

        @JvmStatic
        fun dispose() = Builder.dispose()

    }

}