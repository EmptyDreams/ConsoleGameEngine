package top.kmar.game.map

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
import java.io.Closeable
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BooleanSupplier
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

    private val entities = Int2ObjectRBTreeMap<MutableSet<GEntity>>()
    private val closed = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)

    /** 所有实体 */
    val allEntity: Stream<GEntity>
        get() = entities.values.stream().flatMap { it.stream() }.filter { !it.died }
    /** 所有可视实体 */
    val visibleEntity: Stream<GEntity>
        get() = allEntity.filter { it.visible }
    /** 所有可碰撞实体 */
    val collisibleEntity: Stream<GEntity>
        get() = allEntity.filter { it.collisible }

    /** 检查指定实体与地图中其它实体是否存在碰撞 */
    private fun checkCollision(from: GEntity): Stream<GEntity> {
        if (!from.collisible) return Stream.empty()
        return collisibleEntity
            .filter { it != from && from.hasIntersect(it) && from.checkCollision(it) }
    }

    /** 放置一个实体 */
    fun putEntity(entity: GEntity, layout: Int) {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        val list = entities.getOrPut(layout) { ObjectOpenHashSet(width * height) }
        list.add(entity)
        entity.onGenerate(this)
        checkCollision(entity)
            .forEach {
                it.onCollision(this, entity)
                entity.onCollision(this, it)
            }
    }

    /** 渲染所有实体 */
    fun render() {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        visibleEntity.forEach {
            val graphics = SafeGraphics(this, it.x, it.y, it.width, it.height, ConsolePrinter.index)
            it.render(graphics)
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

    private var prev = AtomicLong(0L)

    /**
     * 让引擎接管所有时序控制。
     *
     * 事件监听的内容将在另一个线程中执行，逻辑内容在当前线程执行，所以该函数仍然会阻塞调用线程。
     *
     * 每一次逻辑循环执行顺序如下（调用该函数后，用户不应当再手动调用下列函数）：
     *
     * 1. [invokeThreadTask]
     * 2. [update]
     * 3. [render]
     *
     * @param eventInterval 事件监听的时间间隔
     * @param logicInterval 逻辑执行的时间间隔
     * @param logicCondition 判断是否继续执行程序，返回 false 后会终止所有任务并退出当前函数
     */
    fun start(eventInterval: Long, logicInterval: Long, logicCondition: BooleanSupplier) {
        require(!closed.get()) { "当前 GMap 已经被关闭，无法执行动作" }
        if (prev.get() != 0L) throw AssertionError("不应该重复启动时序控制")
        val timer = Timer("Event Listener Thread", true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                EventListener.pushEvent()
            }
        }, eventInterval, eventInterval)
        Thread.currentThread().name = "Logic Thread"
        prev.set(System.currentTimeMillis())
        var offset = 0L     // 偏移量，用于修复等待时间不正确时的情况
        val sleepBound = logicInterval - 2
        while (true) {
            val prev = prev.get()
            var now = System.currentTimeMillis()
            var time = now - prev + offset
            // sleep 等待
            while (time < sleepBound) {
                Thread.sleep(sleepBound - time)
                now = System.currentTimeMillis()
                time = now - prev + offset
            }
            // 自旋等待
            while (time < logicInterval) {
                now = System.currentTimeMillis()
                time = now - prev + offset
            }
            time = now - prev
            offset += time - logicInterval
            this.prev.set(now)
            invokeThreadTask()
            update(time)
            render()
            if (stopped.get() || !logicCondition.asBoolean) {
                timer.cancel()
                break
            }
        }
        prev.set(0L)
    }

    /**
     * 终止通过 [start] 启动的所有任务
     *
     * 该函数是线程安全的。
     */
    fun stop() {
        require(prev.get() != 0L) { "任务未启动" }
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
        entities.values.forEach { list ->
            val itor = list.iterator()
            while (itor.hasNext()) {
                val item = itor.next()
                if (item.died) {
                    itor.remove()
                    item.onRemove(this)
                }
            }
        }
    }

    /** 关闭当前 map */
    override fun close() {
        require(prev.get() == 0L) { "GMap 的逻辑正在进行，无法关闭" }
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
                require(field != null) { "值不应当为 NULL" }
                field = value
            }
        /** DLL 路径 */
        @JvmStatic
        var file: File? = null
            set(value) {
                require(field == null) { "不能重复初始化属性" }
                require(field != null) { "值不应当为 NULL" }
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
            if (fontWidth == -1) fontWidth = 6
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
                true
            } else {
                System.gc()
                list.removeIf { it.get() == null }
                if (list.isEmpty()) {
                    ConsolePrinter.dispose()
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