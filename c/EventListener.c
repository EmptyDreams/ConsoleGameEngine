#include "top_kmar_game_EventListener.h"
#include <windows.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wc2x-extensions"

extern HANDLE stdInput;

JNIEXPORT void JNICALL Java_top_kmar_game_EventListener_getKeyMouseInput(
        JNIEnv* env, jclass, jbooleanArray boolArray
) {
    jboolean* array = (*env)->GetBooleanArrayElements(env, boolArray, FALSE);
    for (int i = 1; i != 223; ++i) {
        array[i] = GetAsyncKeyState(i) != 0;
    }
    (*env)->ReleaseBooleanArrayElements(env, boolArray, array, 0);
}

JNIEXPORT void JNICALL Java_top_kmar_game_EventListener_getMouseLocationN(
        JNIEnv* env, jclass, jint width, jint height, jintArray intArray
) {
    HWND console = GetConsoleWindow();
    if (console != GetForegroundWindow()) return;
    jint* array = (*env)->GetIntArrayElements(env, intArray, FALSE);
    POINT pos;
    GetCursorPos(&pos);
    RECT rect;
    GetClientRect(console, &rect);
    ScreenToClient(console, &pos);
    if (pos.x >= 0 && pos.x < rect.right) {
        int unitX = rect.right / width;
        array[0] = pos.x / unitX;
    }
    if (pos.y >= 0 && pos.y < rect.bottom) {
        int unitY = rect.bottom / height;
        array[1] = pos.y / unitY;
    }
    (*env)->ReleaseIntArrayElements(env, intArray, array, 0);
}

#pragma clang diagnostic pop