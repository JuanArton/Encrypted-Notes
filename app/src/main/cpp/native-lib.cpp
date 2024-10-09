#include <jni.h>
#include <string.h>

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_juanarton_encnotes_ui_main_MainActivity_keyWork(
        JNIEnv *env, jobject thiz
    ) {
        const char *hello = "10.0.2.2";
        return env->NewStringUTF(hello);
    }
}
