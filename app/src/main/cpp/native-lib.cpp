#include <jni.h>
#include <string.h>

extern "C" {
JNIEXPORT jstring JNICALL
    Java_com_juanarton_encnotes_ui_main_MainActivity_keyWork(
            JNIEnv *env, jobject thiz
    ) {
        const char *baseUrl = "10.0.2.2";
        return env->NewStringUTF(baseUrl);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_encnotes_ui_login_LoginActivity_webKey(
            JNIEnv *env, jobject thiz
    ) {
        const char *web_key = "696494720728-gpfh2n19b19jj8ajpdnt3q1im1sqp9ra.apps.googleusercontent.com";
        return env->NewStringUTF(web_key);
    }
}
