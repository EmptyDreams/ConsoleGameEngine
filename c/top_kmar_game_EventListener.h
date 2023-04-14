#ifndef JNI_TOP_KMAR_GAME_EVENTLISTENER_H
#define JNI_TOP_KMAR_GAME_EVENTLISTENER_H

#include <jni.h>

JNIEXPORT void JNICALL Java_top_kmar_game_EventListener_getKeyMouseInput(
        JNIEnv *, jclass, jbooleanArray
);

JNIEXPORT void JNICALL Java_top_kmar_game_EventListener_getMouseLocationN(
        JNIEnv *, jclass, jint, jint, jintArray
);

#endif //JNI_TOP_KMAR_GAME_EVENTLISTENER_H
