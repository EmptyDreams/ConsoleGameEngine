package top.kmar.game.plane

import top.kmar.game.map.GEntity
import top.kmar.game.map.GMap
import top.kmar.game.map.Point2D
import top.kmar.game.map.SafeGraphics
import java.util.stream.Stream

class PlayerPlane(
    override var x: Int,
    override var y: Int
) : GEntity {

    override val collisible = true
    override var died = false
    override val width = 11
    override val height = 9

    override fun render(graphics: SafeGraphics) {
        graphics.fillRect('@', width.shr(1) or 1, 0, 1, 1)
        graphics.fillRect('#', width.shr(1) - 1, 1, 3, 1)
        graphics.fillRect('#', width.shr(1) - 2, 2, 2, 2)
        graphics.fillRect('#', width.shr(1) + 1, 2, 2, 2)
        graphics.fillRect('A', 1, 3, 1, 1)
        graphics.fillRect('A', width - 2, 3, 1, 1)
        graphics.fillRect('#', 0, 4, width, 1)
        graphics.fillRect('#', 1, 5, 3, 1)
        graphics.fillRect('#', width - 4, 5, 3, 1)
        graphics.fillRect('#', 0, 6, 3, 1)
        graphics.fillRect('#', width - 3, 6, 3, 1)
        graphics.fillRect('#', 1, 7, 3, 1)
        graphics.fillRect('#', width - 4, 7, 3, 1)
        graphics.fillRect('#', 2, 8, 1, 1)
        graphics.fillRect('#', width - 3, 8, 1, 1)
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()

        return builder.build()
    }

    override fun update(map: GMap, time: Long) {
        val bulletLeft = BulletEntity(x + 1, y + 2, 1, 1)
        val bulletRight = BulletEntity(right - 1, y + 2, 1, 1)
        map.runTaskOnLogicThread {
            map.putEntity(bulletLeft, 0)
            map.putEntity(bulletRight, 0)
            true
        }
    }

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }

    override fun copy() = PlayerPlane(x, y)

}