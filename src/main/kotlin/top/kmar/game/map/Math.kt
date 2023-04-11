@file:Suppress("unused")

package top.kmar.game.map

/** 用于表示一个矩形区域 */
data class Rect2D(val x: Int, val y: Int, val width: Int, val height: Int) {

    /** 右边界（包含在矩形之中） */
    val right: Int
        get() = x + width - 1

    /** 下边界（包含在矩形之中） */
    val bottom: Int
        get() = y + height - 1

    val isEmpty: Boolean
        get() = width == 0 || height == 0

    fun containsY(y: Int) = y >= this.y && y < this.y + height

    fun containsX(x: Int) = x >= this.x && x < this.x + width

    companion object {

        @JvmStatic
        @get:JvmName("empty")
        val empty = Rect2D(0, 0, 0, 0)

    }

}

/** 用于表示一个一维矩形（直线） */
data class Rect1D(val x: Int, val width: Int) {

    /** 右边界（包含在矩形之中） */
    val right: Int
        get() = x + width - 1

    val isEmpty: Boolean
        get() = width == 0

    companion object {

        @JvmStatic
        @get:JvmName("empty")
        val empty = Rect1D(0, 0)

    }

}

/** 用于表示一个二维坐标 */
data class Location2D(val x: Int, val y: Int)

/** 用于表示一个二维的尺寸 */
data class Size2D(val width: Int, val height: Int)