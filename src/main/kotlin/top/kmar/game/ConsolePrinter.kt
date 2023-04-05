package top.kmar.game

import java.io.File

typealias printer = ConsolePrinter

/**
 * 控制台操作类。
 *
 * 有关概念说明：
 *
 * + 缓存下标：
 *
 *      　　程序会给每一个缓存分配一个下标（从 0 开始），第一个被显示的下标为最后一个缓存，调用一次 [flush] 后将会显示下标为 0 的缓存。
 *      正在显示的缓存我们称之为活动缓存或前台，直接向活动缓存写入数据会直接显示在屏幕上，向非活动缓存写入数据会在其变为活动缓存时显示到屏幕上。
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
 *
 *      　　除 clear 系列函数外的所有函数，传入 `attr = -1` 表示无效 attr，打印内容时将忽略 attr 信息。
 *
 * + 字符宽度：
 *
 *      　　在控制台中，不同字符宽度不同，拉丁文字符宽度为 1，而中文字符宽度为 2，在打印字符串时会自动计算宽度来保证打印结果不超出打印边界。
 *      不过部分函数不会处理这些信息，需要用户自行处理。
 *
 *      　　计算字符宽度时，满足 `char < 0x100` 的宽度视为 1，否则为 2.
 * @author 空梦
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
    const val FOREGROUND_WHITE = FOREGROUND_BLUE or FOREGROUND_GREEN or FOREGROUND_RED or FOREGROUND_INTENSITY
    const val BACKGROUND_BLUE = 0x10
    const val BACKGROUND_GREEN = 0x20
    const val BACKGROUND_RED = 0x40
    const val BACKGROUND_INTENSITY = 0x80
    const val BACKGROUND_WHITE = BACKGROUND_BLUE or BACKGROUND_GREEN or BACKGROUND_RED or BACKGROUND_INTENSITY
    const val COMMON_LVB_LEADING_BYTE = 0x100
    const val COMMON_LVB_TRAILING_BYTE = 0x200
    const val COMMON_LVB_GRID_HORIZONTAL = 0x400
    @Suppress("SpellCheckingInspection")
    const val COMMON_LVB_GRID_LVERTICAL = 0x800
    @Suppress("SpellCheckingInspection")
    const val COMMON_LVB_GRID_RVERTICAL = 0x1000
    const val COMMON_LVB_REVERSE_VIDEO = 0x4000
    const val COMMON_LVB_UNDERSCORE = 0x8000

    /**
     * 初始化控制台信息，开始使用前必须调用该函数。
     *
     * **注意：缓存数量一般用于解决闪屏问题，当程序性能被绘制速度限制时可以通过提高缓存数量预绘制图形来提高性能。
     * 缓存数量较多时启动时会有明显的窗口闪动**
     *
     * @param width 横向字符数量
     * @param height 纵向字符数量
     * @param fontWidth 一个字符的宽度（宽高比固定为 1:2）
     * @param cache 缓存数量，必须大于 0
     * @param path DLL 文件的路径
     */
    @JvmStatic
    fun init(width: Int, height: Int, fontWidth: Int, cache: Int = 2, path: File = File("./libs/utils.dll")) {
        require(cache > 0) { "缓存数量[$cache]应当大于 0" }
        System.load(path.absolutePath)
        this.width = width
        this.height = height
        initN(width, height, fontWidth, cache)
    }

    /** 快速清空全图的字符 */
    @JvmStatic
    fun quickClearAllChar(char: Char = ' ', index: Int = this.index) {
        quickFillChar(char, 0, 0, width * height, index)
    }

    /**
     * 快速填充字符，若填充宽度超过当前行宽，会跨行填充而非截止。
     *
     * (x1, y1) 是填充起始位点，(x2, y2) 是填充的结束位点，填充时不包括结束位点本身。
     *
     * 填充时会从起点开始横向填充，直到遇到结束位点，最终的填充区域并非是由两点构成的矩形。
     */
    @JvmStatic
    fun quickFillChar(char: Char, x1: Int, y1: Int, x2: Int, y2: Int, index: Int = this.index) {
        quickFillChar(char, x1, y1, x2 + y2 * width, index)
    }

    /** 快速清空全图的 attr */
    @JvmStatic
    fun quickClearAllAttr(attr: Int = -1, index: Int = this.index) {
        quickFillAtr(attr, 0, 0, width * height, index)
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
        quickFillAtr(attr, x1, y1, x2 + y2 * width, index)
    }

    /** 快速清空全图字符和 attr */
    @JvmStatic
    fun quickClear(char: Char = ' ', attr: Int = -1, index: Int = this.index) {
        quickClearAllAttr(attr, index)
        quickClearAllChar(char, index)
    }

    @JvmStatic
    fun getCharWidth(char: Char): Int =
        if (char.code < 0x100) 1 else 2

    // ---------- native function ----------//

    @JvmStatic
    private external fun initN(width: Int, height: Int, fontWidth: Int, cache: Int)

    /**
     * 刷新控制台显示，将指定下标的缓存设置为活动缓存。
     *
     * 即使缓存数量为一也应当定期调用该函数，否则无法隐藏输入光标。
     */
    @JvmStatic
    external fun flush(index: Int = this.index)

    /** 快速填充字符，若填充宽度超过当前行款，会跨行填充而非截止 */
    @JvmStatic
    external fun quickFillChar(char: Char, x: Int, y: Int, amount: Int, index: Int = this.index)

    /** 快速填充 attr，若填充宽度超过当前行宽，会跨行填充而非截止 */
    @JvmStatic
    external fun quickFillAtr(attr: Int, x: Int, y: Int, amount: Int, index: Int = this.index)

    @JvmStatic
    external fun fillRect(char: Char, x: Int, y: Int, width: Int, height: Int, index: Int = this.index)

    /** 使用指定字符填充一个空心矩形 */
    @JvmStatic
    external fun fillRectHollow(char: Char, x: Int, y: Int, width: Int, height: Int, index: Int = this.index)

    /** 修改指定区域的填充属性 */
    @JvmStatic
    external fun modifyAttr(attr: Int, x: Int, y: Int, width: Int, height: Int, index: Int = this.index)

    /**
     * 打印一个字符串
     * @param text 要打印的字符串
     */
    @JvmStatic
    external fun drawString(text: String, x: Int, y: Int, index: Int = this.index)

    /**
     * 以指定字符为填充绘制一条虚线。
     *
     * 偏移量为 0 时将以实现的第一格为开始进行绘制，通过调整偏移量可以修改虚线的起始形状（偏移后不会超出绘制边界）。
     *
     * @param charWidth 每个字符的宽度（1 或 2）
     * @param width 虚线总长（单位：字符）
     * @param lineLength 每一段实线的长度（单位：字符）
     * @param spaceLength 两个实线之间的长度（单位：格）
     * @param offset 横向偏移量（单位：字符），取值范围在 [0, lineLength + spaceLength)
     */
    @JvmStatic
    external fun drawDottedLine(
        char: Char, charWidth: Int,
        x: Int, y: Int, width: Int, height: Int,
        lineLength: Int, spaceLength: Int, offset: Int,
        index: Int  = this.index
    )

    @JvmStatic
    external fun drawVerticalDottedLine(
        char: Char, charWidth: Int,
        x: Int, y: Int, width: Int, height: Int,
        lineLength: Int, spaceLength: Int, offset: Int,
        index: Int = this.index
    )

}