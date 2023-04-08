#ifndef JNI_TOP_KMAR_GAME_EVENTLISTENER_H
#define JNI_TOP_KMAR_GAME_EVENTLISTENER_H

#include <jni.h>

JNIEXPORT jint JNICALL Java_top_kmar_game_EventListener_checkKeyboardInput(
        JNIEnv *, jclass, jbooleanArray
);

#endif //JNI_TOP_KMAR_GAME_EVENTLISTENER_H
