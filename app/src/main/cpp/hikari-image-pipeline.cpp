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
  return {(pixel & 0xFF) / 255.0f, ((pixel >> 8) & 0xFF) / 255.0f,
          ((pixel >> 16) & 0xFF) / 255.0f, ((pixel >> 24) & 0xFF) / 255.0f};
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

inline void FsrEasuTap(float &aR, float &aG, float &aB, float &aA, float &aW,
                       float offX, float offY, float dirX, float dirY,
                       float lenX, float lenY, float lob, float clp,
                       const FloatColor &c) {
  float vx = offX * dirX + offY * dirY;
  float vy = offX * -dirY + offY * dirX;
  vx *= lenX;
  vy *= lenY;
  float d2 = vx * vx + vy * vy;
  d2 = std::min(d2, clp);
  float wB = 0.4f * d2 - 1.0f;
  float wA = lob * d2 - 1.0f;
  wB *= wB;
  wA *= wA;
  wB = 1.5625f * wB - 0.5625f;
  float w = wB * wA;
  aR += c.r * w;
  aG += c.g * w;
  aB += c.b * w;
  aA += c.a * w;
  aW += w;
}

inline void FsrEasuSet(float &dirX, float &dirY, float &len, float ppX,
                       float ppY, bool biS, bool biT, bool biU, bool biV,
                       float lA, float lB, float lC, float lD, float lE) {
  float w = 0.0f;
  if (biS)
    w = (1.0f - ppX) * (1.0f - ppY);
  if (biT)
    w = ppX * (1.0f - ppY);
  if (biU)
    w = (1.0f - ppX) * ppY;
  if (biV)
    w = ppX * ppY;
  float dc = lD - lC;
  float cb = lC - lB;
  float lenX = std::max(std::abs(dc), std::abs(cb));
  lenX = APrxLoRcpF1(lenX);
  float dx = lD - lB;
  lenX = clamp(std::abs(dx) * lenX, 0.0f, 1.0f);
  lenX *= lenX;
  float ec = lE - lC;
  float ca = lC - lA;
  float lenY = std::max(std::abs(ec), std::abs(ca));
  lenY = APrxLoRcpF1(lenY);
  float dy = lE - lA;
  lenY = clamp(std::abs(dy) * lenY, 0.0f, 1.0f);
  lenY *= lenY;
  dirX += dx * w;
  dirY += dy * w;
  len += w * lenX + w * lenY;
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

      FloatColor bL = getFloatColor(p_b);
      FloatColor cL = getFloatColor(p_c);
      FloatColor eL = getFloatColor(p_e);
      FloatColor fL = getFloatColor(p_f);
      FloatColor gL = getFloatColor(p_g);
      FloatColor hL = getFloatColor(p_h);
      FloatColor iL = getFloatColor(p_i);
      FloatColor jL = getFloatColor(p_j);
      FloatColor kL = getFloatColor(p_k);
      FloatColor lL = getFloatColor(p_l);
      FloatColor nL = getFloatColor(p_n);
      FloatColor oL = getFloatColor(p_o);

      float l_b = getLuma(bL);
      float l_c = getLuma(cL);
      float l_e = getLuma(eL);
      float l_f = getLuma(fL);
      float l_g = getLuma(gL);
      float l_h = getLuma(hL);
      float l_i = getLuma(iL);
      float l_j = getLuma(jL);
      float l_k = getLuma(kL);
      float l_l = getLuma(lL);
      float l_n = getLuma(nL);
      float l_o = getLuma(oL);

      float dirX = 0.0f;
      float dirY = 0.0f;
      float len = 0.0f;

      FsrEasuSet(dirX, dirY, len, ppX, ppY, true, false, false, false, l_b, l_e,
                 l_f, l_g, l_j);
      FsrEasuSet(dirX, dirY, len, ppX, ppY, false, true, false, false, l_c, l_f,
                 l_g, l_h, l_k);
      FsrEasuSet(dirX, dirY, len, ppX, ppY, false, false, true, false, l_f, l_i,
                 l_j, l_k, l_n);
      FsrEasuSet(dirX, dirY, len, ppX, ppY, false, false, false, true, l_g, l_j,
                 l_k, l_l, l_o);

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

      float aR = 0.0f, aG = 0.0f, aB = 0.0f, aA = 0.0f, aW = 0.0f;

      FsrEasuTap(aR, aG, aB, aA, aW, 0.0f - ppX, -1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, bL);
      FsrEasuTap(aR, aG, aB, aA, aW, 1.0f - ppX, -1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, cL);
      FsrEasuTap(aR, aG, aB, aA, aW, -1.0f - ppX, 1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, iL);
      FsrEasuTap(aR, aG, aB, aA, aW, 0.0f - ppX, 1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, jL);
      FsrEasuTap(aR, aG, aB, aA, aW, 0.0f - ppX, 0.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, fL);
      FsrEasuTap(aR, aG, aB, aA, aW, -1.0f - ppX, 0.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, eL);
      FsrEasuTap(aR, aG, aB, aA, aW, 1.0f - ppX, 1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, kL);
      FsrEasuTap(aR, aG, aB, aA, aW, 2.0f - ppX, 1.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, lL);
      FsrEasuTap(aR, aG, aB, aA, aW, 2.0f - ppX, 0.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, hL);
      FsrEasuTap(aR, aG, aB, aA, aW, 1.0f - ppX, 0.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, gL);
      FsrEasuTap(aR, aG, aB, aA, aW, 1.0f - ppX, 2.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, oL);
      FsrEasuTap(aR, aG, aB, aA, aW, 0.0f - ppX, 2.0f - ppY, dirX, dirY, len2X,
                 len2Y, lob, clp, nL);

      float rResult = aW > 0.0f ? (aR / aW) : fL.r;
      float gResult = aW > 0.0f ? (aG / aW) : fL.g;
      float bResult = aW > 0.0f ? (aB / aW) : fL.b;
      float aResult = aW > 0.0f ? (aA / aW) : fL.a;

      float minR = std::min(std::min(fL.r, gL.r), std::min(jL.r, kL.r));
      float maxR = std::max(std::max(fL.r, gL.r), std::max(jL.r, kL.r));
      rResult = clamp(rResult, minR, maxR);

      float minG = std::min(std::min(fL.g, gL.g), std::min(jL.g, kL.g));
      float maxG = std::max(std::max(fL.g, gL.g), std::max(jL.g, kL.g));
      gResult = clamp(gResult, minG, maxG);

      float minB = std::min(std::min(fL.b, gL.b), std::min(jL.b, kL.b));
      float maxB = std::max(std::max(fL.b, gL.b), std::max(jL.b, kL.b));
      bResult = clamp(bResult, minB, maxB);

      float minA = std::min(std::min(fL.a, gL.a), std::min(jL.a, kL.a));
      float maxA = std::max(std::max(fL.a, gL.a), std::max(jL.a, kL.a));
      aResult = clamp(aResult, minA, maxA);

      dst[y * dw + x] =
          ((uint32_t)clamp(aResult * 255.0f, 0.0f, 255.0f) << 24) |
          ((uint32_t)clamp(bResult * 255.0f, 0.0f, 255.0f) << 16) |
          ((uint32_t)clamp(gResult * 255.0f, 0.0f, 255.0f) << 8) |
          (uint32_t)clamp(rResult * 255.0f, 0.0f, 255.0f);
    }
  }
}

inline float computeRcasLobe(float e, float b, float d, float f, float h,
                             float sharpness) {
  float mn = std::min(std::min(b, d), std::min(f, h));
  float mx = std::max(std::max(b, d), std::max(f, h));
  float mnL = std::min(mn, e);
  float mxL = std::max(mx, e);
  float hitMin = mnL / (4.0f * mxL + 1e-5f);
  float hitMax = (1.0f - mxL) / (4.0f * mnL - 4.0f - 1e-5f);
  float lobeL = std::max(-hitMin, hitMax);
  float limit = 0.1875f;
  float lobe =
      std::max(-limit, std::min(lobeL, 0.0f)) * std::pow(2.0f, -sharpness);

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

void rcas(uint32_t *pixels, int w, int h, float sharpness) {
  if (h < 3 || w < 3) return;

  std::vector<uint32_t> prev_row(w);
  std::vector<uint32_t> curr_row(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    for (int x = 1; x < w - 1; x++) {
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

      float lobeR = computeRcasLobe(eR, bR, dR, fR, hR, sharpness);
      float rR = (lobeR * (bR + dR + fR + hR) + eR) / (4.0f * lobeR + 1.0f);

      float lobeG = computeRcasLobe(eG, bG, dG, fG, hG, sharpness);
      float rG = (lobeG * (bG + dG + fG + hG) + eG) / (4.0f * lobeG + 1.0f);

      float lobeB = computeRcasLobe(eB, bB, dB, fB, hB, sharpness);
      float rB = (lobeB * (bB + dB + fB + hB) + eB) / (4.0f * lobeB + 1.0f);

      pixels[y * w + x] = ((uint32_t)clamp(eA * 255.0f, 0.0f, 255.0f) << 24) |
                          ((uint32_t)clamp(rB * 255.0f, 0.0f, 255.0f) << 16) |
                          ((uint32_t)clamp(rG * 255.0f, 0.0f, 255.0f) << 8) |
                          (uint32_t)clamp(rR * 255.0f, 0.0f, 255.0f);
    }

    std::memcpy(prev_row.data(), curr_row.data(), w * sizeof(uint32_t));
  }
}

void sharpen(uint32_t *pixels, int w, int h, float strength) {
  if (h < 3 || w < 3) return;

  std::vector<uint32_t> prev_row(w);
  std::vector<uint32_t> curr_row(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    for (int x = 1; x < w - 1; x++) {
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

void denoise(uint32_t *pixels, int w, int h, float strength) {
  if (h < 3 || w < 3) return;

  std::vector<uint32_t> prev_row(w);
  std::vector<uint32_t> curr_row(w);

  std::memcpy(prev_row.data(), pixels, w * sizeof(uint32_t));

  for (int y = 1; y < h - 1; y++) {
    std::memcpy(curr_row.data(), pixels + y * w, w * sizeof(uint32_t));

    for (int x = 1; x < w - 1; x++) {
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
    if (filters & 4) {
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
    if (filters & 4) {
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
  }

  if (filters & 2) {
    hikari::denoise((uint32_t *)pixels, info.width, info.height,
                    denoisingStrength);
  }

  if (filters & 1) {
    if (filters & 4) {
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
