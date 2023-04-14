package top.kmar.game.plane

import top.kmar.game.EventListener
import top.kmar.game.listener.IMousePosListener
import top.kmar.game.map.GMap
import java.io.File

fun main() {
    GMap.Builder.apply {
        width = 100
        height = 60
        fontWidth = 5
        cache = 2
        file = File("utils.dll")
        println(file!!.absolutePath)
    }.build().use {
        val player = PlayerPlane((it.width - 11) shr 1, it.height - 9)
        EventListener.registryMousePosEvent(object : IMousePosListener {
            override fun onMove(x: Int, y: Int, oldX: Int, oldY: Int) {
                player.x = (x - 5).coerceAtLeast(0).coerceAtMost(it.width - player.width)
                player.y = (y - 4).coerceAtLeast(0).coerceAtMost(it.height - player.height)
            }
        })
        it.putEntity(player, 1)
        it.start(5, 20) { true }
    }
    GMap.Builder.dispose()
}