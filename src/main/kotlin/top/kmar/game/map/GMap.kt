package top.kmar.game.map

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BooleanSupplier
import java.util.stream.Stream

/**
 * 游戏地图，存储和地图相关的所有数据，同时负责地图的打印。
 *
 * 有关打印更多的信息请查阅：[ConsolePrinter]
 *
 * @author 空梦
 */
class GMap(
    /** 横向字符数量 */
    val width: Int,
    /** 纵向字符数量 */
    val height: Int,
    /** 字符宽度（宽高比为 1:2） */
    fontWidth: Int,
    /** 缓存数量 */
    cache: Int,
    /** 是否忽略 ctrl + C 等快捷键 */
    ignoreClose: Boolean,
    /** DLL 文件路径 */
    file: File = File("utils.dll")
) {

    init {
        ConsolePrinter.init(width, height, fontWidth, cache, ignoreClose, file)
    }

    private val entities = Int2ObjectRBTreeMap<MutableSet<GEntity>>()

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
        taskList.add(task)
    }

    /** 执行使用 [runTaskOnLogicThread] 添加的任务 */
    fun invokeThreadTask() {
        val itor = taskList.iterator()
        while (itor.hasNext()) {
            val item = itor.next()
            if (item.asBoolean) itor.remove()
        }
    }

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
        if (prev != 0L) throw AssertionError("不应该重复启动时序控制")
        val timer = Timer("Event Listener Thread", true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                EventListener.pushKeyboardEvent()
            }
        }, eventInterval, eventInterval)
        Thread.currentThread().name = "Logic Thread"
        prev = System.currentTimeMillis()
        var offset = 0L     // 偏移量，用于修复等待时间不正确时的情况
        val sleepBound = logicInterval - 2
        while (true) {
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
            prev = now
            invokeThreadTask()
            update(time)
            render()
            if (!logicCondition.asBoolean) {
                timer.cancel()
                break
            }
        }
    }

    private var prev = 0L

    /**
     * 更新地图中的信息。
     *
     * 该函数会调用所有存活实体的 [GEntity.update] 函数，并移除所有非存活实体。
     *
     * @param time 距离上一次执行的时间间隔（ms）
     */
    fun update(time: Long) {
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

    /** 销毁控制台 */
    fun dispose() {
        ConsolePrinter.dispose()
    }

    fun finalize() {
        println("1")
        dispose()
    }

}