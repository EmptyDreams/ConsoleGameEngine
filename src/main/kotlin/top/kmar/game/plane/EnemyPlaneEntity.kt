package top.kmar.game.plane

import top.kmar.game.map.GEntity
import top.kmar.game.map.GMap
import top.kmar.game.map.SafeGraphics
import top.kmar.game.utils.Point2D
import java.util.stream.Stream

open class EnemyPlaneEntity(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    private var blood: Int
) : GEntity {

    override val collisible = true
    override var died = false

    override fun render(graphics: SafeGraphics) {
        for (y in 0 until height) {
            graphics.fillRect('⊕', y, y, (height - y) shl 1, 1)
        }
    }

    override fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Point2D> {
        val builder = Stream.builder<Point2D>()
        val bottom = y + height
        val right = x + width
        for (k in y until bottom) {
            for (i in x until (right - k.shl(1)).coerceAtMost(right)) {
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
        map.checkCollision(this)
            .forEach { onCollision(map, it) }
    }

    override fun onCollision(map: GMap, that: GEntity) {
        when (that) {
            is PlayerPlane -> {
                died = true
                --that.blood
            }
            is BulletEntity -> {
                blood -= that.power
                if (blood <= 0)
                    beKilled(map, that.owner)
                that.beKilled(map, this)
            }
        }
    }

    override fun beKilled(map: GMap, killer: GEntity) {
        died = true
        if (killer is PlayerPlane) {
            ++killer.score
        }
    }

    override fun copy() = EnemyPlaneEntity(x, y, width, height, blood)

}