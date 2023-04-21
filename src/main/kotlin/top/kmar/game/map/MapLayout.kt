package top.kmar.game.map

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.stream.Stream

/**
 * 地图的分层管理器
 * @author 空梦
 */
class MapLayout(private val source: GMap) {

    private val map = Int2ObjectRBTreeMap<MutableCollection<GEntity>>()
    private val keyList = ConcurrentSkipListSet<Int>()

    val allEntities: Stream<GEntity>
        get() = keyList.stream().flatMap { map[it].stream() }.filter { !it.died }
    val visibleEntities: Stream<GEntity>
        get() = allEntities.filter { it.visible }
    val collisibleEntities: Stream<GEntity>
        get() = allEntities.filter { it.collisible }

    fun sync() {
        keyList.forEach { layer -> map[layer].removeIf { it.died } }
    }

    /** 添加一个元素到地图中 */
    fun add(entity: GEntity, layer: Int) {
        val list = if (map.containsKey(layer)) map[layer] else ConcurrentLinkedQueue<GEntity>().apply { map.put(layer, this) }
        list.add(entity)
        keyList.add(layer)
    }

}