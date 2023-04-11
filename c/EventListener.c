#include "top_kmar_game_EventListener.h"
#include <windows.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wc2x-extensions"

extern HANDLE stdInput;

JNIEXPORT jint JNICALL Java_top_kmar_game_EventListener_getKeyMouseInput(
        JNIEnv * env, jclass, jbooleanArray boolArray
) {
    jboolean* array = (*env)->GetBooleanArrayElements(env, boolArray, FALSE);
    for (int i = 1; i != 223; ++i) {
        array[i] = GetAsyncKeyState(i) != 0;
    }
    (*env)->ReleaseBooleanArrayElements(env, boolArray, array, 0);
}

#pragma clang diagnostic pop