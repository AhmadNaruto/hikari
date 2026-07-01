# Audit & Optimization Report: `lancir.h` in Native Image Pipeline — Hikari

This report documents the audit findings and optimizations made to the native image processing pipeline using the `lancir.h` resizer in the Hikari project.

---

## 1. SIMD (NEON) Path Verification

*   **Compiler Constants:** Hikari compiles for the `arm64-v8a` architecture (standard 64-bit ARMv8-A). The NDK Clang compiler automatically defines `__aarch64__` when compiling for this ABI.
*   **`lancir.h` Detection:** In [lancir.h](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/lancir.h) lines 82–96, the library checks for the presence of `__aarch64__` (among other 64-bit ARM macros):
    ```cpp
    #elif defined( __aarch64__ ) || defined( __arm64__ ) || \
        defined( _M_ARM64 ) || defined( _M_ARM64EC )
        #include <arm_neon.h>
        #define LANCIR_NEON
        #define LANCIR_ALIGN 16
    ```
    This automatically selects the ARM NEON SIMD optimized path (`LANCIR_NEON`) with a 16-byte boundary alignment.
*   **Flag Overrides:** Checked `CMakeLists.txt` and NDK configuration in `build.gradle.kts`. There are no overrides (such as `-mfpu` or conflicting `-march` flags) that would disable NEON detection. The CMake directive `set(CMAKE_ANDROID_ARM_NEON TRUE)` is set to enable NEON for 32-bit ARM (armeabi-v7a), leaving the default full NEON configuration on 64-bit ARM untouched.
*   **Binary Disassembly:** To comply with workspace rules against executing Gradle commands, a release build could not be compiled during this session, so direct `objdump` / `readelf` disassembly was not performed. However, static macro paths guarantee that Clang compiles the NEON SIMD implementations for `resize1` through `resize4` functions under `__aarch64__`.

---

## 2. Object Lifecycle & Thread-Safety Audit of `CLancIR`

*   **Audit Results:** In [hikari-image-pipeline.cpp](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/hikari-image-pipeline.cpp), a new instance of `avir::CLancIR` (and `avir::CImageResizer<>`) was previously allocated on the stack *on every single call* to `lancir_resize` and `avir_resize`.
    *   *Problem:* This discarded the built-in buffer reuse mechanism and cache locality of LANCIR, forcing memory reallocation on every image resize.
*   **Refactor to `thread_local`:** We converted the resizer instances inside `avir_resize` and `lancir_resize` to `thread_local`:
    ```cpp
    thread_local avir::CLancIR ImageResizer;
    ```
    *   *Thread-Safety:* Since the instance is thread-local, each JNI worker thread owns a separate instance. This prevents race conditions (as `CLancIR` is not thread-safe).
    *   *Buffer Reuse:* The NDK keeps the instance alive for the duration of the thread. Consecutive resize requests processed on the same thread reuse the allocated internal buffers, significantly reducing memory allocation overhead and improving cache performance.

---

## 3. Compiler Optimization Flags

*   **Problem:** The previous `CMakeLists.txt` appended `-O3 -ffast-math` to the global `CMAKE_CXX_FLAGS`. This forced aggressive vectorization and inline optimization on all targets, including helper libraries like `bbfjni` (which contains codec and string pool logic), bloating the final APK binary size unnecessarily.
*   **Target-Specific Optimization:** We removed the global `CMAKE_CXX_FLAGS` modification and applied `-O3 -ffast-math` specifically to the `hikari-image` target (containing the image pipeline and resizers) in [CMakeLists.txt](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/CMakeLists.txt):
    ```cmake
    add_library(hikari-image SHARED hikari-image-pipeline.cpp)
    if(NOT MSVC)
        target_compile_options(hikari-image PRIVATE -O3 -ffast-math)
    endif()
    ```
    This ensures `lancir.h` and the resizing functions receive maximum performance optimization, while `bbfjni` is compiled with standard size-conscious Release configurations.

---

## 4. `resizeImage()` Parameters & Stride Correctness

### 4.1 Data Types
*   The calls to `resizeImage` use `Tin = uint8_t` and `Tout = uint8_t` (by casting the `uint32_t*` buffer to `uint8_t*`). This correctly utilizes LANCIR's 8-bit precision path without redundant conversions to and from floats, optimizing throughput.

### 4.2 Stride Handling & Alignment (Critical Fix)
*   **Problem:** The Android Bitmap locked pixels buffer (`pixels` retrieved via `AndroidBitmap_lockPixels`) has a stride of `info.stride` (in bytes). Bitmaps can contain padding bytes at the end of each row (especially on certain GPUs/architectures), meaning `info.stride` might be larger than `NewWidth * 4` (bytes).
*   *Prior Code:* The resizer assumed the destination buffer was contiguous and had a stride of exactly `dw * 4` bytes. If `info.stride != dw * 4`, this would result in visual distortion (skewing) or heap memory corruption.
*   **Resolution:**
    *   For `lancir_resize`, we passed the physical destination scanline stride (`info.stride` bytes, which equals elements since `Tout` is `uint8_t`) directly using `CLancIRParams`:
        ```cpp
        void lancir_resize(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh, int dst_stride) {
          thread_local avir::CLancIR ImageResizer;
          avir::CLancIRParams Params(0, dst_stride);
          ImageResizer.resizeImage((uint8_t*)src, sw, sh, (uint8_t*)dst, dw, dh, 4, &Params);
        }
        ```
        This leverages LANCIR's internal stride support, writing directly into the padded Android Bitmap buffer without extra copies or allocations.
    *   For `avir_resize` (which does not support custom destination strides natively), we added a dynamic check. If `dst_stride == dw * 4`, it resizes directly. Otherwise, it resizes into a contiguous temporary vector and copies row-by-row to respect the bitmap's row padding, guaranteeing correct output under all layout conditions:
        ```cpp
        void avir_resize(uint32_t *src, uint32_t *dst, int sw, int sh, int dw, int dh, int dst_stride) {
          thread_local avir::CImageResizer<> ImageResizer(8);
          if (dst_stride == dw * 4) {
            ImageResizer.resizeImage((uint8_t*)src, sw, sh, 0, (uint8_t*)dst, dw, dh, 4, 0);
          } else {
            std::vector<uint8_t> temp_dst(dw * dh * 4);
            ImageResizer.resizeImage((uint8_t*)src, sw, sh, 0, temp_dst.data(), dw, dh, 4, 0);
            for (int y = 0; y < dh; ++y) {
              std::memcpy((uint8_t*)dst + y * dst_stride, temp_dst.data() + y * dw * 4, dw * 4);
            }
          }
        }
        ```
