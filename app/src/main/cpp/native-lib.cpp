#include <jni.h>
#include <string.h>

extern "C" {
JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_ui_activity_main_MainActivity_00024Companion_baseUrl(
            JNIEnv *env, jobject thiz
    ) {
        const char *baseUrl = "https://privynoteapp.my.id/";
        return env->NewStringUTF(baseUrl);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_ui_activity_login_LoginActivity_webKey(
            JNIEnv *env, jobject thiz
    ) {
        const char *web_key = "696494720728-gpfh2n19b19jj8ajpdnt3q1im1sqp9ra.apps.googleusercontent.com";
        return env->NewStringUTF(web_key);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_ui_activity_register_RegisterActivity_webKey(
            JNIEnv *env, jobject thiz
    ) {
        const char *web_key = "696494720728-gpfh2n19b19jj8ajpdnt3q1im1sqp9ra.apps.googleusercontent.com";
        return env->NewStringUTF(web_key);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_core_data_api_API_getSha256Pin(
            JNIEnv *env, jobject thiz
    ) {
        const char *sha256Pin = "sha256/VD2li3hvVZf41friOwQgMqCa9JlPTtclLOKXwlQPX8Q=";
        return env->NewStringUTF(sha256Pin);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_core_data_api_API_getNakedHost(
            JNIEnv *env, jobject thiz
    ) {
        const char *nakedHost = "privynoteapp.my.id";
        return env->NewStringUTF(nakedHost);
    }

    JNIEXPORT jstring JNICALL
    Java_com_juanarton_privynote_core_utils_Cryptography_00024Companion_publicKey(
            JNIEnv *env, jobject thiz
    ) {
        const char *publicKey = "CM745wsS8wEK5wEKPXR5cGUuZ29vZ2xlYXBpcy5jb20vZ29vZ2xlLmNyeXB0by50aW5rLkVjaWVzQWVhZEhrZGZQdWJsaWNLZXkSowESXAoECAIQAxJSElAKOHR5cGUuZ29vZ2xlYXBpcy5jb20vZ29vZ2xlLmNyeXB0by50aW5rLkFlc0N0ckhtYWNBZWFkS2V5EhIKBgoCCBAQEBIICgQIAxAQECAYARgBGiAnMK3NhFHavAIadY/L52XhjxjZmCCCWBy1scXreoDCSCIhAKaW44ndb0YJJtOjY+BYCOcyUz+f2231pn6RNzJZRPfPGAMQARjO+OcLIAE=";
        return env->NewStringUTF(publicKey);
    }
}
