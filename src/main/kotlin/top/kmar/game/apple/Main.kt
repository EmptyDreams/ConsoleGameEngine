package top.kmar.game.apple

import top.kmar.game.ConsolePrinter
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** 每帧的时长（ms） */
const val frame = 50
/** 绘制宽度（是横向像素的两倍） */
const val screenWidth = 320 * 2
/** 绘制高度（与纵向像素相等） */
const val screenHeight = 240
/** 缓存数量 */
const val cacheAmount = 2
val progress = AtomicInteger(0)
val flag = Array(cacheAmount) { AtomicBoolean(false) }

fun main() {
    ConsolePrinter.init(
        screenWidth,
        screenHeight,
        1,
        cacheAmount
    )
    val pool = ThreadPoolExecutor(
        cacheAmount, cacheAmount, 0L, TimeUnit.MILLISECONDS, LinkedBlockingDeque(cacheAmount)
    )
    for (i in 0 until cacheAmount) {
        pool.submit {
            ConsolePrinter.quickClearAllAttr(0, i)
            while (progress.get() + 1 != ImageReader.amount) {
                if (flag[i].get()) {
                    Thread.sleep(3)
                    continue
                }
                ImageReader.render(i)
                flag[i].set(true)
            }
        }
    }
    pool.shutdown()
    ImageReader.start(cacheAmount)

    while (ImageReader.cacheSize.get() < 500)
        Thread.sleep(50)
    var offset = 0L
    var pre = System.currentTimeMillis()
    var group = cacheAmount - 1
    var first = true
    while (progress.get() + 1 != ImageReader.amount) {
        val preGroup = group
        if (++group == cacheAmount) group = 0
        while (!flag[group].get())
            Thread.sleep(1)
        var time = System.currentTimeMillis()
        val fix = offset.coerceAtMost((frame shr 1).toLong())
        while (time - pre - fix < frame) {
            Thread.sleep(1)
            time = System.currentTimeMillis()
        }
        offset += frame - (time - pre)
        pre = time
        ConsolePrinter.flush(group)
        if (first) first = false
        else flag[preGroup].set(false)
        progress.incrementAndGet()
    }
}