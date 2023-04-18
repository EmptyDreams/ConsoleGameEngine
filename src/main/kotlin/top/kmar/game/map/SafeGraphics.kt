package top.kmar.game.map

import top.kmar.game.ConsolePrinter
import top.kmar.game.ConsolePrinter.getCharWidth
import top.kmar.game.utils.Rect2D
import kotlin.math.max
import kotlin.math.min

/**
 * 画笔
 * @author 空梦
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class SafeGraphics @Deprecated("不应当调用该构造函数创建对象") internal constructor(
    /** 画笔在画布中的 X 轴坐标 */
    val x: Int,
    /** 画笔在画布中的 Y 轴坐标 */
    val y: Int,
    /** 绘制区域宽度 */
    val width: Int,
    /** 绘制区域高度 */
    val height: Int,
    /** 缓存下标 */
    val index: Int
) {

    private fun clip(x: Int, y: Int, width: Int, height: Int): Rect2D {
        val left = max(x, 0)
        val top = max(y, 0)
        val right = min(x + width, this.width)
        val bottom = min(y + height, this.height)
        if (left >= right || top >= bottom) return Rect2D.empty
        return Rect2D(this.x + left, this.y + top, right - left, bottom - top)
    }

    /** 以指定符号填充一个矩形 */
    fun fillRect(char: Char, x: Int, y: Int, width: Int, height: Int, attr: Int = -1) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        ConsolePrinter.fillRect(char, bound.x, bound.y, bound.width shr (getCharWidth(char) - 1), bound.height, index)
        if (attr != -1)
            ConsolePrinter.modifyAttr(attr, bound.x, bound.y, bound.width, bound.height, index)
    }

    /** 以指定符号填充一个空心矩形 */
    fun fillRectHallow(char: Char, x: Int, y: Int, width: Int, height: Int, attr: Int = -1) {
        if (height < 3) return fillRect(char, x, y, width, height, attr)
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        val charWidth = getCharWidth(char)
        val length = bound.width shr (charWidth - 1)
        if (bound.y == this.y + y) {
            if (attr != -1) ConsolePrinter.modifyAttr(attr, bound.x, y, bound.width, index)
            ConsolePrinter.quickFillChar(char, bound.x, y, length, index)
        }
        if (bound.bottom == this.y + y + height) {
            if (attr != -1) ConsolePrinter.modifyAttr(attr, bound.x, bound.bottom - 1, bound.width, index)
            ConsolePrinter.quickFillChar(char, bound.x, bound.bottom - 1, length, index)
        }
        if (bound.x == this.x + x) {
            if (attr != -1) ConsolePrinter.modifyAttr(attr, x, bound.y + 1, charWidth, bound.height - 2, index)
            ConsolePrinter.fillRect(char, bound.x, bound.y + 1, charWidth, bound.height - 2, index)
        }
        if (bound.right == this.x + x + width) {
            val left = bound.right - charWidth
            if (attr != -1) ConsolePrinter.modifyAttr(attr, left, bound.y + 1, charWidth, bound.height, index)
            ConsolePrinter.fillRect(char, left, bound.y + 1, charWidth, bound.height - 2, index)
        }
    }

    /** 修改一个矩形区域内的 attr */
    fun modifyRect(attr: Int, x: Int, y: Int, width: Int, height: Int) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        ConsolePrinter.modifyAttr(attr, bound.x, bound.y, bound.width, bound.height, index)
    }

    /**
     * 打印一个不换行的字符串，超出范围的字符将被删除
     * @param maxWidth 打印的最大宽度
     * @param offsetX 横向（向左）偏移量
     * @return 如果想要打印出字符串结尾，offsetX 需要增加的的最小量
     */
    fun drawStringLine(text: String, x: Int, y: Int, maxWidth: Int, offsetX: Int = 0, attr: Int = -1): Int {
        require(offsetX >= 0) { "offsetX[$offsetX] 应当 >= 0" }
        var bound = clip(x, y, maxWidth, 1)
        if (bound.isEmpty) return 0
        val offset = offsetX - x
        val start = if (offset <= 0) 0 else {
            var i = 0
            var width = 0
            while (i != text.length) {
                width += getCharWidth(text[i++])
                if (width == offset) break
                else if (width > offset) {
                    bound = bound.copy(x = bound.x + 1)
                    break
                }
            }
            i
        }
        var width = 0
        val end = run {
            for (i in start until text.length) {
                width += getCharWidth(text[i])
                if (width == bound.width) return@run i + 1
                else if (width > bound.width) return@run i
            }
            text.length
        }
        if (start == 0 && end == text.length)
            ConsolePrinter.drawString(text, bound.x, bound.y, index)
        else
            ConsolePrinter.drawString(text.substring(start until end), x, y, index)
        if (attr != -1)
            ConsolePrinter.modifyAttr(attr, bound.x, bound.y, width, 1, index)
        var minOffset = 0
        for (i in end until text.length) {
            minOffset += getCharWidth(text[i])
        }
        return minOffset
    }

    /**
     * 打印一个换行的字符串，当字符串内容超过最大限制时会换行打印。
     *
     * 注意：如果打印内容超出了显示范围但没有超过最大打印限制，将直接被删除而非换行。
     *
     * @param maxWidth 最大宽度限制
     * @param maxHeight 最大高度限制，超出高度的内容将被删除
     * @param offsetY 纵向偏移量
     * @return 实际打印区域，单位是字符数量
     */
    fun drawStringRect(
        text: String, x: Int, y: Int,
        maxWidth: Int, maxHeight: Int, offsetY: Int = 0,
        attr: Int = -1
    ): Int {
        require(offsetY >= 0) { "offsetY[$offsetY] 应当 >= 0" }
        val bound = clip(x, y, maxWidth, maxHeight)
        if (bound.isEmpty) return 0
        var width = 0
        var preIndex = 0
        var yFlag = y - offsetY
        for ((i, it) in text.withIndex()) {
            val next = width + getCharWidth(it)
            if (next == maxWidth) {
                if (bound.containsY(yFlag))
                    drawStringLine(text.substring(preIndex .. i), x, yFlag, maxWidth, 0, attr)
                preIndex = i + 1
                ++yFlag
                width = 0
            } else if (next > maxWidth) {
                if (bound.containsY(yFlag))
                    drawStringLine(text.substring(preIndex until i), x, yFlag, maxWidth, 0, attr)
                preIndex = i
                ++yFlag
                width = getCharWidth(it)
            } else width = next
        }
        if (preIndex != text.length) {
            if (bound.containsY(yFlag))
                drawStringLine(text.substring(preIndex), x, yFlag, maxWidth, 0, attr)
            ++yFlag
        }
        return (yFlag - bound.bottom).coerceAtLeast(0)
    }

    /**
     * 以指定字符为填充绘制一条虚线
     * @param width 虚线总长
     * @param lineLength 每一段实线的长度
     * @param spaceLength 两个实线之间的长度
     */
    fun drawDottedLine(
        char: Char,
        x: Int, y: Int, width: Int, height: Int,
        lineLength: Int, spaceLength: Int, offset: Int = 0
    ) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        ConsolePrinter.drawDottedLine(
            char, getCharWidth(char),
            bound.x, bound.y, bound.width, bound.height,
            lineLength, spaceLength, (bound.x - x + offset) % (lineLength + spaceLength),
            index
        )
    }

    /**
     * 以指定字符填充一条竖直的虚线
     * @param height 虚线总长
     * @param lineLength 每一段虚线的长度
     * @param spaceLength 两个实线之间的长度
     */
    fun drawVerticalDottedLine(
        char: Char,
        x: Int, y: Int,width: Int, height: Int,
        lineLength: Int, spaceLength: Int, offset: Int = 0
    ) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        ConsolePrinter.drawVerticalDottedLine(
            char, getCharWidth(char),
            bound.x, bound.y, bound.width, bound.height,
            lineLength, spaceLength, (bound.y - y + offset) % (lineLength + spaceLength),
            index
        )
    }

    @Suppress("FunctionName")
    companion object {

        @JvmStatic
        @JvmName("createSafeGraphics")
        internal fun _createSafeGraphics(map: GMap, index: Int) = SafeGraphics(map, index)

        @JvmStatic
        @JvmName("createSafeGraphics")
        internal fun _createSafeGraphics(map: GMap) = SafeGraphics(map)

        @JvmStatic
        @JvmName("createSafeGraphics")
        internal fun _createSafeGraphics(map: GMap, x: Int, y: Int, width: Int, height: Int, index: Int) =
            SafeGraphics(map, x, y, width, height, index)

        @JvmStatic
        @JvmName("createSafeGraphics")
        internal fun _createSafeGraphics(map: GMap, x: Int, y: Int, width: Int, height: Int) =
            SafeGraphics(map, x, y, width, height)

        @JvmStatic
        @JvmName("createHalfSafeGraphics")
        internal fun _createHalfSafeGraphics(x: Int, y: Int, width: Int, height: Int, index: Int) =
            HalfSafeGraphics(x, y, width, height, index)

    }

}

@Suppress("DEPRECATION")
@JvmName("_ do not use")
fun SafeGraphics(map: GMap, index: Int = ConsolePrinter.index): SafeGraphics =
    SafeGraphics(0, 0, map.width, map.height, index)

@Suppress("DEPRECATION")
@JvmName("_ do not use")
fun SafeGraphics(map: GMap, x: Int, y: Int, width: Int, height: Int, index: Int = ConsolePrinter.index): SafeGraphics {
    val left = x.coerceAtLeast(0)
    val top = y.coerceAtLeast(0)
    val right = (x + width).coerceAtMost(map.width)
    val bottom = (y + height).coerceAtMost(map.height)
    return SafeGraphics(x, y, right - left, bottom - top, index)
}

@Suppress("FunctionName", "DEPRECATION")
@JvmName("_ do not use")
fun HalfSafeGraphics(x: Int, y: Int, width: Int, height: Int, index: Int): SafeGraphics =
    SafeGraphics(x, y, width, height, index)