package top.kmar.game

import java.io.File
import kotlin.math.max
import kotlin.math.min

typealias printer = ConsolePrinter

/**
 * 控制台操作类
 *
 * 有关概念说明：
 *
 * + 缓存下标：
 *
 *      　　程序会给每一个缓存分配一个下标（从 0 开始），第一个被显示的下标为最后一个缓存，调用一次 [flush] 后将会显示下标为 0 的缓存。
 *      正在显示的缓存我们称之为活动缓存或前台，直接向活动缓存写入数据会直接显示在屏幕上，向非活动缓存写入数据会在其变为活动缓存时显示到屏幕上。
 *
 *      　　多缓存机制可用于解决刷新显示时的闪屏问题。
 *
 *      　　请注意，出于性能方面的考虑，当一个缓存由活动缓存变为非活动缓存时，其内容不会被清空，再向该缓存写入数据时上一帧的数据将仍然存在，如果有必要，
 *      请手动调用 clear 系列函数清空缓存。（这里的上一帧是指该缓存的上一帧，而非整个程序的上一帧，当缓存数量大于 1 时这两个概念将显示出不同.）
 *
 *      　　所有接收缓存下标的函数都会缺省填入活动缓存的下一个缓存。
 *
 *      　　活动缓存的判定是在 JVM 端完成的，如果使用反射等方法调用了 [flushN] 函数，请务必手动更新 JVM 端的缓存下标。
 *
 * + ATTR：
 *
 *      　　这是用于控制终端字体颜色、背景颜色等属性的值，所有支持的类型已在 ConsolePrinter 中列出。需要注意的是，在调用 [flush] 函数时，
 *      同样不会清除上一次设置的 ATTR 信息。
 */
object ConsolePrinter {

    @JvmStatic
    var width = 0
        private set
    @JvmStatic
    var height = 0
        private set
    /** 标记当前正在写入的缓存下标 */
    @JvmStatic
    var index: Int = 0
        private set

    const val FOREGROUND_BLUE = 0x1
    const val FOREGROUND_GREEN = 0x2
    const val FOREGROUND_RED = 0x4
    const val FOREGROUND_INTENSITY = 0x8
    const val BACKGROUND_BLUE = 0x10
    const val BACKGROUND_GREEN = 0x20
    const val BACKGROUND_RED = 0x40
    const val BACKGROUND_INTENSITY = 0x80
    const val COMMON_LVB_LEADING_BYTE = 0x100
    const val COMMON_LVB_TRAILING_BYTE = 0x200
    const val COMMON_LVB_GRID_HORIZONTAL = 0x400
    @Suppress("SpellCheckingInspection")
    const val COMMON_LVB_GRID_LVERTICAL = 0x800
    @Suppress("SpellCheckingInspection")
    const val COMMON_LVB_GRID_RVERTICAL = 0x1000
    const val COMMON_LVB_REVERSE_VIDEO = 0x4000
    const val COMMON_LVB_UNDERSCORE = 0x8000

    /** 对于 PowerShell 的缺省属性 */
    const val DEFAULT_ATTR = FOREGROUND_RED or FOREGROUND_GREEN or FOREGROUND_BLUE or BACKGROUND_BLUE or BACKGROUND_RED

    /**
     * 初始化控制台信息，开始使用前必须调用该函数。
     *
     * **注意：缓存数量一般用于解决闪屏问题，当程序性能被绘制速度限制时可以通过提高缓存数量预绘制图形来提高性能。
     * 缓存数量较多时启动时会有明显的窗口抖动**
     *
     * @param width 横向字符数量
     * @param height 纵向字符数量
     * @param fontWidth 一个字符的宽度（宽高比固定为 1:2）
     * @param cache 缓存数量，必须大于 0
     * @param path DLL 文件的路径
     */
    @JvmStatic
    fun init(width: Int, height: Int, fontWidth: Int, cache: Int = 2, path: File = File("./utils.dll")) {
        System.load(path.absolutePath)
        require(cache > 0) { "缓存数量[$cache]应当大于 0" }
        this.width = width
        this.height = height
        initN(width, height, fontWidth, cache)
    }

    @JvmStatic
    private fun clip(x: Int, y: Int, width: Int, height: Int): Rect2D {
        val right = min(x + width, this.width)
        val bottom = min(y + height, this.height)
        val left = max(x, 0)
        val top = max(y, 0)
        if (right < left || bottom < top || left >= this.width || top >= this.height) return Rect2D.empty
        return Rect2D(left, top, right - left, bottom - top)
    }

    /** 快速清空全图的字符 */
    @JvmStatic
    fun quickClearAllChar(char: Char = ' ', index: Int = this.index) {
        quickFillCharN(char, 0, 0, width * height, index)
    }

    /** 快速填充字符，若填充宽度超过当前行款，会跨行填充而非截止 */
    @JvmStatic
    fun quickFillChar(char: Char, x: Int, y: Int, width: Int, index: Int = this.index) {
        val head = y * this.width + x
        val tail = head + width
        val bound = this.width * this.height
        val amount = min(tail, bound) - head
        quickFillCharN(char, x, y, amount, index)
    }

    /**
     * 快速填充字符，若填充宽度超过当前行宽，会跨行填充而非截止。
     *
     * (x1, y1) 是填充起始位点，(x2, y2) 是填充的结束位点，填充时不包括结束位点本身。
     *
     * 填充时会从起点开始横向填充，直到遇到结束位点，最终的填充区域并非是由两点构成的矩形。
     */
    fun quickFillChar(char: Char, x1: Int, y1: Int, x2: Int, y2: Int, index: Int = this.index) {
        val x = x1.coerceAtLeast(0)
        val y = y1.coerceAtLeast(0)
        val amount = x2.coerceAtMost(width) + y2.coerceAtMost(height) * width
        quickFillCharN(char, x, y, amount, index)
    }

    /** 快速清空全图的 attr */
    @JvmStatic
    fun quickClearAllAttr(attr: Int = DEFAULT_ATTR, index: Int = this.index) {
        quickFillAtrN(attr, 0, 0, width * height, index)
    }

    /** 快速填充 attr，若填充宽度超过当前行宽，会跨行填充而非截止 */
    @JvmStatic
    fun quickFillAttr(attr: Int, x: Int, y: Int, width: Int, index: Int = this.index) {
        val head = y * this.width + x
        val tail = head + width
        val bound = this.width * this.height
        val amount = min(tail, bound) - head
        quickFillAtrN(attr, x, y, amount, index)
    }

    /**
     * 快速填充 attr，若填充宽度超过当前行宽，会跨行填充而非截止。
     *
     * (x1, y1) 是填充起始位点，(x2, y2) 是填充的结束位点，填充时不包括结束位点本身。
     *
     * 填充时会从起点开始横向填充，直到遇到结束位点，最终的填充区域并非是由两点构成的矩形。
     */
    @JvmStatic
    fun quickFillAttr(attr: Int, x1: Int, y1: Int, x2: Int, y2: Int, index: Int = this.index) {
        val x = x1.coerceAtLeast(0)
        val y = y1.coerceAtLeast(0)
        val amount = x2.coerceAtMost(width) + y2.coerceAtMost(height) * width
        quickFillAtrN(attr, x, y, amount, index)
    }

    /** 快速清空全图字符和 attr */
    @JvmStatic
    fun clear(char: Char = ' ', attr: Int = DEFAULT_ATTR, index: Int = this.index) {
        quickClearAllAttr(attr, index)
        quickClearAllChar(char, index)
    }

    /**
     * 清空指定区域
     *
     * 如果想要清空全图请调用 [quickClearAllChar]、[quickClearAllAttr]、[clear]
     */
    @JvmStatic
    fun clearRect(
        x: Int, y: Int, width: Int, height: Int,
        char: Char = ' ', attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        fillRect(char, x, y, width, height, attr, index)
    }

    /** 以指定字符绘制一个实心矩形 */
    @JvmStatic
    fun fillRect(
        char: Char,
        x: Int, y: Int, width: Int, height: Int,
        attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        val len = getCharWidth(char)
        fillRectN(char, bound.x, bound.y, bound.width shr (len - 1), bound.height, attr, index)
    }

    /** 以指定字符绘制一个空心矩形 */
    @JvmStatic
    fun fillRectHollow(
        char: Char,
        x: Int, y: Int, width: Int, height:
        Int, attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        val len = getCharWidth(char)
        fillRectHollowN(char, bound.x, bound.y, bound.width shr (len - 1), bound.height, attr, index)
    }

    /**
     * 绘制一个字符串（不换行）
     * @param width 宽度限制（超出区域的部分将被删除）
     */
    @JvmStatic
    fun drawStringLine(
        text: String,
        x: Int, y: Int, width: Int,
        attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, 1)
        if (bound.isEmpty) return
        var start = 0
        if (x < 0) {    // 如果起点为负数则计算真实的起始位置
            var w = 0
            for (i in text.indices) {
                w += getCharWidth(text[i])
                if (w + x >= 0) {
                    start = i + 1
                    break
                }
            }
        }
        var w = 0
        var end = text.length
        for (i in start until text.length) {
            val tmp = getCharWidth(text[i])
            if (w + tmp > bound.width) {
                end = i
                break
            }
            w += tmp
        }
        if (start == 0 && end == text.length)
            drawStringN(text, bound.x, bound.y, bound.width, attr, index)
        else
            drawStringN(text.substring(start until end), bound.x, bound.y, bound.width, attr, index)
    }

    /**
     * 绘制一个字符串，超出限定区域后换行显示。
     * 注意，如果字符串超出了渲染范围但未超出输入的限定区域，超出渲染范围的内容将被删除而非换行。
     */
    @JvmStatic
    fun drawStringRect(
        text: String,
        x: Int, y: Int, width: Int, height: Int,
        attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        val sb = StringBuilder()
        var w = 0
        var line = y
        text.forEach {
            val tmp = getCharWidth(it)
            if (w + tmp > width) {
                drawStringLine(sb.toString(), x, line++, width, attr, index)
                if (line - y == height) return
                sb.clear()
                w = 0
            }
            w += tmp
            sb.append(it)
        }
        if (sb.isNotEmpty())
            drawStringLine(sb.toString(), x, line, width, attr, index)
    }

    /** 以指定字符为填充绘制一条水平的直线 */
    @JvmStatic
    fun drawLine(
        char: Char,
        x: Int, y: Int, width: Int,
        attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, 1)
        if (bound.isEmpty) return
        drawLineN(char, bound.x, bound.y, bound.width, attr, index)
    }

    /** 以指定字符为填充绘制一条竖直的直线 */
    @JvmStatic
    fun drawVerticalLine(
        char: Char,
        x: Int, y: Int, height: Int,
        attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, 1, height)
        if (bound.isEmpty) return
        drawVerticalLineN(char, bound.x, bound.y, bound.height, attr, index)
    }

    /**
     * 绘制水平的虚线
     * @param char 填充字符
     * @param width 总宽度
     * @param lineLength 每条线的长度
     * @param airLength 空白长度
     * @param background 空白处是否应用 attr
     */
    @JvmStatic
    fun drawDottedLine(
        char: Char,
        x: Int, y: Int, width: Int,
        lineLength: Int, airLength: Int,
        background: Boolean, attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, width, 1)
        if (bound.isEmpty) return
        drawDottedLineN(char, bound.x, bound.y, bound.width, lineLength, airLength, background, attr, index)
    }

    /**
     * 绘制竖直的虚线
     * @param char 填充字符
     * @param height 高度
     * @param lineLength 每条线的长度
     * @param airLength 空白长度
     * @param background 空白处是否应用 attr
     */
    @JvmStatic
    fun drawVerticalDottedLine(
        char: Char,
        x: Int, y: Int, height: Int,
        lineLength: Int, airLength: Int,
        background: Boolean, attr: Int = DEFAULT_ATTR, index: Int = this.index
    ) {
        val bound = clip(x, y, 1, height)
        if (bound.isEmpty) return
        drawVerticalDottedLineN(
            char,
            bound.x, bound.y, bound.height,
            lineLength, airLength,
            background, attr, index
        )
    }

    /** 修改指定区域的填充属性 */
    @JvmStatic
    fun modifyAttr(attr: Int, x: Int, y: Int, width: Int, height: Int, index: Int = this.index) {
        val bound = clip(x, y, width, height)
        if (bound.isEmpty) return
        modifyAttrN(attr, bound.x, bound.y, bound.width, bound.height, index)
    }

    /**
     * 刷新控制台显示，将指定下标的缓存设置为活动缓存
     */
    @JvmStatic
    fun flush(index: Int = this.index) = flushN(index)

    // ---------- native and private function ----------//

    @JvmStatic
    private fun getCharWidth(char: Char): Int =
        if (char.code < 0x100) 1 else 2

    @JvmStatic
    private external fun initN(width: Int, height: Int, fontWidth: Int, cache: Int)

    @JvmStatic
    private external fun quickFillCharN(char: Char, x: Int, y: Int, amount: Int, index: Int)

    @JvmStatic
    private external fun quickFillAtrN(attr: Int, x: Int, y: Int, amount: Int, index: Int)

    @JvmStatic
    private external fun drawLineN(char: Char, x: Int, y: Int, width: Int, attr: Int, index: Int)

    @JvmStatic
    private external fun drawVerticalLineN(char: Char, x: Int, y: Int, height: Int, attr: Int, index: Int)

    @JvmStatic
    private external fun drawDottedLineN(char: Char, x: Int, y: Int, width: Int, lineLength: Int, airLength: Int, background: Boolean, attr: Int, index: Int)

    @JvmStatic
    private external fun drawVerticalDottedLineN(char: Char, x: Int, y: Int, height: Int, lineLength: Int, airLength: Int, background: Boolean, attr: Int, index: Int)

    /**
     * 使用指定字符填充一个实心矩形
     * @param char 要填充的字符
     * @param attr 填充属性
     */
    @JvmStatic
    private external fun fillRectN(char: Char, x: Int, y: Int, width: Int, height: Int, attr: Int, index: Int)

    /** 使用指定字符填充一个空心矩形 */
    @JvmStatic
    private external fun fillRectHollowN(char: Char, x: Int, y: Int, width: Int, height: Int, attr: Int, index: Int)

    /** 修改指定区域的填充属性 */
    @JvmStatic
    private external fun modifyAttrN(attr: Int, x: Int, y: Int, width: Int, height: Int, index: Int)

    /**
     * 打印一个字符串
     * @param text 要打印的字符串
     * @param width 打印宽度
     * @param attr 填充属性
     */
    @JvmStatic
    private external fun drawStringN(text: String, x: Int, y: Int, width: Int, attr: Int, index: Int)

    @JvmStatic
    private external fun flushN(index: Int)

}

/** 用于表示一个矩形区域 */
data class Rect2D(val x: Int, val y: Int, val width: Int, val height: Int) {

    val right: Int
        get() = x + width

    val bottom: Int
        get() = y + height

    val isEmpty: Boolean
        get() = width == 0 || height == 0

    companion object {

        val empty = Rect2D(0, 0, 0, 0)

    }

}