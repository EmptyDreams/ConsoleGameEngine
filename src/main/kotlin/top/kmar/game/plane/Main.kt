package top.kmar.game.plane

import top.kmar.game.ConsolePrinter
import top.kmar.game.EventListener
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
        EventListener.registryMousePosEvent { x, y, _, _ ->
            player.x = (x - 5).coerceAtLeast(0).coerceAtMost(it.width - player.width)
            player.y = (y - 4).coerceAtLeast(0).coerceAtMost(it.height - player.height)
        }
        var maxTime = 70
        var maxBlood = 50
        var time = 0
        val random = Random()
        it.appendReusableTask(GMap.AFTER_LOGIC) {
            if (++time >= maxTime) {
                time = 0
                val x = random.nextInt(it.width - 10)
                it.putEntity(EnemyPlaneEntity(x, -5, 10, 5, random.nextInt(25, maxBlood)), 1)
                if (maxTime != 50 && random.nextBoolean())
                    --maxTime
                if (maxBlood != 500 && random.nextBoolean())
                    ++maxBlood
            }
        }
        it.appendReusableTask(GMap.AFTER_RENDER) {
            ConsolePrinter.drawString("玩家分数：${player.score}", 0, 0)
            ConsolePrinter.drawString("玩家血量: ${player.blood}", 0, 1)
            ConsolePrinter.drawString("生成间隔: $maxTime ms", 0, 2)
            ConsolePrinter.drawString("最大血量: $maxBlood", 0, 3)
            ConsolePrinter.drawString("FPS：${it.fps}", 0, 4)
        }
        it.putEntity(player, 5)
        it.start(5, 20, 15) { true }
    }
    GMap.Builder.dispose()
}