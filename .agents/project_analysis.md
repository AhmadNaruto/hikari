# Analisis Project & Daftar File Penting — Hikari (AhmadNaruto/hikari)

Dokumen ini mencatat pemetaan arsitektur dan berkas-berkas penting di dalam codebase Hikari. Tujuannya adalah mempermudah developer atau agen kecerdasan buatan berikutnya dalam memahami alur kerja proyek tanpa perlu melakukan pencarian dari nol.

---

## 1. Native C++ & JNI Image Pipeline

Kumpulan file yang menangani pemrosesan gambar tingkat rendah (resizing, sharpening, denoising, dan integrasi libvips).

*   [`app/src/main/cpp/CMakeLists.txt`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/CMakeLists.txt)
    *   **Peran:** Konfigurasi build CMake untuk modul C++ native. Mengatur target shared library `hikari-image` dan `bbfjni`, menentukan flag optimasi (`-O3 -ffast-math`), serta mengimpor dan menautkan precompiled libraries libvips.
*   [`app/src/main/cpp/hikari-image-pipeline.cpp`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/hikari-image-pipeline.cpp)
    *   **Peran:** Pipeline pemrosesan utama gambar di tingkat native. Mengimplementasikan algoritma upscaling AMD EASU, resizer AVIR & LANCIR (dengan NEON SIMD), sharpening RCAS, denoise, serta pengelolaan `thread_local` scratch buffer untuk efisiensi alokasi heap.
*   [`app/src/main/cpp/vips_jni.cpp`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/vips_jni.cpp)
    *   **Peran:** JNI Wrapper untuk pustaka `libvips`. Berisi fungsi-fungsi kompresi dan konversi format gambar unduhan ke JPEG, WebP, dan PNG secara langsung di memori native.
*   [`app/src/main/jniLibs/arm64-v8a/`](file:///data/data/com.termux/files/home/hikari/app/src/main/jniLibs/arm64-v8a/)
    *   **Peran:** Folder berisi precompiled shared libraries (`.so`) libvips & dependensinya (seperti GLib, Gio, GObject, Zlib, dan Libintl) khusus untuk arsitektur target ARM64.
*   [`app/src/main/cpp/include/`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/include/)
    *   **Peran:** File header C++ pendukung (seperti `<vips/vips.h>`, `<glib.h>`) yang dibutuhkan oleh CMake untuk proses kompilasi native.

---

## 2. BBF Codec (Binary Book Format)

Format biner khusus untuk pengarsipan dan pendistribusian bab manga terdedup/kompresi tinggi.

*   [`app/src/main/cpp/bbf/libbbf.h`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/bbf/libbbf.h)
    *   **Peran:** Definisi struktur biner file `.bbf` (header offsets, footer, struct meta, dan sections).
*   [`app/src/main/cpp/bbf/bbfcodec.h` & `bbfcodec.cpp`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/bbf/bbfcodec.cpp)
    *   **Peran:** Implementasi logika reader (mmap-based zero-copy read) dan builder (pembacaan file ke buffer via helper `readFileToBuffer` terpadu, penulisan tabel asset, pengelompokkan halaman, serta pemangkasan duplikasi bytes).
*   [`app/src/main/cpp/bbf/bbfjni.cpp`](file:///data/data/com.termux/files/home/hikari/app/src/main/cpp/bbf/bbfjni.cpp)
    *   **Peran:** Jafascript Native Interface (JNI) untuk BBF. Dioptimalkan dengan caching reference `jclass` dan `jmethodID` global di `JNI_OnLoad` untuk mencegah overhead refleksi JVM saat ekstraksi metadata/halaman.

---

## 3. Alur Unduhan (Download Pipeline) & Konfigurasi

Bagian Kotlin yang mengkoordinasikan proses pengambilan gambar dari internet, manipulasi gambar (resize/split/convert), dan pengarsipan.

*   [`app/src/main/java/eu/kanade/tachiyomi/data/download/Downloader.kt`](file:///data/data/com.termux/files/home/hikari/app/src/main/java/eu/kanade/tachiyomi/data/download/Downloader.kt)
    *   **Peran:** Koordinator utama download manga. Memanggil `resizeImageIfNeeded()`, pembelahan gambar panjang (`splitTallImageIfNeeded()`), serta memicu pemrosesan kompresi/konversi libvips (`compressImagesIfNeeded()`) termasuk mekanisme fallback jika gambar melebihi limit WebP (16383px).
*   [`domain/src/main/java/tachiyomi/domain/download/service/DownloadPreferences.kt`](file:///data/data/com.termux/files/home/hikari/domain/src/main/java/tachiyomi/domain/download/service/DownloadPreferences.kt)
    *   **Peran:** Skema penyimpanan preferensi unduhan (resize size, resize filter, status konversi libvips, target format, dan kualitas).
*   [`app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDownloadScreen.kt`](file:///data/data/com.termux/files/home/hikari/app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDownloadScreen.kt)
    *   **Peran:** Antarmuka pengaturan (Compose UI) untuk menu Downloads, menampilkan slider kualitas secara dinamis tergantung pada format target yang dipilih.

---

## 4. Halaman Library & Manajemen Ekstensi

Bagian yang memproses data manga favorit, melacak status inisialisasi modul ekstensi, dan merender badge informasi di UI.

*   [`app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryScreenModel.kt`](file:///data/data/com.termux/files/home/hikari/app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryScreenModel.kt)
    *   **Peran:** ViewModel/ScreenModel untuk halaman Library. Mengatur flow kategori manga, pengurutan, filter, dan reaktivitas aliran data favorit (`getFavoritesFlow()`) yang dikombinasikan dengan `sourceManager.isInitialized` untuk memastikan badge bahasa ter-update begitu ekstensi selesai dimuat.
*   [`app/src/main/java/eu/kanade/tachiyomi/source/AndroidSourceManager.kt`](file:///data/data/com.termux/files/home/hikari/app/src/main/java/eu/kanade/tachiyomi/source/AndroidSourceManager.kt)
    *   **Peran:** Pengelola utama dafar ekstensi yang terinstal dan stub source. Berisi perbaikan bug sinkronisasi database ke memori (`stubSourcesMap`).
*   [`app/src/main/java/eu/kanade/presentation/library/components/LibraryBadges.kt`](file:///data/data/com.termux/files/home/hikari/app/src/main/java/eu/kanade/presentation/library/components/LibraryBadges.kt)
    *   **Peran:** Mengandung rendering komponen visual badge di library (`DownloadsBadge`, `UnreadBadge`, dan `LanguageBadge`).

---

## 5. Sistem Utilitas Pendukung (System Utils)

*   [`core/common/src/main/kotlin/tachiyomi/core/common/util/system/NativeImageDecoder.kt`](file:///data/data/com.termux/files/home/hikari/core/common/src/main/kotlin/tachiyomi/core/common/util/system/NativeImageDecoder.kt)
    *   **Peran:** Penghubung Kotlin untuk decoding gambar native. Memiliki kode inisialisasi statis pemuatan pustaka dependensi libvips.
*   [`core/common/src/main/kotlin/tachiyomi/core/common/util/system/VipsNative.kt`](file:///data/data/com.termux/files/home/hikari/core/common/src/main/kotlin/tachiyomi/core/common/util/system/VipsNative.kt)
    *   **Peran:** Interface internal untuk menjembatani kode Kotlin dengan pustaka native JNI `vips_jni.cpp`.
*   [`core/common/src/main/kotlin/tachiyomi/core/common/util/system/ImageUtil.kt`](file:///data/data/com.termux/files/home/hikari/core/common/src/main/kotlin/tachiyomi/core/common/util/system/ImageUtil.kt)
    *   **Peran:** Utilitas manipulasi gambar (menghitung aspek rasio, mendeteksi tinggi webtoon, mengekstrak properti dimensi, membagi gambar panjang).

---

## 6. Konfigurasi ProGuard & R8

*   [`app/build.gradle.kts`](file:///data/data/com.termux/files/home/hikari/app/build.gradle.kts)
    *   **Peran:** Mengatur setting rilis APK, obfuscation, target SDK, dan status pengecilan kode (`isMinifyEnabled`/`isShrinkResources`).
*   [`app/proguard-rules.pro`](file:///data/data/com.termux/files/home/hikari/app/proguard-rules.pro)
    *   **Peran:** Aturan keep ProGuard yang dioptimalkan untuk memangkas ukuran biner APK sembari menjaga entri refleksi pemuatan ekstensi runtime dan JNI native entrypoints agar tidak hilang.
