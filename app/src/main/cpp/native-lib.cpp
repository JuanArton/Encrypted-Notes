#include <jni.h>
#include <string.h>

extern "C" {
JNIEXPORT jstring JNICALL
    Java_com_juanarton_encnotes_ui_activity_main_MainActivity_00024Companion_baseUrl(
            JNIEnv *env, jobject thiz
    ) {
        const char *baseUrl = "http://192.168.0.100:5500/";
        return env->NewStringUTF(baseUrl);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_encnotes_ui_activity_login_LoginActivity_webKey(
            JNIEnv *env, jobject thiz
    ) {
        const char *web_key = "696494720728-gpfh2n19b19jj8ajpdnt3q1im1sqp9ra.apps.googleusercontent.com";
        return env->NewStringUTF(web_key);
    }
}
