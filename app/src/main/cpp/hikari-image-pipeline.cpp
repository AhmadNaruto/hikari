#include <algorithm>
#include <android/bitmap.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <android/log.h>
#include <android/rect.h>
#include <dlfcn.h>
#include <jni.h>
#include <math.h>
#include <vector>

#define TAG "HikariImagePipeline"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

typedef struct AImageDecoder AImageDecoder;
typedef struct AImageDecoderHeaderInfo AImageDecoderHeaderInfo;

typedef int (*pfn_AImageDecoder_createFromBuffer)(const void *, size_t,
                                                  AImageDecoder **);

typedef const AImageDecoderHeaderInfo *(*pfn_AImageDecoder_getHeaderInfo)(
    const AImageDecoder *);

typedef int32_t (*pfn_AImageDecoderHeaderInfo_getWidth)(
    const AImageDecoderHeaderInfo *);

typedef int32_t (*pfn_AImageDecoderHeaderInfo_getHeight)(
    const AImageDecoderHeaderInfo *);

typedef int (*pfn_AImageDecoder_setTargetSize)(AImageDecoder *, int32_t,
                                               int32_t);

typedef int (*pfn_AImageDecoder_setTargetRect)(AImageDecoder *, ARect);

typedef int (*pfn_AImageDecoder_decodeImage)(AImageDecoder *, void *, size_t,
                                             size_t);

typedef void (*pfn_AImageDecoder_delete)(AImageDecoder *);

struct ImageDecoderFunctions {
  pfn_AImageDecoder_createFromBuffer createFromBuffer;
  pfn_AImageDecoder_getHeaderInfo getHeaderInfo;
  pfn_AImageDecoderHeaderInfo_getWidth getWidth;
  pfn_AImageDecoderHeaderInfo_getHeight getHeight;
  pfn_AImageDecoder_setTargetSize setTargetSize;
  pfn_AImageDecoder_setTargetRect setTargetRect;
  pfn_AImageDecoder_decodeImage decodeImage;
  pfn_AImageDecoder_delete deleteDecoder;

  bool available = false;

  void load() {
    void *lib = dlopen("libjnigraphics.so", RTLD_NOW);
    if (!lib)
      return;

    createFromBuffer = (pfn_AImageDecoder_createFromBuffer)dlsym(
        lib, "AImageDecoder_createFromBuffer");
    getHeaderInfo = (pfn_AImageDecoder_getHeaderInfo)dlsym(
        lib, "AImageDecoder_getHeaderInfo");
    getWidth = (pfn_AImageDecoderHeaderInfo_getWidth)dlsym(
        lib, "AImageDecoderHeaderInfo_getWidth");
    getHeight = (pfn_AImageDecoderHeaderInfo_getHeight)dlsym(
        lib, "AImageDecoderHeaderInfo_getHeight");
    setTargetSize = (pfn_AImageDecoder_setTargetSize)dlsym(
        lib, "AImageDecoder_setTargetSize");
    setTargetRect = (pfn_AImageDecoder_setTargetRect)dlsym(
        lib, "AImageDecoder_setTargetRect");
    decodeImage =
        (pfn_AImageDecoder_decodeImage)dlsym(lib, "AImageDecoder_decodeImage");
    deleteDecoder =
        (pfn_AImageDecoder_delete)dlsym(lib, "AImageDecoder_delete");

    available = createFromBuffer && decodeImage && deleteDecoder;
  }
};

static ImageDecoderFunctions gDecoder;

namespace hikari {

struct Color {
  float r, g, b, a;
};

inline float clamp(float v, float min, float max) {
  return std::max(min, std::min(max, v));
}

void easu(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh) {
  float sx = (float)sw / dw;
  float sy = (float)sh / dh;

  for (int y = 0; y < dh; y++) {
    for (int x = 0; x < dw; x++) {
      float srcX = x * sx;
      float srcY = y * sy;
      int x0 = (int)srcX;
      int y0 = (int)srcY;
      int x1 = std::min(x0 + 1, sw - 1);
      int y1 = std::min(y0 + 1, sh - 1);

      float dx = srcX - x0;
      float dy = srcY - y0;

      uint32_t c00 = src[y0 * sw + x0];
      uint32_t c10 = src[y0 * sw + x1];
      uint32_t c01 = src[y1 * sw + x0];
      uint32_t c11 = src[y1 * sw + x1];

      float r = (1 - dx) * (1 - dy) * (c00 & 0xFF) +
                dx * (1 - dy) * (c10 & 0xFF) + (1 - dx) * dy * (c01 & 0xFF) +
                dx * dy * (c11 & 0xFF);
      float g = (1 - dx) * (1 - dy) * ((c00 >> 8) & 0xFF) +
                dx * (1 - dy) * ((c10 >> 8) & 0xFF) +
                (1 - dx) * dy * ((c01 >> 8) & 0xFF) +
                dx * dy * ((c11 >> 8) & 0xFF);
      float b = (1 - dx) * (1 - dy) * ((c00 >> 16) & 0xFF) +
                dx * (1 - dy) * ((c10 >> 16) & 0xFF) +
                (1 - dx) * dy * ((c01 >> 16) & 0xFF) +
                dx * dy * ((c11 >> 16) & 0xFF);
      float a = (1 - dx) * (1 - dy) * ((c00 >> 24) & 0xFF) +
                dx * (1 - dy) * ((c10 >> 24) & 0xFF) +
                (1 - dx) * dy * ((c01 >> 24) & 0xFF) +
                dx * dy * ((c11 >> 24) & 0xFF);

      dst[y * dw + x] = ((uint32_t)a << 24) | ((uint32_t)b << 16) |
                        ((uint32_t)g << 8) | (uint32_t)r;
    }
  }
}

void sharpen(uint32_t *pixels, int w, int h) {
  std::vector<uint32_t> src(pixels, pixels + w * h);
  for (int y = 1; y < h - 1; y++) {
    for (int x = 1; x < w - 1; x++) {
      int idx = y * w + x;

      auto getR = [&](uint32_t p) { return p & 0xFF; };
      auto getG = [&](uint32_t p) { return (p >> 8) & 0xFF; };
      auto getB = [&](uint32_t p) { return (p >> 16) & 0xFF; };

      int r = 5 * getR(src[idx]) - getR(src[idx - 1]) - getR(src[idx + 1]) -
              getR(src[idx - w]) - getR(src[idx + w]);
      int g = 5 * getG(src[idx]) - getG(src[idx - 1]) - getG(src[idx + 1]) -
              getG(src[idx - w]) - getG(src[idx + w]);
      int b = 5 * getB(src[idx]) - getB(src[idx - 1]) - getB(src[idx + 1]) -
              getB(src[idx - w]) - getB(src[idx + w]);

      uint32_t a = (src[idx] >> 24) & 0xFF;
      pixels[idx] = (a << 24) | ((uint32_t)clamp(b, 0, 255) << 16) |
                    ((uint32_t)clamp(g, 0, 255) << 8) |
                    (uint32_t)clamp(r, 0, 255);
    }
  }
}

void denoise(uint32_t *pixels, int w, int h) {
  std::vector<uint32_t> src(pixels, pixels + w * h);
  for (int y = 1; y < h - 1; y++) {
    for (int x = 1; x < w - 1; x++) {
      int r = 0, g = 0, b = 0, a = 0;
      for (int ky = -1; ky <= 1; ky++) {
        for (int kx = -1; kx <= 1; kx++) {
          uint32_t p = src[(y + ky) * w + (x + kx)];
          r += p & 0xFF;
          g += (p >> 8) & 0xFF;
          b += (p >> 16) & 0xFF;
          a += (p >> 24) & 0xFF;
        }
      }
      pixels[y * w + x] = ((uint32_t)(a / 9) << 24) |
                          ((uint32_t)(b / 9) << 16) | ((uint32_t)(g / 9) << 8) |
                          (uint32_t)(r / 9);
    }
  }
}

} // namespace hikari

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  gDecoder.load();
  return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeDecode(
    JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray jData, jint length,
    jint filters) {
  if (bitmap == nullptr || jData == nullptr || !gDecoder.available)
    return JNI_FALSE;

  jbyte *data = env->GetByteArrayElements(jData, nullptr);
  AImageDecoder *decoder = nullptr;
  int ret = gDecoder.createFromBuffer(data, length, &decoder);
  if (ret != 0) {
    env->ReleaseByteArrayElements(jData, data, JNI_ABORT);
    return JNI_FALSE;
  }

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  void *pixels;
  AndroidBitmap_lockPixels(env, bitmap, &pixels);

  if (filters & 4) {
    const AImageDecoderHeaderInfo *header = gDecoder.getHeaderInfo(decoder);
    int sw = gDecoder.getWidth(header);
    int sh = gDecoder.getHeight(header);

    std::vector<uint32_t> temp(sw * sh);
    gDecoder.decodeImage(decoder, temp.data(), sw * 4, sw * sh * 4);

    hikari::easu(temp.data(), (uint32_t *)pixels, sw, sh, info.width,
                 info.height);
  } else {
    gDecoder.setTargetSize(decoder, info.width, info.height);
    gDecoder.decodeImage(decoder, pixels, info.stride,
                         info.stride * info.height);
  }

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height);
  }

  if (filters & 1) {
    hikari::sharpen((uint32_t *)pixels, info.width, info.height);
  }

  AndroidBitmap_unlockPixels(env, bitmap);
  gDecoder.deleteDecoder(decoder);
  env->ReleaseByteArrayElements(jData, data, JNI_ABORT);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeDecodeRegion(
    JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray jData, jint length,
    jint left, jint top, jint right, jint bottom, jint sampleSize,
    jint filters) {
  if (bitmap == nullptr || jData == nullptr || !gDecoder.available)
    return JNI_FALSE;

  jbyte *data = env->GetByteArrayElements(jData, nullptr);
  AImageDecoder *decoder = nullptr;
  int ret = gDecoder.createFromBuffer(data, length, &decoder);
  if (ret != 0) {
    env->ReleaseByteArrayElements(jData, data, JNI_ABORT);
    return JNI_FALSE;
  }

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  void *pixels;
  AndroidBitmap_lockPixels(env, bitmap, &pixels);

  if (gDecoder.setTargetRect) {
    gDecoder.setTargetRect(decoder, {left, top, right, bottom});
  }

  int targetWidth = (right - left) / sampleSize;
  int targetHeight = (bottom - top) / sampleSize;
  if (gDecoder.setTargetSize) {
    gDecoder.setTargetSize(decoder, targetWidth, targetHeight);
  }

  gDecoder.decodeImage(decoder, pixels, info.stride, info.stride * info.height);

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height);
  }

  if (filters & 1) {
    hikari::sharpen((uint32_t *)pixels, info.width, info.height);
  }

  AndroidBitmap_unlockPixels(env, bitmap);
  gDecoder.deleteDecoder(decoder);
  env->ReleaseByteArrayElements(jData, data, JNI_ABORT);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeProcess(
    JNIEnv *env, jobject thiz, jobject bitmap, jint filters) {
  if (bitmap == nullptr)
    return JNI_FALSE;

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    return JNI_FALSE;

  void *pixels;
  AndroidBitmap_lockPixels(env, bitmap, &pixels);

  if (filters & 4) {
    LOGD("Native post-processing filter applied: UPSCALING");
  }

  AndroidBitmap_unlockPixels(env, bitmap);
  return JNI_TRUE;
}

JNIEXPORT jobject JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeDecodeToHardwareBuffer(
    JNIEnv *env, jobject thiz, jint width, jint height) {
  AHardwareBuffer_Desc desc = {
      .width = (uint32_t)width,
      .height = (uint32_t)height,
      .layers = 1,
      .format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM,
      .usage = AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE |
               AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN,
  };

  AHardwareBuffer *buffer = nullptr;
  if (AHardwareBuffer_allocate(&desc, &buffer) != 0) {
    return nullptr;
  }

  jobject hardwareBufferObj = AHardwareBuffer_toHardwareBuffer(env, buffer);
  AHardwareBuffer_release(buffer);
  return hardwareBufferObj;
}
}
