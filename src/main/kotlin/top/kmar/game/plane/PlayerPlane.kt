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
    override val width = 7
    override val height = 9

    override fun render(graphics: SafeGraphics) {
        graphics.fillRect('@', width shr 1, 0, 1, 1)
        for (y in 1 until 4) {
            graphics.fillRect('#', (width shr 1) - y, y, (y shl 1) or 1, 1)
        }
        graphics.fillRect('#', 1, 4, 5, 1)
        graphics.fillRect('#', 2, 5, 3, 1)
        for (i in 0 until 3) {
            graphics.fillRect('#', (width shr 1) + i, 6 + i, (i shl 1) or 1, 1)
        }
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()

        return builder.build()
    }

    override fun update(map: GMap, time: Long) {

    }

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
    }

    override fun copy() = PlayerPlane(x, y)

}