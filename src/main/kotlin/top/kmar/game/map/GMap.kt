package top.kmar.game.map

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import top.kmar.game.ConsolePrinter
import java.util.stream.Stream

/**
 * 游戏地图，存储和地图相关的所有数据，同时负责地图的打印。
 *
 * 有关打印更多的信息请查阅：[ConsolePrinter]
 *
 * @author 空梦
 */
class GMap(val width: Int, val height: Int) {

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

    fun render() {
        visibleEntity.forEach {
            val graphics = SafeGraphics(it.x, it.y, it.width, it.height, ConsolePrinter.index)
            it.render(graphics)
        }
    }

    /** 每一次游戏循环结束后调用该函数更新地图信息 */
    fun update() {
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

}