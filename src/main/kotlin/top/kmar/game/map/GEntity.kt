package top.kmar.game.map

import java.util.stream.Stream

/**
 * 游戏实体，出现在游戏内的任何元素都应从该接口派生
 * @author 空梦
 */
@Suppress("unused")
interface GEntity {

    /** 实体是否可见 */
    val visible: Boolean
        get() = true
    /** 该实体是否具有碰撞箱 */
    val collisible: Boolean
    /** 该实体是否死亡，为 true 时将会在下一个游戏循环中被移除 */
    val died: Boolean

    /** 实体 X 轴坐标（单位：格） */
    val x: Int
    /** 实体 Y 轴坐标（单位：格） */
    val y: Int
    /** 实体宽度（单位：格） */
    val width: Int
    /** 实体宽度（单位：格） */
    val height: Int
    /** 实体所占区域 */
    val bound: Rect2D
        get() = Rect2D(x, y, width, height)

    /** 实体右边界的坐标 */
    val right: Int
        get() = x + width - 1
    /** 实体下边界的坐标 */
    val bottom: Int
        get() = y + height - 1

    /** 将该实体绘制到画布中 */
    fun render(graphics: SafeGraphics)

    /**
     * 获取当前实体指定区域内的碰撞信息。
     *
     * 坐标相对于当前实体，返回的 Stream 中可以包含指定区域外的部分。
     */
    fun getCollision(x: Int, y: Int, width: Int, height: Int): Stream<Location2D>

    /** @see getCollision */
    fun getCollision(rect: Rect2D) = getCollision(rect.x, rect.y, rect.width, rect.height)

    /**
     * 更新实体状态
     * @param time 距离上一次执行间隔的时间（ms）
     */
    fun update(map: GMap, time: Long)

    /** 使该实体被指定实体杀死 */
    fun beKilled(map: GMap, killer: GEntity) { }

    /** 在发生与其它实体的碰撞时触发 */
    fun onCollision(map: GMap, that: GEntity) { }

    /** 在被添加到地图时触发 */
    fun onGenerate(map: GMap) { }

    /** 在被从地图移除后触发 */
    fun onRemove(map: GMap) { }

    /** 返回当前对象的深拷贝对象 */
    fun copy(): GEntity

}