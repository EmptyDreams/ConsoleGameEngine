#include "top_kmar_game_ConsolePrinter.h"

#include <windows.h>
#include <stdbool.h>
#include <wchar.h>
#include <handleapi.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#pragma clang diagnostic ignored "-Wc2x-extensions"

static HANDLE* buffers = NULL;
static LONG srcStyle = -1;
HANDLE stdInput;
static DWORD stdInputMode = 0;

// 判断指定位置是否为占用两格宽的字符
static bool isWideChar(HANDLE buffer, jint x, jint y) {
    WCHAR value;
    DWORD tmp = 0;
    COORD coord = {(SHORT) x, (SHORT) y};
    ReadConsoleOutputCharacterW(buffer, &value, 1, coord, &tmp);
    if (value >= 0x100) return true;
    if (value != 32 || x == 0) return false;
    --coord.X;
    ReadConsoleOutputCharacterW(buffer, &value, 1, coord, &tmp);
    return value >= 0x100;
}

/*
 * 清理指定位置的文本。
 * 如果指定位置的文本是占用两个字节的字符，会将连续的两个字节全部移除。
 */
static void clearOutput(HANDLE buffer, jint x, jint y) {
    bool wide = isWideChar(buffer, x, y);
    DWORD tmp = 0;
    COORD coord = {(SHORT) (x - wide), (SHORT) y};
    FillConsoleOutputCharacterA(buffer, ' ', 1 + wide, coord, &tmp);
}

static void fillOutput(HANDLE buffer, jchar c, jint x, jint y, jint length) {
    clearOutput(buffer, x, y);
    if (length != 1) clearOutput(buffer, x + length - 1, y);
    if (c >= 0x100) clearOutput(buffer, x + (length << 1), y);
    COORD pos = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    FillConsoleOutputCharacterW(buffer, c, length, pos, &tmp);
}

static void fillAttr(HANDLE buffer, jint attr, jint x, jint y, jint width) {
    COORD pos = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    FillConsoleOutputAttribute(buffer, attr, width, pos, &tmp);
}

static HANDLE createHandle(jint width, jint height, jint fontWidth, HWND consoleWindow) {
    HANDLE result = CreateConsoleScreenBuffer(
            GENERIC_READ | GENERIC_WRITE,
            FILE_SHARE_READ | FILE_SHARE_WRITE,
            NULL,
            CONSOLE_TEXTMODE_BUFFER,
            NULL
    );
    SetConsoleActiveScreenBuffer(result);
    // 修改字体大小
    CONSOLE_FONT_INFOEX font;
    font.cbSize = sizeof(CONSOLE_FONT_INFOEX);
    GetCurrentConsoleFontEx(result, FALSE, &font);
    font.dwFontSize.X = (SHORT) fontWidth;
    font.dwFontSize.Y = (SHORT) (fontWidth << 1);
    SetCurrentConsoleFontEx(result, FALSE, &font);
    // 修改窗体大小
    SetWindowPos(consoleWindow, NULL, 0, 0, GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN), 0);
    CONSOLE_SCREEN_BUFFER_INFOEX screen;
    screen.cbSize = sizeof(CONSOLE_SCREEN_BUFFER_INFOEX);
    GetConsoleScreenBufferInfoEx(result, &screen);
    screen.dwSize.X = (SHORT) width;
    screen.dwSize.Y = (SHORT) height;
    SetConsoleScreenBufferInfoEx(result, &screen);
    return result;
}

BOOL WINAPI CtrlHandler(DWORD _) {
    return true;
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_initN
        (JNIEnv*, jclass, jint width, jint height, jint fontWidth, jint cache, jboolean ignoreClose) {
    HWND consoleWindow = GetConsoleWindow();
    SetConsoleOutputCP(CP_UTF8);
    LONG style = GetWindowLong(consoleWindow, GWL_STYLE);
    srcStyle = style;
    style &= ~WS_MAXIMIZEBOX & ~WS_SIZEBOX;     // 禁止修改窗体大小
    style &= ~(ENABLE_WRAP_AT_EOL_OUTPUT | ENABLE_MOUSE_INPUT | DISABLE_NEWLINE_AUTO_RETURN);
    style &= ~(ENABLE_QUICK_EDIT_MODE | ENABLE_MOUSE_INPUT);
    SetWindowLong(consoleWindow, GWL_STYLE, style);
    buffers = malloc(sizeof(HANDLE) * cache);
    for (int i = 0; i != cache; ++i) {
        buffers[i] = createHandle(width, height, fontWidth, consoleWindow);
    }
    stdInput = GetStdHandle(STD_INPUT_HANDLE);
    DWORD mode = 0;
    GetConsoleMode(stdInput, &mode);
    stdInputMode = mode;
    SetConsoleMode(stdInput, mode & ~(ENABLE_ECHO_INPUT | ENABLE_LINE_INPUT));
    if (ignoreClose)
        SetConsoleCtrlHandler(CtrlHandler, TRUE);
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_flush
        (JNIEnv *, jclass, jint index) {
    HANDLE buffer = buffers[index];
    // 隐藏光标
    CONSOLE_CURSOR_INFO cci;
    cci.bVisible = FALSE;
    cci.dwSize = 1;
    SetConsoleCursorInfo(buffer, &cci);
    // 修改 active
    SetConsoleActiveScreenBuffer(buffer);
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_disposeN
        (JNIEnv *, jclass, jint size) {
    HANDLE buffer = GetStdHandle(STD_OUTPUT_HANDLE);
    HWND consoleWindow = GetConsoleWindow();
    SetWindowLong(consoleWindow, GWL_STYLE, srcStyle);
    SetConsoleActiveScreenBuffer(buffer);
    for (int i = 0; i != size; ++i) {
        CloseHandle(buffers[i]);
    }
    free(buffers);
    buffers = NULL;
    SetConsoleMode(stdInput, stdInputMode);
    stdInput = NULL;
    SetConsoleCtrlHandler(CtrlHandler, FALSE);
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_quickFillChar
        (JNIEnv *, jclass, jchar c, jint x, jint y, jint amount, jint index) {
    HANDLE buffer = buffers[index];
    COORD coord = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    if (c >= 0x100) amount >>= 1;
    FillConsoleOutputCharacterW(buffer, c, amount, coord, &tmp);
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_quickFillAttr
        (JNIEnv *, jclass, jint attr, jint x, jint y, jint amount, jint index) {
    HANDLE buffer = buffers[index];
    COORD coord = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    FillConsoleOutputAttribute(buffer, attr, amount, coord, &tmp);
}

#define fillRect Java_top_kmar_game_ConsolePrinter_fillRect

JNIEXPORT void JNICALL fillRect
        (JNIEnv *, jclass, jchar c, jint x, jint y, jint width, jint height, jint index) {
    HANDLE buffer = buffers[index];
    for (int i = 0; i != height; ++i) {
        fillOutput(buffer, c, x, y++, width);
    }
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_fillRectHollow
        (JNIEnv* env, jclass class, jchar c, jint x, jint y, jint width, jint height, jint attr, jint index) {
    if (height < 3)
        fillRect(env, class, c, x, y, width, height, index);
    else {
        HANDLE buffer = buffers[index];
        jint len = (c >= 0x100) + 1;
        jint bottom = y + height - 1;
        jint right = x + ((width - 1) << len);
        if (attr != -1) {
            fillAttr(buffer, attr, x, y, width);
            fillAttr(buffer, attr, x, bottom, width);
            for (int i = y + 1; i != bottom; ++i) {
                fillAttr(buffer, attr, x, i, len);
                fillAttr(buffer, attr, right, i, len);
            }
        }
        fillOutput(buffer, c, x, y, width);
        fillOutput(buffer, c, x, bottom, width);
        for (int i = 1; i != height - 1; ++i) {
            fillOutput(buffer, c, x, y + i, 1);
            fillOutput(buffer, c, right, y + i, 1);
        }
    }
}

#define modifyAttr Java_top_kmar_game_ConsolePrinter_modifyAttr

JNIEXPORT void JNICALL modifyAttr
        (JNIEnv *, jclass, jint attr, jint x, jint y, jint width, jint height, jint index) {
    HANDLE buffer = buffers[index];
    COORD coord = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    for (int i = 0; i != height; ++i) {
        FillConsoleOutputAttribute(buffer, attr, width, coord, &tmp);
        ++coord.Y;
    }
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawString
        (JNIEnv* env, jclass, jstring text, jint x, jint y, jint index) {
    HANDLE buffer = buffers[index];
    const char* array = (*env)->GetStringUTFChars(env, text, JNI_FALSE);
    jsize length = (*env)->GetStringUTFLength(env, text);
    COORD coord = {(SHORT) x, (SHORT) y};
    DWORD tmp = 0;
    WriteConsoleOutputCharacter(buffer, array, length, coord, &tmp);
    (*env)->ReleaseStringUTFChars(env, text, array);
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawDottedLine(
        JNIEnv* env, jclass class,
        jchar c, jint charWidth,
        jint x, jint y, jint width, jint height,
        jint lineLength, jint airLength, jint offset,
        jint index
) {
    jint right = x + width;
    --charWidth;
    if (offset != 0) {
        if (offset < lineLength) {
            jint length = min(lineLength - offset, width);
            fillRect(env, class, c, x, y, length >> charWidth, height, index);
            x += length;
            length = min(airLength, width - length);
            fillRect(env, class, ' ', x, y, length, height, index);
            x += length;
        } else {
            jint length = min(lineLength + airLength - offset, width);
            fillRect(env, class, ' ', x, y, length, height, index);
            x += length;
        }
    }
    while (true) {
        jint length = min(lineLength, right - x);
        if (length == 0) break;
        fillRect(env, class, c, x, y, length >> charWidth, height, index);
        x += length;
        length = min(airLength, right - x);
        if (length == 0) break;
        fillRect(env, class, ' ', x, y, length, height, index);
        x += length;
    }
}

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawVerticalDottedLine(
        JNIEnv* env, jclass class,
        jchar c, jint charWidth,
        jint x, jint y, jint width, jint height,
        jint lineLength, jint airLength, jint offset,
        jint index
) {
    jint bottom = y + height;
    --charWidth;
    if (offset != 0) {
        if (offset < lineLength) {
            jint h = min(lineLength - offset, height);
            fillRect(env, class, c, x, y, width >> charWidth, h, index);
            y += h;
            h = min(airLength, height - h);
            fillRect(env, class, ' ', x, y, width, h, index);
            y += h;
        } else {
            jint h = min(lineLength + airLength - offset, height);
            fillRect(env, class, ' ', x, y, width, h, index);
            y += h;
        }
    }
    while (true) {
        jint h = min(lineLength, bottom - y);
        if (h == 0) break;
        fillRect(env, class, c, x, y, width >> charWidth, h, index);
        y += h;
        h = min(airLength, bottom - y);
        if (h == 0) break;
        fillRect(env, class, ' ', x, y, width, h, index);
        y += h;
    }
}

#pragma clang diagnostic pop