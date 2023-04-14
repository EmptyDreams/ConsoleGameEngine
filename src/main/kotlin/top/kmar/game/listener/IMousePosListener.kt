package top.kmar.game.listener

/** 鼠标坐标事件 */
interface IMousePosListener {

    /** 鼠标移动事件 */
    fun onMove(x: Int, y: Int, oldX: Int, oldY: Int)

}