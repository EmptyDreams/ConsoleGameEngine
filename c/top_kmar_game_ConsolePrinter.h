/* Header for class top_kmar_game_ConsolePrinter */

#ifndef _Included_top_kmar_game_ConsolePrinter
#define _Included_top_kmar_game_ConsolePrinter
#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

/*
 * Class:     top_kmar_game_ConsolePrinter
 * Method:    init
 */
JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_initN
  (JNIEnv *, jclass, jint, jint, jint, jint, jboolean);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_flush
        (JNIEnv *, jclass, jint);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_disposeN
        (JNIEnv *, jclass, jint);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_quickFillChar
        (JNIEnv *, jclass, jchar, jint, jint, jint, jint);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_quickFillAttr
        (JNIEnv *, jclass, jint, jint, jint, jint, jint);

/*
 * Class:     top_kmar_game_ConsolePrinter
 * Method:    fillRect
 */
JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_fillRect
        (JNIEnv *, jclass, jchar, jint, jint, jint, jint, jint);

/*
 * Class:     top_kmar_game_ConsolePrinter
 * Method:    fillRectHollow
 */
JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_fillRectHollow
        (JNIEnv *, jclass, jchar, jint, jint, jint, jint, jint, jint);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_modifyAttr
        (JNIEnv *, jclass, jint, jint, jint, jint, jint, jint);

/*
 * Class:     top_kmar_game_ConsolePrinter
 * Method:    fillRectHollow
 */
JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawString
        (JNIEnv *, jclass, jstring, jint, jint, jint);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawDottedLine(
        JNIEnv *, jclass,
        jchar c, jint charWidth,
        jint x, jint y, jint width, jint height,
        jint lineLength, jint airLength, jint offset,
        jint index
);

JNIEXPORT void JNICALL Java_top_kmar_game_ConsolePrinter_drawVerticalDottedLine(
        JNIEnv *, jclass,
        jchar c, jint charWidth,
        jint x, jint y, jint width, jint height,
        jint lineLength, jint airLength, jint offset,
        jint index
);

#ifdef __cplusplus
}
#endif
#endif
