package top.kmar.game.map

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Stream
import kotlin.concurrent.withLock

/**
 * 地图的分层管理器
 * @author 空梦
 */
class MapLayout(private val source: GMap) {

    private val map = Int2ObjectRBTreeMap<MutableCollection<GEntity>>()
    private val addList = LinkedList<AddedItem>()
    val lock = ReentrantLock()

    val allEntities: Stream<GEntity>
        get() = map.values.stream().flatMap { it.stream() }.filter { !it.died }
    val visibleEntities: Stream<GEntity>
        get() = allEntities.filter { it.visible }
    val collisibleEntities: Stream<GEntity>
        get() = allEntities.filter { it.collisible }

    fun sync() {
        lock.withLock {
            map.values.forEach { list ->
                val itor = list.iterator()
                while (itor.hasNext()) {
                    val it = itor.next()
                    if (it.died) {
                        itor.remove()
                        source.runTaskOnLogicThread {
                            it.onRemove(source)
                            false
                        }
                    }
                }
            }
            addList.forEach {
                val list = map.getOrPut(it.layer) { LinkedList() }
                list.add(it.value)
                source.runTaskOnLogicThread {
                    it.value.onGenerate(source)
                    false
                }
            }
            addList.clear()
        }
    }

    /** 添加一个元素到地图中 */
    fun add(entity: GEntity, layer: Int) {
        addList.add(AddedItem(entity, layer))
    }

    private data class AddedItem(
        val value: GEntity,
        val layer: Int
    )

}