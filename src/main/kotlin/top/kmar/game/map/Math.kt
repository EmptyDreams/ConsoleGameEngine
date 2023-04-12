@file:Suppress("unused")

package top.kmar.game.map

import kotlin.math.max
import kotlin.math.min

/** 用于表示一个矩形区域 */
data class Rect2D(val x: Int, val y: Int, val width: Int, val height: Int) : Comparable<Rect2D> {

    /** 右边界 */
    val right: Int
        get() = x + width

    /** 下边界 */
    val bottom: Int
        get() = y + height

    val isEmpty: Boolean
        get() = width == 0 || height == 0

    fun containsY(y: Int) = y >= this.y && y < this.y + height

    fun containsX(x: Int) = x >= this.x && x < this.x + width

    /**
     * 与指定矩形求交集。
     *
     * 注意：仅当两者存在交集时可以计算出正确结果。
     */
    fun intersect(that: Rect2D): Rect2D {
        val left = max(x, that.x)
        val top = max(y, that.y)
        val right = min(right, that.right)
        val bottom = min(bottom, that.bottom)
        return Rect2D(left, top, right - left, bottom - top)
    }

    /** 判断是否存在交集 */
    fun hasIntersection(that: Rect2D): Boolean =
        !(x >= that.right || y >= that.bottom || right <= that.x || bottom <= that.y)

    /** 将相对于地图的全局坐标映射到相对于实体的区域坐标 */
    fun mapToEntity(entity: GEntity): Rect2D =
        Rect2D(x - entity.x, y - entity.y, width, height)

    override fun compareTo(other: Rect2D): Int {
        if (x != other.x) return x - other.x
        if (y != other.y) return y - other.y
        if (width != other.width) return width - other.width
        return height - other.height
    }

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