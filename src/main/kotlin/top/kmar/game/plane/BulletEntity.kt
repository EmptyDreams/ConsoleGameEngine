package top.kmar.game.plane

import top.kmar.game.map.GEntity
import top.kmar.game.map.GMap
import top.kmar.game.map.Point2D
import top.kmar.game.map.SafeGraphics
import java.util.stream.Stream

class BulletEntity(
    val owner: GEntity,
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    val power: Int,
    val offset: Int = -1
) : GEntity {

    override val collisible = true
    override var died = false

    override fun render(graphics: SafeGraphics) {
        graphics.fillRect('O', 0, 0, width, height)
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()
        val right = x + width
        val bottom = y + height
        for (k in y until bottom) {
            for (i in 0 until right) {
                builder.add(Point2D(i, k))
            }
        }
        return builder.build()
    }

    override fun update(map: GMap, time: Long) {
        y += offset
        if (y < 0 || y >= map.height) died = true
    }

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }

    override fun copy() = BulletEntity(owner, x, y, width, height, power)

}