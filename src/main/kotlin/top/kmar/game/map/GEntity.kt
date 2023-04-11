package top.kmar.game.map

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

    /** 实体右边界的坐标 */
    val right: Int
        get() = x + width - 1
    /** 实体下边界的坐标 */
    val bottom: Int
        get() = y + height - 1

    /** 将该实体绘制到画布中 */
    fun render(graphics: SafeGraphics)

    /**
     * 检查当前实体的指定位置是否具有碰撞体积。
     *
     * 注意:传入的坐标是相对于地图的。
     */
    fun hasCollision(x: Int, y: Int): Boolean

    /**
     * 检查当前实体与指定实体是否存在碰撞
     *
     * @return 所有碰撞的实体
     */
    fun checkCollision(that: GEntity): Boolean {
        for (y in this.y .. bottom) {
            for (x in this.x .. right) {
                if (hasCollision(x, y) && that.hasCollision(x, y))
                    return true
            }
        }
        return false
    }

    /**
     * 判断当前实体与另一个实体的区域是否存在交叉。
     *
     * 该函数用于判断碰撞箱前的预筛选，排除不可能存在碰撞的实体。
     */
    fun hasIntersect(that: GEntity): Boolean =
        x >= that.x && y >= that.y && right <= that.right && bottom <= that.bottom

    /**
     * 更新实体状态
     * @param time 距离上一次执行间隔的时间（ms）
     */
    fun update(map: GMap, time: Long)

    /** 使该实体被指定实体杀死 */
    fun beKilled(map: GMap, killer: GEntity)

    /** 在发生与其它实体的碰撞时触发 */
    fun onCollision(map: GMap, that: GEntity)

    /** 在被添加到地图时触发 */
    fun onGenerate(map: GMap)

    /** 在被从地图移除后触发 */
    fun onRemove(map: GMap)

    /** 返回当前对象的深拷贝对象 */
    fun copy(): GEntity

}