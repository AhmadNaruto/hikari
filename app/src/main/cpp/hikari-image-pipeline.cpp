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
#include <cstring>
#include <arm_neon.h>
#include "avir.h"
#include "lancir.h"

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

typedef int (*pfn_AImageDecoder_setAndroidBitmapFormat)(AImageDecoder *,
                                                        int32_t);

struct ImageDecoderFunctions {
  pfn_AImageDecoder_createFromBuffer createFromBuffer;
  pfn_AImageDecoder_getHeaderInfo getHeaderInfo;
  pfn_AImageDecoderHeaderInfo_getWidth getWidth;
  pfn_AImageDecoderHeaderInfo_getHeight getHeight;
  pfn_AImageDecoder_setTargetSize setTargetSize;
  pfn_AImageDecoder_setTargetRect setTargetRect;
  pfn_AImageDecoder_decodeImage decodeImage;
  pfn_AImageDecoder_delete deleteDecoder;
  pfn_AImageDecoder_setAndroidBitmapFormat setAndroidBitmapFormat;

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
    setAndroidBitmapFormat = (pfn_AImageDecoder_setAndroidBitmapFormat)dlsym(
        lib, "AImageDecoder_setAndroidBitmapFormat");

    available = createFromBuffer && decodeImage && deleteDecoder &&
                setAndroidBitmapFormat;
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

struct FloatColor {
  float r, g, b, a;
};

inline uint32_t getPixelClamp(const uint32_t *src, int x, int y, int sw,
                              int sh) {
  x = std::max(0, std::min(x, sw - 1));
  y = std::max(0, std::min(y, sh - 1));
  return src[y * sw + x];
}

inline FloatColor getFloatColor(uint32_t pixel) {
  const float inv255 = 1.0f / 255.0f;
  return {(pixel & 0xFF) * inv255, ((pixel >> 8) & 0xFF) * inv255,
          ((pixel >> 16) & 0xFF) * inv255, ((pixel >> 24) & 0xFF) * inv255};
}

inline float getLuma(const FloatColor &c) {
  return (c.r + 2.0f * c.g + c.b) * 0.25f;
}

inline float APrxLoRcpF1(float a) {
  uint32_t u;
  std::memcpy(&u, &a, sizeof(float));
  u = 0x7ef07ebb - u;
  float f;
  std::memcpy(&f, &u, sizeof(float));
  return f;
}

inline float APrxLoRsqF1(float a) {
  uint32_t u;
  std::memcpy(&u, &a, sizeof(float));
  u = 0x5f347d74 - (u >> 1);
  float f;
  std::memcpy(&f, &u, sizeof(float));
  return f;
}

inline float APrxMedRcpF1(float a) {
  uint32_t u;
  std::memcpy(&u, &a, sizeof(float));
  u = 0x7ef19fff - u;
  float f;
  std::memcpy(&f, &u, sizeof(float));
  return f * (-f * a + 2.0f);
}

// NEON helper for APrxLoRcpF1
inline float32x4_t APrxLoRcpF1Neon(float32x4_t a) {
  uint32x4_t u = vreinterpretq_u32_f32(a);
  uint32x4_t res = vsubq_u32(vdupq_n_u32(0x7ef07ebb), u);
  return vreinterpretq_f32_u32(res);
}

// Helper to unpack 4 pixels to float colors
inline void unpack_u8x4_to_float(uint8x8_t u8_val, float32x4_t &f_val) {
  uint16x4_t u16_val = vget_low_u16(vmovl_u8(u8_val));
  uint32x4_t u32_val = vmovl_u16(u16_val);
  f_val = vmulq_f32(vcvtq_f32_u32(u32_val), vdupq_n_f32(1.0f / 255.0f));
}

// Vectorized FsrEasuTap for 4 taps at once
inline void FsrEasuTap4(
    float32x4_t &aR, float32x4_t &aG, float32x4_t &aB, float32x4_t &aA, float32x4_t &aW,
    float32x4_t offX, float32x4_t offY,
    float32x4_t dirX, float32x4_t dirY,
    float32x4_t lenX, float32x4_t lenY,
    float32x4_t lob, float32x4_t clp,
    float32x4_t cR, float32x4_t cG, float32x4_t cB, float32x4_t cA) {

  // vx = offX * dirX + offY * dirY
  float32x4_t vx = vmlaq_f32(vmulq_f32(offX, dirX), offY, dirY);
  // vy = offX * -dirY + offY * dirX
  float32x4_t vy = vmlaq_f32(vmulq_f32(offX, vnegq_f32(dirY)), offY, dirX);

  // vx *= lenX
  vx = vmulq_f32(vx, lenX);
  // vy *= lenY
  vy = vmulq_f32(vy, lenY);

  // d2 = vx * vx + vy * vy
  float32x4_t d2 = vmlaq_f32(vmulq_f32(vx, vx), vy, vy);
  // d2 = min(d2, clp)
  d2 = vminq_f32(d2, clp);

  // wB = 0.4f * d2 - 1.0f
  float32x4_t wB = vmlaq_f32(vdupq_n_f32(-1.0f), d2, vdupq_n_f32(0.4f));
  // wA = lob * d2 - 1.0f
  float32x4_t wA = vmlaq_f32(vdupq_n_f32(-1.0f), d2, lob);

  // wB *= wB
  wB = vmulq_f32(wB, wB);
  // wA *= wA
  wA = vmulq_f32(wA, wA);

  // wB = 1.5625f * wB - 0.5625f
  wB = vmlaq_f32(vdupq_n_f32(-0.5625f), wB, vdupq_n_f32(1.5625f));

  // w = wB * wA
  float32x4_t w = vmulq_f32(wB, wA);

  // Accumulate
  aR = vmlaq_f32(aR, cR, w);
  aG = vmlaq_f32(aG, cG, w);
  aB = vmlaq_f32(aB, cB, w);
  aA = vmlaq_f32(aA, cA, w);
  aW = vaddq_f32(aW, w);
}

void easu(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh) {
  float sx = (float)sw / dw;
  float sy = (float)sh / dh;
  for (int y = 0; y < dh; y++) {
    for (int x = 0; x < dw; x++) {
      float srcX = (x + 0.5f) * sx - 0.5f;
      float srcY = (y + 0.5f) * sy - 0.5f;
      int x0 = (int)std::floor(srcX);
      int y0 = (int)std::floor(srcY);
      float ppX = srcX - x0;
      float ppY = srcY - y0;

      uint32_t p_b = getPixelClamp(src, x0, y0 - 1, sw, sh);
      uint32_t p_c = getPixelClamp(src, x0 + 1, y0 - 1, sw, sh);
      uint32_t p_e = getPixelClamp(src, x0 - 1, y0, sw, sh);
      uint32_t p_f = getPixelClamp(src, x0, y0, sw, sh);
      uint32_t p_g = getPixelClamp(src, x0 + 1, y0, sw, sh);
      uint32_t p_h = getPixelClamp(src, x0 + 2, y0, sw, sh);
      uint32_t p_i = getPixelClamp(src, x0 - 1, y0 + 1, sw, sh);
      uint32_t p_j = getPixelClamp(src, x0, y0 + 1, sw, sh);
      uint32_t p_k = getPixelClamp(src, x0 + 1, y0 + 1, sw, sh);
      uint32_t p_l = getPixelClamp(src, x0 + 2, y0 + 1, sw, sh);
      uint32_t p_n = getPixelClamp(src, x0, y0 + 2, sw, sh);
      uint32_t p_o = getPixelClamp(src, x0 + 1, y0 + 2, sw, sh);

      const float l_scale = 0.25f / 255.0f;
      auto getLumaFromPixel = [l_scale](uint32_t p) {
        return ((p & 0xFF) + 2.0f * ((p >> 8) & 0xFF) + ((p >> 16) & 0xFF)) * l_scale;
      };

      float l_b = getLumaFromPixel(p_b);
      float l_c = getLumaFromPixel(p_c);
      float l_e = getLumaFromPixel(p_e);
      float l_f = getLumaFromPixel(p_f);
      float l_g = getLumaFromPixel(p_g);
      float l_h = getLumaFromPixel(p_h);
      float l_i = getLumaFromPixel(p_i);
      float l_j = getLumaFromPixel(p_j);
      float l_k = getLumaFromPixel(p_k);
      float l_l = getLumaFromPixel(p_l);
      float l_n = getLumaFromPixel(p_n);
      float l_o = getLumaFromPixel(p_o);

      float32x4_t w = {
        (1.0f - ppX) * (1.0f - ppY),
        ppX * (1.0f - ppY),
        (1.0f - ppX) * ppY,
        ppX * ppY
      };

      float32x4_t v_lA = {l_b, l_c, l_f, l_g};
      float32x4_t v_lB = {l_e, l_f, l_i, l_j};
      float32x4_t v_lC = {l_f, l_g, l_j, l_k};
      float32x4_t v_lD = {l_g, l_h, l_k, l_l};
      float32x4_t v_lE = {l_j, l_k, l_n, l_o};

      float32x4_t dc = vsubq_f32(v_lD, v_lC);
      float32x4_t cb = vsubq_f32(v_lC, v_lB);
      float32x4_t lenX = vmaxq_f32(vabsq_f32(dc), vabsq_f32(cb));
      lenX = APrxLoRcpF1Neon(lenX);
      float32x4_t dx = vsubq_f32(v_lD, v_lB);
      lenX = vmulq_f32(vabsq_f32(dx), lenX);
      lenX = vminq_f32(lenX, vdupq_n_f32(1.0f));
      lenX = vmaxq_f32(lenX, vdupq_n_f32(0.0f));
      lenX = vmulq_f32(lenX, lenX);

      float32x4_t ec = vsubq_f32(v_lE, v_lC);
      float32x4_t ca = vsubq_f32(v_lC, v_lA);
      float32x4_t lenY = vmaxq_f32(vabsq_f32(ec), vabsq_f32(ca));
      lenY = APrxLoRcpF1Neon(lenY);
      float32x4_t dy = vsubq_f32(v_lE, v_lA);
      lenY = vmulq_f32(vabsq_f32(dy), lenY);
      lenY = vminq_f32(lenY, vdupq_n_f32(1.0f));
      lenY = vmaxq_f32(lenY, vdupq_n_f32(0.0f));
      lenY = vmulq_f32(lenY, lenY);

      float32x4_t dx_w = vmulq_f32(dx, w);
      float32x4_t dy_w = vmulq_f32(dy, w);
      float32x4_t len_w = vmlaq_f32(vmulq_f32(w, lenX), w, lenY);

      float dirX = vaddvq_f32(dx_w);
      float dirY = vaddvq_f32(dy_w);
      float len = vaddvq_f32(len_w);

      float dirR = dirX * dirX + dirY * dirY;
      bool zro = dirR < (1.0f / 32768.0f);
      float dirRsq = APrxLoRsqF1(dirR);
      if (zro) {
        dirRsq = 1.0f;
        dirX = 1.0f;
      }
      dirX *= dirRsq;
      dirY *= dirRsq;

      float maxDir = std::max(std::abs(dirX), std::abs(dirY));
      float stretch = (dirX * dirX + dirY * dirY) * APrxLoRcpF1(maxDir);
      float len_val = len * 0.5f;
      len_val *= len_val;

      float len2X = 1.0f + (stretch - 1.0f) * len_val;
      float len2Y = 1.0f - 0.5f * len_val;
      float lob = 0.5f + (0.21f - 0.5f) * len_val;
      float clp = APrxLoRcpF1(lob);

      uint32_t px0[4] = {p_b, p_c, p_i, p_j};
      uint32_t px1[4] = {p_f, p_e, p_k, p_l};
      uint32_t px2[4] = {p_h, p_g, p_o, p_n};

      uint8x8x4_t v0 = vld4_u8(reinterpret_cast<const uint8_t*>(px0));
      uint8x8x4_t v1 = vld4_u8(reinterpret_cast<const uint8_t*>(px1));
      uint8x8x4_t v2 = vld4_u8(reinterpret_cast<const uint8_t*>(px2));

      float32x4_t cR0, cG0, cB0, cA0;
      float32x4_t cR1, cG1, cB1, cA1;
      float32x4_t cR2, cG2, cB2, cA2;

      unpack_u8x4_to_float(v0.val[0], cR0);
      unpack_u8x4_to_float(v0.val[1], cG0);
      unpack_u8x4_to_float(v0.val[2], cB0);
      unpack_u8x4_to_float(v0.val[3], cA0);

      unpack_u8x4_to_float(v1.val[0], cR1);
      unpack_u8x4_to_float(v1.val[1], cG1);
      unpack_u8x4_to_float(v1.val[2], cB1);
      unpack_u8x4_to_float(v1.val[3], cA1);

      unpack_u8x4_to_float(v2.val[0], cR2);
      unpack_u8x4_to_float(v2.val[1], cG2);
      unpack_u8x4_to_float(v2.val[2], cB2);
      unpack_u8x4_to_float(v2.val[3], cA2);

      float32x4_t offX0 = { 0.0f - ppX,  1.0f - ppX, -1.0f - ppX,  0.0f - ppX};
      float32x4_t offY0 = {-1.0f - ppY, -1.0f - ppY,  1.0f - ppY,  1.0f - ppY};

      float32x4_t offX1 = { 0.0f - ppX, -1.0f - ppX,  1.0f - ppX,  2.0f - ppX};
      float32x4_t offY1 = { 0.0f - ppY,  0.0f - ppY,  1.0f - ppY,  1.0f - ppY};

      float32x4_t offX2 = { 2.0f - ppX,  1.0f - ppX,  1.0f - ppX,  0.0f - ppX};
      float32x4_t offY2 = { 0.0f - ppY,  0.0f - ppY,  2.0f - ppY,  2.0f - ppY};

      float32x4_t v_dirX = vdupq_n_f32(dirX);
      float32x4_t v_dirY = vdupq_n_f32(dirY);
      float32x4_t v_len2X = vdupq_n_f32(len2X);
      float32x4_t v_len2Y = vdupq_n_f32(len2Y);
      float32x4_t v_lob = vdupq_n_f32(lob);
      float32x4_t v_clp = vdupq_n_f32(clp);

      float32x4_t aR = vdupq_n_f32(0.0f);
      float32x4_t aG = vdupq_n_f32(0.0f);
      float32x4_t aB = vdupq_n_f32(0.0f);
      float32x4_t aA = vdupq_n_f32(0.0f);
      float32x4_t aW = vdupq_n_f32(0.0f);

      FsrEasuTap4(aR, aG, aB, aA, aW, offX0, offY0, v_dirX, v_dirY, v_len2X, v_len2Y, v_lob, v_clp, cR0, cG0, cB0, cA0);
      FsrEasuTap4(aR, aG, aB, aA, aW, offX1, offY1, v_dirX, v_dirY, v_len2X, v_len2Y, v_lob, v_clp, cR1, cG1, cB1, cA1);
      FsrEasuTap4(aR, aG, aB, aA, aW, offX2, offY2, v_dirX, v_dirY, v_len2X, v_len2Y, v_lob, v_clp, cR2, cG2, cB2, cA2);

      float sumR = vaddvq_f32(aR);
      float sumG = vaddvq_f32(aG);
      float sumB = vaddvq_f32(aB);
      float sumA = vaddvq_f32(aA);
      float sumW = vaddvq_f32(aW);

      float fL_r = vgetq_lane_f32(cR1, 0);
      float fL_g = vgetq_lane_f32(cG1, 0);
      float fL_b = vgetq_lane_f32(cB1, 0);
      float fL_a = vgetq_lane_f32(cA1, 0);

      float rResult = sumW > 0.0f ? (sumR / sumW) : fL_r;
      float gResult = sumW > 0.0f ? (sumG / sumW) : fL_g;
      float bResult = sumW > 0.0f ? (sumB / sumW) : fL_b;
      float aResult = sumW > 0.0f ? (sumA / sumW) : fL_a;

      float gL_r = vgetq_lane_f32(cR2, 1);
      float gL_g = vgetq_lane_f32(cG2, 1);
      float gL_b = vgetq_lane_f32(cB2, 1);
      float gL_a = vgetq_lane_f32(cA2, 1);

      float jL_r = vgetq_lane_f32(cR0, 3);
      float jL_g = vgetq_lane_f32(cG0, 3);
      float jL_b = vgetq_lane_f32(cB0, 3);
      float jL_a = vgetq_lane_f32(cA0, 3);

      float kL_r = vgetq_lane_f32(cR1, 2);
      float kL_g = vgetq_lane_f32(cG1, 2);
      float kL_b = vgetq_lane_f32(cB1, 2);
      float kL_a = vgetq_lane_f32(cA1, 2);

      float minR = std::min(std::min(fL_r, gL_r), std::min(jL_r, kL_r));
      float maxR = std::max(std::max(fL_r, gL_r), std::max(jL_r, kL_r));
      rResult = clamp(rResult, minR, maxR);

      float minG = std::min(std::min(fL_g, gL_g), std::min(jL_g, kL_g));
      float maxG = std::max(std::max(fL_g, gL_g), std::max(jL_g, kL_g));
      gResult = clamp(gResult, minG, maxG);

      float minB = std::min(std::min(fL_b, gL_b), std::min(jL_b, kL_b));
      float maxB = std::max(std::max(fL_b, gL_b), std::max(jL_b, kL_b));
      bResult = clamp(bResult, minB, maxB);

      float minA = std::min(std::min(fL_a, gL_a), std::min(jL_a, kL_a));
      float maxA = std::max(std::max(fL_a, gL_a), std::max(jL_a, kL_a));
      aResult = clamp(aResult, minA, maxA);

      dst[y * dw + x] =
          ((uint32_t)clamp(aResult * 255.0f, 0.0f, 255.0f) << 24) |
          ((uint32_t)clamp(bResult * 255.0f, 0.0f, 255.0f) << 16) |
          ((uint32_t)clamp(gResult * 255.0f, 0.0f, 255.0f) << 8) |
          (uint32_t)clamp(rResult * 255.0f, 0.0f, 255.0f);
    }
  }
}

inline float computeRcasLobeWithFactor(float e, float b, float d, float f, float h,
                                       float sharpness_factor) {
  float mn = std::min(std::min(b, d), std::min(f, h));
  float mx = std::max(std::max(b, d), std::max(f, h));
  float mnL = std::min(mn, e);
  float mxL = std::max(mx, e);
  float hitMin = mnL / (4.0f * mxL + 1e-5f);
  float hitMax = (1.0f - mxL) / (4.0f * mnL - 4.0f - 1e-5f);
  float lobeL = std::max(-hitMin, hitMax);
  float limit = 0.1875f;
  float lobe =
      std::max(-limit, std::min(lobeL, 0.0f)) * sharpness_factor;

  float nz = 0.25f * b + 0.25f * d + 0.25f * f + 0.25f * h - e;
  float range = std::max(std::max(std::max(b, d), e), std::max(f, h)) -
                std::min(std::min(std::min(b, d), e), std::min(f, h));
  if (range > 1e-5f) {
    float nz_val = clamp(std::abs(nz) / range, 0.0f, 1.0f);
    float nz_factor = -0.5f * nz_val + 1.0f;
    lobe *= nz_factor;
  }
  return lobe;
}

inline float32x4_t computeRcasLobeNeon(float32x4_t e, float32x4_t b, float32x4_t d, float32x4_t f, float32x4_t h, float32x4_t v_sharpness_factor) {
  float32x4_t mn = vminq_f32(vminq_f32(b, d), vminq_f32(f, h));
  float32x4_t mx = vmaxq_f32(vmaxq_f32(b, d), vmaxq_f32(f, h));
  float32x4_t mnL = vminq_f32(mn, e);
  float32x4_t mxL = vmaxq_f32(mx, e);

  float32x4_t v_four = vdupq_n_f32(4.0f);
  float32x4_t v_1e5 = vdupq_n_f32(1e-5f);
  float32x4_t denomMin = vmlaq_f32(v_1e5, mxL, v_four);
  float32x4_t hitMin = vdivq_f32(mnL, denomMin);

  float32x4_t v_one = vdupq_n_f32(1.0f);
  float32x4_t numMax = vsubq_f32(v_one, mxL);
  float32x4_t denomMax = vmlaq_f32(vdupq_n_f32(-4.0f - 1e-5f), mnL, v_four);
  float32x4_t hitMax = vdivq_f32(numMax, denomMax);

  float32x4_t negHitMin = vnegq_f32(hitMin);
  float32x4_t lobeL = vmaxq_f32(negHitMin, hitMax);

  float32x4_t v_zero = vdupq_n_f32(0.0f);
  float32x4_t v_neg_limit = vdupq_n_f32(-0.1875f);
  float32x4_t clampedLobeL = vminq_f32(lobeL, v_zero);
  clampedLobeL = vmaxq_f32(clampedLobeL, v_neg_limit);
  float32x4_t lobe = vmulq_f32(clampedLobeL, v_sharpness_factor);

  float32x4_t sum_bdfh = vaddq_f32(vaddq_f32(b, d), vaddq_f32(f, h));
  float32x4_t nz = vsubq_f32(vmulq_f32(sum_bdfh, vdupq_n_f32(0.25f)), e);

  float32x4_t range = vsubq_f32(mxL, mnL);

  float32x4_t abs_nz = vabsq_f32(nz);
  float32x4_t nz_val = vdivq_f32(abs_nz, range);
  nz_val = vminq_f32(nz_val, v_one);
  nz_val = vmaxq_f32(nz_val, v_zero);

  float32x4_t nz_factor = vmlaq_f32(v_one, nz_val, vdupq_n_f32(-0.5f));

  uint32x4_t mask = vcgtq_f32(range, v_1e5);
  float32x4_t multiplier = vbslq_f32(mask, nz_factor, v_one);
  lobe = vmulq_f32(lobe, multiplier);

  return lobe;
}

inline void unpack_u8_to_float(uint8x16_t u8_vec, float32x4_t &f0, float32x4_t &f1, float32x4_t &f2, float32x4_t &f3) {
  uint16x8_t u16_low = vmovl_u8(vget_low_u8(u8_vec));
  uint16x8_t u16_high = vmovl_u8(vget_high_u8(u8_vec));

  uint32x4_t u32_0 = vmovl_u16(vget_low_u16(u16_low));
  uint32x4_t u32_1 = vmovl_u16(vget_high_u16(u16_low));
  uint32x4_t u32_2 = vmovl_u16(vget_low_u16(u16_high));
  uint32x4_t u32_3 = vmovl_u16(vget_high_u16(u16_high));

  float32x4_t v_scale = vdupq_n_f32(1.0f / 255.0f);
  f0 = vmulq_f32(vcvtq_f32_u32(u32_0), v_scale);
  f1 = vmulq_f32(vcvtq_f32_u32(u32_1), v_scale);
  f2 = vmulq_f32(vcvtq_f32_u32(u32_2), v_scale);
  f3 = vmulq_f32(vcvtq_f32_u32(u32_3), v_scale);
}

inline uint8x16_t pack_float_to_u8(float32x4_t f0, float32x4_t f1, float32x4_t f2, float32x4_t f3) {
  float32x4_t v_zero = vdupq_n_f32(0.0f);
  float32x4_t v_255 = vdupq_n_f32(255.0f);

  f0 = vmaxq_f32(vminq_f32(f0, v_255), v_zero);
  f1 = vmaxq_f32(vminq_f32(f1, v_255), v_zero);
  f2 = vmaxq_f32(vminq_f32(f2, v_255), v_zero);
  f3 = vmaxq_f32(vminq_f32(f3, v_255), v_zero);

  int32x4_t i0 = vcvtq_s32_f32(f0);
  int32x4_t i1 = vcvtq_s32_f32(f1);
  int32x4_t i2 = vcvtq_s32_f32(f2);
  int32x4_t i3 = vcvtq_s32_f32(f3);

  uint16x4_t u16_0 = vqmovun_s32(i0);
  uint16x4_t u16_1 = vqmovun_s32(i1);
  uint16x4_t u16_2 = vqmovun_s32(i2);
  uint16x4_t u16_3 = vqmovun_s32(i3);

  uint16x8_t u16_low = vcombine_u16(u16_0, u16_1);
  uint16x8_t u16_high = vcombine_u16(u16_2, u16_3);

  uint8x8_t u8_low = vqmovn_u16(u16_low);
  uint8x8_t u8_high = vqmovn_u16(u16_high);

  return vcombine_u8(u8_low, u8_high);
}

inline void process_rcas_channel_4(
    float32x4_t e, float32x4_t b, float32x4_t d, float32x4_t f, float32x4_t h,
    float32x4_t v_sharpness_factor, float32x4_t &out) {

  float32x4_t lobe = computeRcasLobeNeon(e, b, d, f, h, v_sharpness_factor);

  float32x4_t sum_bdfh = vaddq_f32(vaddq_f32(b, d), vaddq_f32(f, h));
  float32x4_t num = vmlaq_f32(e, sum_bdfh, lobe);

  float32x4_t denom = vmlaq_f32(vdupq_n_f32(1.0f), lobe, vdupq_n_f32(4.0f));

  out = vdivq_f32(num, denom);
}

inline uint8x16_t process_rcas_channel_16(
    uint8x16_t vE, uint8x16_t vB, uint8x16_t vD, uint8x16_t vF, uint8x16_t vH,
    float32x4_t v_sharpness_factor) {

  float32x4_t e0, e1, e2, e3;
  float32x4_t b0, b1, b2, b3;
  float32x4_t d0, d1, d2, d3;
  float32x4_t f0, f1, f2, f3;
  float32x4_t h0, h1, h2, h3;

  unpack_u8_to_float(vE, e0, e1, e2, e3);
  unpack_u8_to_float(vB, b0, b1, b2, b3);
  unpack_u8_to_float(vD, d0, d1, d2, d3);
  unpack_u8_to_float(vF, f0, f1, f2, f3);
  unpack_u8_to_float(vH, h0, h1, h2, h3);

  float32x4_t out0, out1, out2, out3;
  process_rcas_channel_4(e0, b0, d0, f0, h0, v_sharpness_factor, out0);
  process_rcas_channel_4(e1, b1, d1, f1, h1, v_sharpness_factor, out1);
  process_rcas_channel_4(e2, b2, d2, f2, h2, v_sharpness_factor, out2);
  process_rcas_channel_4(e3, b3, d3, f3, h3, v_sharpness_factor, out3);

  float32x4_t v_255 = vdupq_n_f32(255.0f);
  out0 = vmulq_f32(out0, v_255);
  out1 = vmulq_f32(out1, v_255);
  out2 = vmulq_f32(out2, v_255);
  out3 = vmulq_f32(out3, v_255);

  return pack_float_to_u8(out0, out1, out2, out3);
}

void rcas(uint32_t *pixels, int w, int h, float sharpness) {
  if (h < 3 || w < 3) return;

  static thread_local std::vector<uint32_t> prev_row;
  static thread_local std::vector<uint32_t> curr_row;
  prev_row.resize(w);
  curr_row.resize(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  float sharpness_factor = std::pow(2.0f, -sharpness);
  float32x4_t v_sharpness_factor = vdupq_n_f32(sharpness_factor);

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    int x = 1;
    for (; x + 16 <= w - 1; x += 16) {
      uint8x16x4_t vE = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x]));
      uint8x16x4_t vB = vld4q_u8(reinterpret_cast<const uint8_t*>(&prev_row[x]));
      uint8x16x4_t vD = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x - 1]));
      uint8x16x4_t vF = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x + 1]));
      uint8x16x4_t vH = vld4q_u8(reinterpret_cast<const uint8_t*>(&pixels[(y + 1) * w + x]));

      uint8x16x4_t vOut;
      vOut.val[0] = process_rcas_channel_16(vE.val[0], vB.val[0], vD.val[0], vF.val[0], vH.val[0], v_sharpness_factor);
      vOut.val[1] = process_rcas_channel_16(vE.val[1], vB.val[1], vD.val[1], vF.val[1], vH.val[1], v_sharpness_factor);
      vOut.val[2] = process_rcas_channel_16(vE.val[2], vB.val[2], vD.val[2], vF.val[2], vH.val[2], v_sharpness_factor);
      vOut.val[3] = vE.val[3];

      vst4q_u8(reinterpret_cast<uint8_t*>(&pixels[y * w + x]), vOut);
    }

    for (; x < w - 1; x++) {
      uint32_t cE = curr_row[x];
      uint32_t cB = prev_row[x];
      uint32_t cD = curr_row[x - 1];
      uint32_t cF = curr_row[x + 1];
      uint32_t cH = pixels[(y + 1) * w + x];

      float eR = (cE & 0xFF) / 255.0f;
      float eG = ((cE >> 8) & 0xFF) / 255.0f;
      float eB = ((cE >> 16) & 0xFF) / 255.0f;
      float eA = ((cE >> 24) & 0xFF) / 255.0f;

      float bR = (cB & 0xFF) / 255.0f;
      float bG = ((cB >> 8) & 0xFF) / 255.0f;
      float bB = ((cB >> 16) & 0xFF) / 255.0f;

      float dR = (cD & 0xFF) / 255.0f;
      float dG = ((cD >> 8) & 0xFF) / 255.0f;
      float dB = ((cD >> 16) & 0xFF) / 255.0f;

      float fR = (cF & 0xFF) / 255.0f;
      float fG = ((cF >> 8) & 0xFF) / 255.0f;
      float fB = ((cF >> 16) & 0xFF) / 255.0f;

      float hR = (cH & 0xFF) / 255.0f;
      float hG = ((cH >> 8) & 0xFF) / 255.0f;
      float hB = ((cH >> 16) & 0xFF) / 255.0f;

      float lobeR = computeRcasLobeWithFactor(eR, bR, dR, fR, hR, sharpness_factor);
      float rR = (lobeR * (bR + dR + fR + hR) + eR) / (4.0f * lobeR + 1.0f);

      float lobeG = computeRcasLobeWithFactor(eG, bG, dG, fG, hG, sharpness_factor);
      float rG = (lobeG * (bG + dG + fG + hG) + eG) / (4.0f * lobeG + 1.0f);

      float lobeB = computeRcasLobeWithFactor(eB, bB, dB, fB, hB, sharpness_factor);
      float rB = (lobeB * (bB + dB + fB + hB) + eB) / (4.0f * lobeB + 1.0f);

      pixels[y * w + x] = ((uint32_t)clamp(eA * 255.0f, 0.0f, 255.0f) << 24) |
                          ((uint32_t)clamp(rB * 255.0f, 0.0f, 255.0f) << 16) |
                          ((uint32_t)clamp(rG * 255.0f, 0.0f, 255.0f) << 8) |
                          (uint32_t)clamp(rR * 255.0f, 0.0f, 255.0f);
    }

    std::memcpy(prev_row.data(), curr_row.data(), w * sizeof(uint32_t));
  }
}

inline uint8x8_t process_sharpen_8(
    uint8x8_t vE_l, uint8x8_t vB_l, uint8x8_t vD_l, uint8x8_t vF_l, uint8x8_t vH_l,
    float32x4_t v_strength, float32x4_t v_zero, float32x4_t v_255) {

  uint16x8_t sum = vmovl_u8(vB_l);
  sum = vaddw_u8(sum, vD_l);
  sum = vaddw_u8(sum, vF_l);
  sum = vaddw_u8(sum, vH_l);

  uint16x8_t E4 = vshll_n_u8(vE_l, 2);
  int16x8_t diff = vsubq_s16(vreinterpretq_s16_u16(E4), vreinterpretq_s16_u16(sum));

  int32x4_t diff_l = vmovl_s16(vget_low_s16(diff));
  int32x4_t diff_h = vmovl_s16(vget_high_s16(diff));

  uint16x8_t E_16 = vmovl_u8(vE_l);
  int32x4_t E_l = vreinterpretq_s32_u32(vmovl_u16(vget_low_u16(E_16)));
  int32x4_t E_h = vreinterpretq_s32_u32(vmovl_u16(vget_high_u16(E_16)));

  float32x4_t f_diff_l = vcvtq_f32_s32(diff_l);
  float32x4_t f_diff_h = vcvtq_f32_s32(diff_h);
  float32x4_t f_E_l = vcvtq_f32_s32(E_l);
  float32x4_t f_E_h = vcvtq_f32_s32(E_h);

  float32x4_t res_l = vmlaq_f32(f_E_l, f_diff_l, v_strength);
  float32x4_t res_h = vmlaq_f32(f_E_h, f_diff_h, v_strength);

  res_l = vmaxq_f32(res_l, v_zero);
  res_l = vminq_f32(res_l, v_255);
  res_h = vmaxq_f32(res_h, v_zero);
  res_h = vminq_f32(res_h, v_255);

  int32x4_t res_l_i32 = vcvtq_s32_f32(res_l);
  int32x4_t res_h_i32 = vcvtq_s32_f32(res_h);

  uint16x4_t pack_l = vqmovun_s32(res_l_i32);
  uint16x4_t pack_h = vqmovun_s32(res_h_i32);

  return vqmovn_u16(vcombine_u16(pack_l, pack_h));
}

inline uint8x16_t process_sharpen_16(
    uint8x16_t vE, uint8x16_t vB, uint8x16_t vD, uint8x16_t vF, uint8x16_t vH,
    float32x4_t v_strength, float32x4_t v_zero, float32x4_t v_255) {

  uint8x8_t res_low = process_sharpen_8(vget_low_u8(vE), vget_low_u8(vB), vget_low_u8(vD), vget_low_u8(vF), vget_low_u8(vH), v_strength, v_zero, v_255);
  uint8x8_t res_high = process_sharpen_8(vget_high_u8(vE), vget_high_u8(vB), vget_high_u8(vD), vget_high_u8(vF), vget_high_u8(vH), v_strength, v_zero, v_255);
  return vcombine_u8(res_low, res_high);
}

void sharpen(uint32_t *pixels, int w, int h, float strength) {
  if (h < 3 || w < 3) return;

  static thread_local std::vector<uint32_t> prev_row;
  static thread_local std::vector<uint32_t> curr_row;
  prev_row.resize(w);
  curr_row.resize(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  float32x4_t v_strength = vdupq_n_f32(strength);
  float32x4_t v_zero = vdupq_n_f32(0.0f);
  float32x4_t v_255 = vdupq_n_f32(255.0f);

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    int x = 1;
    for (; x + 16 <= w - 1; x += 16) {
      uint8x16x4_t vE = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x]));
      uint8x16x4_t vB = vld4q_u8(reinterpret_cast<const uint8_t*>(&prev_row[x]));
      uint8x16x4_t vD = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x - 1]));
      uint8x16x4_t vF = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x + 1]));
      uint8x16x4_t vH = vld4q_u8(reinterpret_cast<const uint8_t*>(&pixels[(y + 1) * w + x]));

      uint8x16x4_t vOut;
      vOut.val[0] = process_sharpen_16(vE.val[0], vB.val[0], vD.val[0], vF.val[0], vH.val[0], v_strength, v_zero, v_255);
      vOut.val[1] = process_sharpen_16(vE.val[1], vB.val[1], vD.val[1], vF.val[1], vH.val[1], v_strength, v_zero, v_255);
      vOut.val[2] = process_sharpen_16(vE.val[2], vB.val[2], vD.val[2], vF.val[2], vH.val[2], v_strength, v_zero, v_255);
      vOut.val[3] = vE.val[3];

      vst4q_u8(reinterpret_cast<uint8_t*>(&pixels[y * w + x]), vOut);
    }

    for (; x < w - 1; x++) {
      uint32_t cE = curr_row[x];
      uint32_t cB = prev_row[x];
      uint32_t cD = curr_row[x - 1];
      uint32_t cF = curr_row[x + 1];
      uint32_t cH = pixels[(y + 1) * w + x];

      auto getR = [&](uint32_t p) { return p & 0xFF; };
      auto getG = [&](uint32_t p) { return (p >> 8) & 0xFF; };
      auto getB = [&](uint32_t p) { return (p >> 16) & 0xFF; };

      float r_orig = getR(cE);
      float g_orig = getG(cE);
      float b_orig = getB(cE);

      float r_sharp = 5 * r_orig - getR(cD) - getR(cF) - getR(cB) - getR(cH);
      float g_sharp = 5 * g_orig - getG(cD) - getG(cF) - getG(cB) - getG(cH);
      float b_sharp = 5 * b_orig - getB(cD) - getB(cF) - getB(cB) - getB(cH);

      float r = r_orig + strength * (r_sharp - r_orig);
      float g = g_orig + strength * (g_sharp - g_orig);
      float b = b_orig + strength * (b_sharp - b_orig);

      uint32_t a = (cE >> 24) & 0xFF;
      pixels[y * w + x] = (a << 24) | ((uint32_t)clamp(b, 0.0f, 255.0f) << 16) |
                          ((uint32_t)clamp(g, 0.0f, 255.0f) << 8) |
                          (uint32_t)clamp(r, 0.0f, 255.0f);
    }

    std::memcpy(prev_row.data(), curr_row.data(), w * sizeof(uint32_t));
  }
}

inline uint8x8_t process_denoise_8(
    uint8x8_t vB_l, uint8x8_t vB_m, uint8x8_t vB_r,
    uint8x8_t vE_l, uint8x8_t vE_m, uint8x8_t vE_r,
    uint8x8_t vH_l, uint8x8_t vH_m, uint8x8_t vH_r,
    float32x4_t v_strength, float32x4_t v_nine, float32x4_t v_zero, float32x4_t v_255) {

  uint16x8_t sum = vmovl_u8(vB_l);
  sum = vaddw_u8(sum, vB_m);
  sum = vaddw_u8(sum, vB_r);
  sum = vaddw_u8(sum, vE_l);
  sum = vaddw_u8(sum, vE_m);
  sum = vaddw_u8(sum, vE_r);
  sum = vaddw_u8(sum, vH_l);
  sum = vaddw_u8(sum, vH_m);
  sum = vaddw_u8(sum, vH_r);

  float32x4_t f_sum_l = vcvtq_f32_u32(vmovl_u16(vget_low_u16(sum)));
  float32x4_t f_sum_h = vcvtq_f32_u32(vmovl_u16(vget_high_u16(sum)));

  float32x4_t blur_l = vdivq_f32(f_sum_l, v_nine);
  float32x4_t blur_h = vdivq_f32(f_sum_h, v_nine);

  uint16x8_t E_16 = vmovl_u8(vE_m);
  int32x4_t E_l = vreinterpretq_s32_u32(vmovl_u16(vget_low_u16(E_16)));
  int32x4_t E_h = vreinterpretq_s32_u32(vmovl_u16(vget_high_u16(E_16)));
  float32x4_t f_E_l = vcvtq_f32_s32(E_l);
  float32x4_t f_E_h = vcvtq_f32_s32(E_h);

  float32x4_t diff_l = vsubq_f32(blur_l, f_E_l);
  float32x4_t diff_h = vsubq_f32(blur_h, f_E_h);

  float32x4_t res_l = vmlaq_f32(f_E_l, diff_l, v_strength);
  float32x4_t res_h = vmlaq_f32(f_E_h, diff_h, v_strength);

  res_l = vmaxq_f32(res_l, v_zero);
  res_l = vminq_f32(res_l, v_255);
  res_h = vmaxq_f32(res_h, v_zero);
  res_h = vminq_f32(res_h, v_255);

  int32x4_t res_l_i32 = vcvtq_s32_f32(res_l);
  int32x4_t res_h_i32 = vcvtq_s32_f32(res_h);

  uint16x4_t pack_l = vqmovun_s32(res_l_i32);
  uint16x4_t pack_h = vqmovun_s32(res_h_i32);

  return vqmovn_u16(vcombine_u16(pack_l, pack_h));
}

inline uint8x16_t process_denoise_16(
    uint8x16_t vB_l, uint8x16_t vB_m, uint8x16_t vB_r,
    uint8x16_t vE_l, uint8x16_t vE_m, uint8x16_t vE_r,
    uint8x16_t vH_l, uint8x16_t vH_m, uint8x16_t vH_r,
    float32x4_t v_strength, float32x4_t v_nine, float32x4_t v_zero, float32x4_t v_255) {

  uint8x8_t res_low = process_denoise_8(
      vget_low_u8(vB_l), vget_low_u8(vB_m), vget_low_u8(vB_r),
      vget_low_u8(vE_l), vget_low_u8(vE_m), vget_low_u8(vE_r),
      vget_low_u8(vH_l), vget_low_u8(vH_m), vget_low_u8(vH_r),
      v_strength, v_nine, v_zero, v_255
  );
  uint8x8_t res_high = process_denoise_8(
      vget_high_u8(vB_l), vget_high_u8(vB_m), vget_high_u8(vB_r),
      vget_high_u8(vE_l), vget_high_u8(vE_m), vget_high_u8(vE_r),
      vget_high_u8(vH_l), vget_high_u8(vH_m), vget_high_u8(vH_r),
      v_strength, v_nine, v_zero, v_255
  );
  return vcombine_u8(res_low, res_high);
}

void denoise(uint32_t *pixels, int w, int h, float strength) {
  if (h < 3 || w < 3) return;

  static thread_local std::vector<uint32_t> prev_row;
  static thread_local std::vector<uint32_t> curr_row;
  prev_row.resize(w);
  curr_row.resize(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  float32x4_t v_strength = vdupq_n_f32(strength);
  float32x4_t v_nine = vdupq_n_f32(9.0f);
  float32x4_t v_zero = vdupq_n_f32(0.0f);
  float32x4_t v_255 = vdupq_n_f32(255.0f);

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    int x = 1;
    for (; x + 16 <= w - 1; x += 16) {
      uint8x16x4_t vB = vld4q_u8(reinterpret_cast<const uint8_t*>(&prev_row[x]));
      uint8x16x4_t vB_l = vld4q_u8(reinterpret_cast<const uint8_t*>(&prev_row[x - 1]));
      uint8x16x4_t vB_r = vld4q_u8(reinterpret_cast<const uint8_t*>(&prev_row[x + 1]));

      uint8x16x4_t vE = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x]));
      uint8x16x4_t vE_l = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x - 1]));
      uint8x16x4_t vE_r = vld4q_u8(reinterpret_cast<const uint8_t*>(&curr_row[x + 1]));

      uint8x16x4_t vH = vld4q_u8(reinterpret_cast<const uint8_t*>(&pixels[(y + 1) * w + x]));
      uint8x16x4_t vH_l = vld4q_u8(reinterpret_cast<const uint8_t*>(&pixels[(y + 1) * w + x - 1]));
      uint8x16x4_t vH_r = vld4q_u8(reinterpret_cast<const uint8_t*>(&pixels[(y + 1) * w + x + 1]));

      uint8x16x4_t vOut;
      vOut.val[0] = process_denoise_16(vB_l.val[0], vB.val[0], vB_r.val[0], vE_l.val[0], vE.val[0], vE_r.val[0], vH_l.val[0], vH.val[0], vH_r.val[0], v_strength, v_nine, v_zero, v_255);
      vOut.val[1] = process_denoise_16(vB_l.val[1], vB.val[1], vB_r.val[1], vE_l.val[1], vE.val[1], vE_r.val[1], vH_l.val[1], vH.val[1], vH_r.val[1], v_strength, v_nine, v_zero, v_255);
      vOut.val[2] = process_denoise_16(vB_l.val[2], vB.val[2], vB_r.val[2], vE_l.val[2], vE.val[2], vE_r.val[2], vH_l.val[2], vH.val[2], vH_r.val[2], v_strength, v_nine, v_zero, v_255);
      vOut.val[3] = process_denoise_16(vB_l.val[3], vB.val[3], vB_r.val[3], vE_l.val[3], vE.val[3], vE_r.val[3], vH_l.val[3], vH.val[3], vH_r.val[3], v_strength, v_nine, v_zero, v_255);

      vst4q_u8(reinterpret_cast<uint8_t*>(&pixels[y * w + x]), vOut);
    }

    for (; x < w - 1; x++) {
      int r_blur = 0, g_blur = 0, b_blur = 0, a_blur = 0;

      for (int kx = -1; kx <= 1; kx++) {
        uint32_t p = prev_row[x + kx];
        r_blur += p & 0xFF;
        g_blur += (p >> 8) & 0xFF;
        b_blur += (p >> 16) & 0xFF;
        a_blur += (p >> 24) & 0xFF;
      }

      for (int kx = -1; kx <= 1; kx++) {
        uint32_t p = curr_row[x + kx];
        r_blur += p & 0xFF;
        g_blur += (p >> 8) & 0xFF;
        b_blur += (p >> 16) & 0xFF;
        a_blur += (p >> 24) & 0xFF;
      }

      for (int kx = -1; kx <= 1; kx++) {
        uint32_t p = pixels[(y + 1) * w + (x + kx)];
        r_blur += p & 0xFF;
        g_blur += (p >> 8) & 0xFF;
        b_blur += (p >> 16) & 0xFF;
        a_blur += (p >> 24) & 0xFF;
      }

      uint32_t p_orig = curr_row[x];
      float r_orig = p_orig & 0xFF;
      float g_orig = (p_orig >> 8) & 0xFF;
      float b_orig = (p_orig >> 16) & 0xFF;
      float a_orig = (p_orig >> 24) & 0xFF;

      float r = r_orig + strength * ((r_blur / 9.0f) - r_orig);
      float g = g_orig + strength * ((g_blur / 9.0f) - g_orig);
      float b = b_orig + strength * ((b_blur / 9.0f) - b_orig);
      float a = a_orig + strength * ((a_blur / 9.0f) - a_orig);

      pixels[y * w + x] = ((uint32_t)clamp(a, 0.0f, 255.0f) << 24) |
                          ((uint32_t)clamp(b, 0.0f, 255.0f) << 16) |
                          ((uint32_t)clamp(g, 0.0f, 255.0f) << 8) |
                          (uint32_t)clamp(r, 0.0f, 255.0f);
    }

    std::memcpy(prev_row.data(), curr_row.data(), w * sizeof(uint32_t));
  }
}

void avir_resize(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh) {
  avir::CImageResizer<> ImageResizer(8);
  ImageResizer.resizeImage((uint8_t*)src, sw, sh, 0, (uint8_t*)dst, dw, dh, 4, 0);
}

void lancir_resize(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh) {
  avir::CLancIR ImageResizer;
  ImageResizer.resizeImage((uint8_t*)src, sw, sh, (uint8_t*)dst, dw, dh, 4);
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
    jint filters, jfloat sharpeningStrength, jfloat denoisingStrength) {
  if (bitmap == nullptr || jData == nullptr || !gDecoder.available)
    return JNI_FALSE;

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
      info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
    return JNI_FALSE;
  }

  if (info.format == ANDROID_BITMAP_FORMAT_RGB_565 && filters != 0) {
    return JNI_FALSE;
  }

  void *pixels;
  if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != 0) {
    return JNI_FALSE;
  }

  jbyte *data = (jbyte *)env->GetPrimitiveArrayCritical(jData, nullptr);
  if (!data) {
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_FALSE;
  }

  AImageDecoder *decoder = nullptr;
  int ret = gDecoder.createFromBuffer(data, length, &decoder);
  if (ret != 0) {
    env->ReleasePrimitiveArrayCritical(jData, data, JNI_ABORT);
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_FALSE;
  }

  if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
    gDecoder.setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGB_565);
  } else {
    gDecoder.setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);
  }

  if (filters & 4) {
    const AImageDecoderHeaderInfo *header = gDecoder.getHeaderInfo(decoder);
    int sw = gDecoder.getWidth(header);
    int sh = gDecoder.getHeight(header);
    std::vector<uint32_t> temp(sw * sh);
    gDecoder.decodeImage(decoder, temp.data(), sw * 4, sw * sh * 4);
    hikari::easu(temp.data(), (uint32_t *)pixels, sw, sh, info.width,
                 info.height);
  } else if (filters & 8) {
    const AImageDecoderHeaderInfo *header = gDecoder.getHeaderInfo(decoder);
    int sw = gDecoder.getWidth(header);
    int sh = gDecoder.getHeight(header);
    std::vector<uint32_t> temp(sw * sh);
    gDecoder.decodeImage(decoder, temp.data(), sw * 4, sw * sh * 4);
    hikari::avir_resize(temp.data(), (uint32_t *)pixels, sw, sh, info.width,
                        info.height);
  } else if (filters & 16) {
    const AImageDecoderHeaderInfo *header = gDecoder.getHeaderInfo(decoder);
    int sw = gDecoder.getWidth(header);
    int sh = gDecoder.getHeight(header);
    std::vector<uint32_t> temp(sw * sh);
    gDecoder.decodeImage(decoder, temp.data(), sw * 4, sw * sh * 4);
    hikari::lancir_resize(temp.data(), (uint32_t *)pixels, sw, sh, info.width,
                         info.height);
  } else {
    gDecoder.setTargetSize(decoder, info.width, info.height);
    gDecoder.decodeImage(decoder, pixels, info.stride,
                         info.stride * info.height);
  }

  gDecoder.deleteDecoder(decoder);
  env->ReleasePrimitiveArrayCritical(jData, data, JNI_ABORT);

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height,
                    denoisingStrength);
  }

  if (filters & 1) {
    if ((filters & 4) || (filters & 8) || (filters & 16)) {
      hikari::rcas((uint32_t *)pixels, info.width, info.height,
                   2.0f - sharpeningStrength);
    } else {
      hikari::sharpen((uint32_t *)pixels, info.width, info.height,
                      sharpeningStrength);
    }
  }

  AndroidBitmap_unlockPixels(env, bitmap);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeDecodeRegion(
    JNIEnv *env, jobject thiz, jobject bitmap, jbyteArray jData, jint length,
    jint left, jint top, jint right, jint bottom, jint sampleSize, jint filters,
    jfloat sharpeningStrength, jfloat denoisingStrength) {
  if (bitmap == nullptr || jData == nullptr || !gDecoder.available)
    return JNI_FALSE;

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
      info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
    return JNI_FALSE;
  }

  if (info.format == ANDROID_BITMAP_FORMAT_RGB_565 && filters != 0) {
    return JNI_FALSE;
  }

  void *pixels;
  if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != 0) {
    return JNI_FALSE;
  }

  jbyte *data = (jbyte *)env->GetPrimitiveArrayCritical(jData, nullptr);
  if (!data) {
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_FALSE;
  }

  AImageDecoder *decoder = nullptr;
  int ret = gDecoder.createFromBuffer(data, length, &decoder);
  if (ret != 0) {
    env->ReleasePrimitiveArrayCritical(jData, data, JNI_ABORT);
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_FALSE;
  }

  if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
    gDecoder.setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGB_565);
  } else {
    gDecoder.setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);
  }

  if (gDecoder.setTargetRect) {
    gDecoder.setTargetRect(decoder, {left, top, right, bottom});
  }

  int targetWidth = (right - left) / sampleSize;
  int targetHeight = (bottom - top) / sampleSize;
  if (gDecoder.setTargetSize) {
    gDecoder.setTargetSize(decoder, targetWidth, targetHeight);
  }

  gDecoder.decodeImage(decoder, pixels, info.stride, info.stride * info.height);

  gDecoder.deleteDecoder(decoder);
  env->ReleasePrimitiveArrayCritical(jData, data, JNI_ABORT);

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height,
                    denoisingStrength);
  }

  if (filters & 1) {
    if ((filters & 4) || (filters & 8) || (filters & 16)) {
      hikari::rcas((uint32_t *)pixels, info.width, info.height,
                   2.0f - sharpeningStrength);
    } else {
      hikari::sharpen((uint32_t *)pixels, info.width, info.height,
                      sharpeningStrength);
    }
  }

  AndroidBitmap_unlockPixels(env, bitmap);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_tachiyomi_core_common_util_system_NativeImageDecoder_nativeProcess(
    JNIEnv *env, jobject thiz, jobject bitmap, jint filters,
    jfloat sharpeningStrength, jfloat denoisingStrength) {
  if (bitmap == nullptr)
    return JNI_FALSE;

  AndroidBitmapInfo info;
  AndroidBitmap_getInfo(env, bitmap, &info);

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    return JNI_FALSE;

  void *pixels;
  if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != 0) {
    return JNI_FALSE;
  }

  if (filters & 4) {
    LOGD("Native post-processing filter applied: UPSCALING");
  } else if (filters & 8) {
    LOGD("Native post-processing filter applied: AVIR");
  } else if (filters & 16) {
    LOGD("Native post-processing filter applied: LANCIR");
  }

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height,
                    denoisingStrength);
  }

  if (filters & 1) {
    if ((filters & 4) || (filters & 8) || (filters & 16)) {
      hikari::rcas((uint32_t *)pixels, info.width, info.height,
                   2.0f - sharpeningStrength);
    } else {
      hikari::sharpen((uint32_t *)pixels, info.width, info.height,
                      sharpeningStrength);
    }
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
