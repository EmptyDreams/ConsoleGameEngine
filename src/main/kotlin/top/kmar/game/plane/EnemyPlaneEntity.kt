package top.kmar.game.plane

import top.kmar.game.map.GEntity
import top.kmar.game.map.GMap
import top.kmar.game.map.Point2D
import top.kmar.game.map.SafeGraphics
import java.util.stream.Stream

class EnemyPlaneEntity(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    var blood: Int
) : GEntity {

    override val collisible = true
    override var died = false

    override fun render(graphics: SafeGraphics) {
        for (y in 0 until height) {
            graphics.fillRect('⊕', y, y, (height - y - 1).shl(1) or 1, 1)
        }
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()
        val bottom = y + height
        val right = x + width
        for (k in y until bottom) {
            for (i in x until (k shl 1).or(1).coerceAtMost(right)) {
                builder.add(Point2D(k + i, k))
            }
        }
        return builder.build()
    }

    private var timer = 0

    override fun update(map: GMap, time: Long) {
        if (blood == 0) died = true
        else if (++timer == 10) {
            timer = 0
            if (++y == map.height) died = true
        }
    }

    override fun beKilled(map: GMap, killer: GEntity) {

    }

    override fun copy() = EnemyPlaneEntity(x, y, width, height, blood)

}