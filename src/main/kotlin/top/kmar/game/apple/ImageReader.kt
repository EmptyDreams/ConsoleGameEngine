package top.kmar.game.apple

import top.kmar.game.ConsolePrinter.BACKGROUND_WHITE
import top.kmar.game.printer
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object ImageReader {

    const val width = screenWidth / 2
    const val height = screenHeight

    /**
     * 这里是压缩包文件地址
     * 该地址是我随便写的，注意修改
     */
    private val zip = ZipFile("resources/video ${width}x${height} $frame.zip")
    val amount = zip.size()
    private val index = AtomicInteger(1)
    private val cache = Array(cacheAmount) { ConcurrentLinkedDeque<ArrayList<Node>>() }
    val cacheSize = AtomicInteger(0)

    fun render(group: Int) {
        val deque = cache[group]
        while (deque.isEmpty())
            Thread.sleep(5)
        val array = deque.removeFirst()
        array.parallelStream()
            .forEach { (color, x, y, amount) ->
                printer.quickFillAttr(color, x, y, amount, group)
            }
        cacheSize.getAndDecrement()
    }

    fun start(size: Int) {
        Thread {
            val src = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val previous = Array(size) { src }
            while (index.get() <= amount) {
                if (cacheSize.get() > 1000)
                    Thread.sleep(10)
                val index = index.getAndIncrement()
                val group = (index - 1) % size
                val entry = zip.getEntry("Image${index}.jpg")
                val image = zip.getInputStream(entry).use { ImageIO.read(it) }
                val array = ArrayList<Node>(64)
                var color = -1
                var startX = -1
                var startY = -1
                val preImage = previous[group]
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val white = if (image.getRGB(x, y) and 0xFF > 150) BACKGROUND_WHITE else 0
                        val preWhite = if (preImage.getRGB(x, y) and 0xFF > 150) BACKGROUND_WHITE else 0
                        if (white == preWhite) {
                            if (color == -1) continue
                            array.add(Node(color, startX shl 1, startY, ((x - startX) + (y - startY) * width) shl 1))
                            color = -1
                        } else if (color == -1) {
                            color = white
                            startX = x
                            startY = y
                        } else if (color != white) {
                            array.add(Node(color, startX shl 1, startY, ((x - startX) + (y - startY) * width) shl 1))
                            color = white
                            startX = x
                            startY = y
                        }
                    }
                }
                if (color != -1)
                    array.add(Node(color, startX shl 1, startY, ((width - startX) + (height - startY - 1) * width) shl 1))
                cache[group].add(array)
                previous[group] = image
                cacheSize.getAndIncrement()
            }
        }.start()
    }

}

private data class Node(
    val color: Int,
    val x: Int,
    val y: Int,
    val amount: Int
)