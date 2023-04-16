package top.kmar.game.map

import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
import top.kmar.game.utils.GTimer
import top.kmar.game.utils.Point2D
import java.io.Closeable
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BooleanSupplier
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.concurrent.withLock

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
    private val closed = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    /** 每次渲染前的清图操作 */
    var clear: () -> Unit = { ConsolePrinter.quickClear() }

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
    private fun checkCollision(from: GEntity): Stream<GEntity> {
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

    /** 渲染所有实体 */
    fun render() {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        clear()
        entities.lock.withLock {
            visibleEntity.forEach {
                val graphics = SafeGraphics(this, it.x, it.y, it.width, it.height, ConsolePrinter.index)
                it.render(graphics)
            }
        }
        ConsolePrinter.flush()
    }

    private val taskList = ConcurrentLinkedQueue<BooleanSupplier>()

    /**
     * 将一个任务添加到逻辑线程执行。
     *
     * 该函数是线程安全的。
     *
     * @param task 要执行的任务，返回值用于标明是否移除任务，返回 true 会在执行任务后将任务移除，否则下一次逻辑循环将再一次执行该任务
     */
    fun runTaskOnLogicThread(task: BooleanSupplier) {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        taskList.add(task)
    }

    /** 执行使用 [runTaskOnLogicThread] 添加的任务 */
    fun invokeThreadTask() {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        val itor = taskList.iterator()
        while (itor.hasNext()) {
            val item = itor.next()
            if (item.asBoolean) itor.remove()
        }
    }

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
     * 1. [invokeThreadTask]
     * 2. [update]
     * 4. [logicCondition]
     *
     * 该函数会阻塞调用线程，直到逻辑线程和渲染线程执行完毕
     *
     * @param eventInterval 事件监听的时间间隔
     * @param logicInterval 逻辑执行的时间间隔
     * @param logicCondition 判断是否继续执行程序，返回 false 后会终止所有任务并退出当前函数
     */
    fun start(eventInterval: Long, logicInterval: Long, renderInterval: Long, logicCondition: BooleanSupplier) {
        require(eventInterval > 0 && logicInterval > 0 && renderInterval > 0) {
            "eventInterval[$eventInterval]、logicInterval[$logicInterval] 和 renderInterval[$renderInterval] 均应大于 0"
        }
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        val eventTimer = GTimer()
        eventTimer.startNonFixed("Event Thread", eventInterval, true) {
            EventListener.pushButtonEvent()
            EventListener.pushMouseLocationEvent()
        }
        val logicTimer = GTimer()
        logicTimer.start("Logic Thread", logicInterval, false) {
            invokeThreadTask()
            update(it)
            entities.sync()
            if (stopped.get() || !logicCondition.asBoolean) {
                logicTimer.cancel()
            }
        }
        val renderTimer = GTimer()
        renderTimer.startNonFixed("Render Thread", renderInterval, false) {
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

    /**
     * 终止通过 [start] 启动的所有任务
     *
     * 该函数是线程安全的。
     */
    fun interrupt() {
        stopped.set(true)
    }

    /**
     * 更新地图中的信息。
     *
     * 该函数会调用所有存活实体的 [GEntity.update] 函数，并移除所有非存活实体。
     *
     * @param time 距离上一次执行的时间间隔（ms）
     */
    fun update(time: Long) {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        allEntity.forEach { it.update(this, time) }
    }

    /** 关闭当前 map */
    override fun close() {
        stopped.set(true)
        closed.set(true)
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