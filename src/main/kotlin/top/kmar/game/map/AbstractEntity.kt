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
        protected set
    final override var collisible = collisible
        protected set
    final override var x = x
        protected set
    final override var y = y
        protected set
    final override var width = width
        protected set
    final override var height = height
        protected set
    @Volatile
    final override var died = false
        protected set

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }

}