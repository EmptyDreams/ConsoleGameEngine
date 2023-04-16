package top.kmar.game.map

/**
 * 内置了一些实现的实体类
 * @author 空梦
 */
abstract class AbstractEntity(
    x: Int, y: Int, width: Int, height: Int,
    visible: Boolean, collisible: Boolean
) : GEntity {

    @Volatile
    final override var visible = visible
        private set
    final override var collisible = collisible
        private set
    final override var x = x
        private set
    final override var y = y
        private set
    final override var width = width
        private set
    final override var height = height
        private set
    @Volatile
    final override var died = false
        private set

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }

}