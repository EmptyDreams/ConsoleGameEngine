package top.kmar.game.listener

/** 按键事件 */
interface IButtonListener {

    /** 键盘按下时触发 */
    fun onPressed(code: Int)

    /** 键盘释放时触发 */
    fun onReleased(code: Int)

    /**
     * 键盘按下时触发
     *
     * 与 [onPressed] 不同的是，该函数在每次事件循环中都会触发
     *
     * 同时，该事件会在 [onPressed] 之后触发
     */
    fun onActive(code: Int)

}