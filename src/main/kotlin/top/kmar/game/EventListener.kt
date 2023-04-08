package top.kmar.game

/**
 * 事件监听器
 */
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

    // 主键盘区标点符号（如果有无 SHIFT 符号不相同，以未按 SHIFT 时的符号命名）
    const val KEY_BACK_QUOTE = 192              // 反引号
    const val KEY_MAIN_MINUS = 189              // 减号
    const val KEY_EQUAL = 187                   // 等号
    const val KEY_BRACKET_SQUARE_LEFT = 219     // 左方括号
    const val KEY_BRACKET_SQUARE_RIGHT = 221    // 右方括号
    const val KEY_SEMICOLON = 186               // 分号
    const val KEY_QUOTE = 222                   // 引号
    const val KEY_BACKSLASH = 220               // 反斜杠
    const val KEY_COMMA = 188                   // 逗号
    const val KEY_MAIN_POINT = 190              // 句号
    const val KEY_MAIN_SLASH = 191              // 斜杠

    // 小键盘区标点符号
    const val KEY_NUM_POINT = 110               // 小数点
    const val KEY_NUM_SLASH = 111               // 除号
    const val KEY_NUM_MULTIPLICATION = 106      // 乘号
    const val KEY_NUM_MINUS = 108               // 减号
    const val KEY_NUM_PLUS = 107                // 加号

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
    const val KEY_ALT = 18
    const val KEY_WIN = 91
    const val KEY_MENU = 93
    const val KEY_BACKSPACE = 8
    const val KEY_TAB = 9
    const val KEY_CAPS_LOCK = 20
    const val KEY_SHIFT = 16
    const val KEY_ENTER = 13
    const val KEY_ESC = 27
    const val KEY_UP = 38
    const val KEY_DOWN = 40
    const val KEY_LEFT = 37
    const val KEY_RIGHT = 39

    @JvmStatic
    val keys = BooleanArray(223)

    @JvmStatic
    fun checkKeyboardInput() = checkKeyboardInput(keys)

    /** 获取按下的按键列表 */
    @JvmStatic
    private external fun checkKeyboardInput(array: BooleanArray)

}