package top.kmar.game

import top.kmar.game.listener.IButtonListener
import top.kmar.game.listener.IKeyboardListener
import top.kmar.game.listener.IMouseListener
import top.kmar.game.listener.IMousePosListener
import top.kmar.game.utils.Point2D
import java.util.*

/**
 * 事件监听器
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object EventListener {

    // 字母键
    const val KEY_A = 65
    const val KEY_B = 66
    const val KEY_C = 67
    const val KEY_D = 68
    const val KEY_E = 69
    const val KEY_F = 70
    const val KEY_G = 71
    const val KEY_H = 72
    const val KEY_I = 73
    const val KEY_J = 74
    const val KEY_K = 75
    const val KEY_L = 76
    const val KEY_M = 77
    const val KEY_N = 78
    const val KEY_O = 79
    const val KEY_P = 80
    const val KEY_Q = 81
    const val KEY_R = 82
    const val KEY_S = 83
    const val KEY_T = 84
    const val KEY_U = 85
    const val KEY_V = 86
    const val KEY_W = 87
    const val KEY_X = 88
    const val KEY_Y = 89
    const val KEY_Z = 90

    // 主键盘数字
    const val KEY_MAIN1 = 49
    const val KEY_MAIN2 = 50
    const val KEY_MAIN3 = 51
    const val KEY_MAIN4 = 52
    const val KEY_MAIN5 = 53
    const val KEY_MAIN6 = 54
    const val KEY_MAIN7 = 55
    const val KEY_MAIN8 = 56
    const val KEY_MAIN9 = 57
    const val KEY_MAIN0 = 48

    // 小键盘数字
    const val KEY_NUM1 = 97
    const val KEY_NUM2 = 98
    const val KEY_NUM3 = 99
    const val KEY_NUM4 = 100
    const val KEY_NUM5 = 101
    const val KEY_NUM6 = 102
    const val KEY_NUM7 = 103
    const val KEY_NUM8 = 104
    const val KEY_NUM9 = 105
    const val KEY_NUM0 = 96

    // F*
    const val KEY_F1 = 112
    const val KEY_F2 = 113
    const val KEY_F3 = 114
    const val KEY_F4 = 115
    const val KEY_F5 = 116
    const val KEY_F6 = 117
    const val KEY_F7 = 118
    const val KEY_F8 = 119
    const val KEY_F9 = 120
    const val KEY_F10 = 121
    const val KEY_F11 = 122
    const val KEY_F12 = 123
    const val KEY_F13 = 124
    const val KEY_F14 = 125
    const val KEY_F15 = 126
    const val KEY_F16 = 127
    const val KEY_F17 = 128
    const val KEY_F18 = 129
    const val KEY_F19 = 130
    const val KEY_F20 = 131
    const val KEY_F21 = 132
    const val KEY_F22 = 133
    const val KEY_F23 = 134
    const val KEY_F24 = 135

    // 主键盘区标点符号
    /** 反引号、波浪线 */
    const val KEY_BACK_QUOTE = 192
    /** 减号、下划线 */
    const val KEY_MAIN_MINUS = 189
    /** 等于号、加号 */
    const val KEY_EQUAL = 187
    /** 左方括号、左大括号 */
    const val KEY_BRACKET_SQUARE_LEFT = 219
    /** 右方括号、右大括号 */
    const val KEY_BRACKET_SQUARE_RIGHT = 221
    /** 分号、冒号 */
    const val KEY_SEMICOLON = 186
    /** 单引号、双引号 */
    const val KEY_QUOTE = 222
    /** 反斜杠、竖线 */
    const val KEY_BACKSLASH = 220
    /** 逗号、左尖括号 */
    const val KEY_COMMA = 188
    /** 句号、右尖括号 */
    const val KEY_MAIN_POINT = 190
    /** 斜杠、问号 */
    const val KEY_MAIN_SLASH = 191

    // 小键盘区标点符号
    /** 小数点 */
    const val KEY_NUM_POINT = 110
    /** 除号 */
    const val KEY_NUM_SLASH = 111
    /** 乘号 */
    const val KEY_NUM_MULTIPLICATION = 106
    /** 减号 */
    const val KEY_NUM_MINUS = 108
    /** 加号 */
    const val KEY_NUM_PLUS = 107

    // 功能键
    const val KEY_SYS_RQ = 44
    const val KEY_PRINT_SCREEN = KEY_SYS_RQ
    const val KEY_INSERT = 45
    const val KEY_DELETE = 46
    const val KEY_END = 35
    const val KEY_HOME = 36
    const val KEY_PAGE_UP = 33
    const val KEY_PAGE_DOWN = 34
    const val KEY_NUM_LOCK = 144
    const val KEY_SCROLL_LOCK = 145
    const val KEY_BREAK = 19
    const val KEY_PAUSE = KEY_BREAK
    const val KEY_CTRL = 17
    const val KEY_CTRL_LEFT = 162
    const val KEY_CTRL_RIGHT = 163
    const val KEY_ALT = 18
    const val KEY_WIN_LEFT = 91
    const val KEY_WIN_RIGHT = 92
    const val KEY_MENU = 93
    const val KEY_MENU_LEFT = 164
    const val KEY_MENU_RIGHT = 165
    const val KEY_BACKSPACE = 8
    const val KEY_TAB = 9
    const val KEY_CAPS_LOCK = 20
    const val KEY_SHIFT = 16
    const val KEY_SHIFT_LEFT = 160
    const val KEY_SHIFT_RIGHT = 161
    const val KEY_ENTER = 13
    const val KEY_ESC = 27
    const val KEY_UP = 38
    const val KEY_DOWN = 40
    const val KEY_LEFT = 37
    const val KEY_RIGHT = 39

    // 鼠标
    const val MOUSE_LEFT = 1
    const val MOUSE_RIGHT = 2
    const val MOUSE_CENTER = 4
    const val MOUSE_DOWN = 5
    const val MOUSE_UP = 6

    @JvmStatic
    private var keys = BooleanArray(233)
    @JvmStatic
    private var oldKeys = BooleanArray(233)     // 存储上一次的 key 值表
    @JvmStatic
    private val mousePos = IntArray(2)
    @JvmStatic
    private val keyboardListeners = LinkedList<IKeyboardListener>()
    @JvmStatic
    private val mouseListeners = LinkedList<IMouseListener>()
    @JvmStatic
    private val mousePosListeners = LinkedList<IMousePosListener>()

    /** 注册一个键盘事件 */
    @JvmStatic
    fun registryKeyboardEvent(listener: IKeyboardListener) {
        keyboardListeners.add(listener)
    }

    /** 注册一个鼠标事件 */
    @JvmStatic
    fun registryMouseEvent(listener: IMouseListener) {
        mouseListeners.add(listener)
    }

    /** 注册一个鼠标坐标事件 */
    @JvmStatic
    fun registryMousePosEvent(listener: IMousePosListener) {
        mousePosListeners.add(listener)
    }

    /** 删除一个键盘事件 */
    @JvmStatic
    fun removeKeyboardEvent(listener: IKeyboardListener) {
        keyboardListeners.remove(listener)
    }

    /** 删除一个鼠标事件 */
    fun removeMouseEvent(listener: IMouseListener) {
        mouseListeners.remove(listener)
    }

    /** 删除一个鼠标坐标事件 */
    @JvmStatic
    fun removeMousePosEvent(listener: IMousePosListener) {
        mousePosListeners.remove(listener)
    }

    /** 判断指定按键是否被按下 */
    @JvmStatic
    fun isPressed(code: Int) = keys[code]

    /** 获取鼠标下标 */
    @JvmStatic
    fun getMousePos() = Point2D(mousePos[0], mousePos[1])

    /** 更新键盘和鼠标的按键输入并触发事件 */
    @JvmStatic
    fun pushButtonEvent() {
        keys = oldKeys.apply { oldKeys = keys }
        getKeyMouseInput(keys)
        fun compare(list: List<IButtonListener>, index: Int) {
            if (keys[index]) {
                if (oldKeys[index]) list.forEach { it.onActive(index) }
                else {
                    list.forEach {
                        it.onPressed(index)
                        it.onActive(index)
                    }
                }
            } else if (oldKeys[index]) {
                list.forEach { it.onReleased(index) }
            }
        }
        for (i in 1 until 7) {
            compare(mouseListeners, i)
        }
        for (i in 8 until keys.size) {
            compare(keyboardListeners, i)
        }
    }

    /** 更新鼠标坐标并发布事件 */
    @JvmStatic
    fun pushMouseLocationEvent() {
        val oldX = mousePos[0]
        val oldY = mousePos[1]
        getMouseLocationN(ConsolePrinter.width, ConsolePrinter.height, mousePos)
        if (oldX != mousePos[0] || oldY != mousePos[1]) {
            mousePosListeners.forEach { it.onMove(mousePos[0], mousePos[1], oldX, oldY) }
        }
    }

    /** 获取按下的按键列表 */
    @JvmStatic
    private external fun getKeyMouseInput(array: BooleanArray)

    @JvmStatic
    private external fun getMouseLocationN(width: Int, height: Int, array: IntArray)

}