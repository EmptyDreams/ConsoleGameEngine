package top.kmar.game.plane

import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
import top.kmar.game.listener.IMousePosListener
import top.kmar.game.map.GMap
import java.io.File
import java.util.*

fun main() {
    GMap.Builder.apply {
        width = 100
        height = 60
        fontWidth = 5
        cache = 2
        file = File("utils.dll")
        println(file!!.absolutePath)
    }.build().use {
        val player = PlayerPlane((it.width - 11) shr 1, it.height - 9, 10)
        EventListener.registryMousePosEvent(object : IMousePosListener {
            override fun onMove(x: Int, y: Int, oldX: Int, oldY: Int) {
                player.x = (x - 5).coerceAtLeast(0).coerceAtMost(it.width - player.width)
                player.y = (y - 4).coerceAtLeast(0).coerceAtMost(it.height - player.height)
            }
        })
        var maxTime = 70
        var maxBlood = 50
        var time = 0
        val random = Random()
        it.runTaskOnLogicThread {
            if (++time >= maxTime) {
                time = 0
                val x = random.nextInt(it.width - 9)
                it.putEntity(EnemyPlaneEntity(x, -5, 9, 5, random.nextInt(25, maxBlood)), 0)
                if (maxTime != 40 && random.nextBoolean())
                    --maxTime
                if (maxBlood != 500 && random.nextBoolean())
                    ++maxBlood
            }
            false
        }
        it.clear = {
            ConsolePrinter.quickClearAllChar()
            ConsolePrinter.drawString("玩家分数：${player.score}", 0, 0)
            ConsolePrinter.drawString("玩家血量: ${player.blood}", 0, 1)
            ConsolePrinter.drawString("生成间隔: $maxTime ms", 0, 2)
            ConsolePrinter.drawString("最大血量: $maxBlood", 0, 3)
        }
        it.putEntity(player, 1)
        it.start(5, 20) { true }
    }
    GMap.Builder.dispose()
}