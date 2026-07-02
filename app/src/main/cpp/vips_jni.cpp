#include <jni.h>
#include <string>
#include <android/log.h>
#include <vips/vips.h>

#define LOG_TAG "VipsJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_init(JNIEnv *env, jclass clazz) {
    if (VIPS_INIT("Hikari") != 0) {
        LOGE("Failed to init vips: %s", vips_error_buffer());
        vips_error_clear();
        return JNI_FALSE;
    }
    LOGI("Vips initialized successfully, version: %s", vips_version_string());
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_getVersion(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(vips_version_string());
}

JNIEXPORT jbyteArray JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_compressJpeg(JNIEnv *env, jclass clazz, 
    jbyteArray input, jint quality) {
    
    jsize len = env->GetArrayLength(input);
    jbyte *data = env->GetByteArrayElements(input, nullptr);
    
    VipsImage *image = vips_image_new_from_buffer(data, len, "", nullptr);
    if (!image) {
        LOGE("Failed to load image: %s", vips_error_buffer());
        vips_error_clear();
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    void *outBuf = nullptr;
    size_t outLen = 0;
    
    if (vips_jpegsave_buffer(image, &outBuf, &outLen, "Q", quality, nullptr) != 0) {
        LOGE("Failed to save jpeg: %s", vips_error_buffer());
        vips_error_clear();
        g_object_unref(image);
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(outLen);
    env->SetByteArrayRegion(result, 0, outLen, (jbyte*)outBuf);
    
    g_free(outBuf);
    g_object_unref(image);
    env->ReleaseByteArrayElements(input, data, 0);
    
    LOGI("Compressed JPEG: %d -> %zu bytes (Q=%d)", len, outLen, quality);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_compressWebp(JNIEnv *env, jclass clazz, 
    jbyteArray input, jint quality) {
    
    jsize len = env->GetArrayLength(input);
    jbyte *data = env->GetByteArrayElements(input, nullptr);
    
    VipsImage *image = vips_image_new_from_buffer(data, len, "", nullptr);
    if (!image) {
        LOGE("Failed to load image: %s", vips_error_buffer());
        vips_error_clear();
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    void *outBuf = nullptr;
    size_t outLen = 0;
    
    if (vips_webpsave_buffer(image, &outBuf, &outLen, "Q", quality, nullptr) != 0) {
        LOGE("Failed to save webp: %s", vips_error_buffer());
        vips_error_clear();
        g_object_unref(image);
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(outLen);
    env->SetByteArrayRegion(result, 0, outLen, (jbyte*)outBuf);
    
    g_free(outBuf);
    g_object_unref(image);
    env->ReleaseByteArrayElements(input, data, 0);
    
    LOGI("Compressed WebP: %d -> %zu bytes (Q=%d)", len, outLen, quality);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_compressPng(JNIEnv *env, jclass clazz, 
    jbyteArray input, jint compression) {
    
    jsize len = env->GetArrayLength(input);
    jbyte *data = env->GetByteArrayElements(input, nullptr);
    
    VipsImage *image = vips_image_new_from_buffer(data, len, "", nullptr);
    if (!image) {
        LOGE("Failed to load image: %s", vips_error_buffer());
        vips_error_clear();
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    void *outBuf = nullptr;
    size_t outLen = 0;
    
    if (vips_pngsave_buffer(image, &outBuf, &outLen, "compression", compression, nullptr) != 0) {
        LOGE("Failed to save png: %s", vips_error_buffer());
        vips_error_clear();
        g_object_unref(image);
        env->ReleaseByteArrayElements(input, data, 0);
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(outLen);
    env->SetByteArrayRegion(result, 0, outLen, (jbyte*)outBuf);
    
    g_free(outBuf);
    g_object_unref(image);
    env->ReleaseByteArrayElements(input, data, 0);
    
    LOGI("Compressed PNG: %d -> %zu bytes (compression=%d)", len, outLen, compression);
    return result;
}

JNIEXPORT void JNICALL
Java_tachiyomi_core_common_util_system_VipsNative_nativeShutdown(JNIEnv *env, jclass clazz) {
    vips_shutdown();
    LOGI("Vips shutdown");
}

} // extern "C"
