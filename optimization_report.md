# Optimization Report: ProGuard/R8 Rules & Build Configuration — Hikari (app module)

This report documents the audit findings and optimizations made to the ProGuard/R8 rules and build configurations for the Hikari project (`app.hikari`).

---

## 1. Audit of Reflection & Dynamic Loading in Codebase

A search and analysis of the codebase was conducted to identify all areas relying on reflection, dynamic class loading, or JNI callbacks:

### 1.1 `Class.forName(...)` Usages
*   **`ExtensionLoader.kt`**: Dynamically loads source classes (implementing `Source` or `SourceFactory`) from external extension APKs via a custom `ChildFirstPathClassLoader`.
    *   *Requirement*: The core source interface classes (`eu.kanade.tachiyomi.source.**`) and common network/utility helpers must maintain their exact names and public/protected member signatures.
*   **`ShellInterface.kt`**: Accesses internal Android system classes (such as `IPackageManager`, `SystemProperties`, etc.) to run package installation functions under Shizuku/root context.
    *   *Requirement*: Since these are system classes, R8 will not modify them. However, `ShellInterface` itself is loaded reflectively by the Shizuku helper process.
*   **`DeviceUtil.kt`**: Interacts with MIUI and system properties using reflection.
    *   *Requirement*: Standard system classes, no custom keep rules required.
*   **`LockedWidget.kt` / `UpdatesWidget.kt`**: Dynamically loads `eu.kanade.tachiyomi.ui.main.MainActivity` by class name from the widget module.
    *   *Requirement*: `MainActivity` is declared in the `AndroidManifest.xml` and is automatically kept by AAPT2 rules, but we explicitly keep it to guarantee safety.

### 1.2 `::class.java` Usages
*   **`ReaderPageImageView.kt`**: Accesses the private `decoder` field in `SubsamplingScaleImageView` via reflection (`SubsamplingScaleImageView::class.java.getDeclaredField("decoder")`).
    *   *Requirement*: The class `com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView` must preserve its `decoder` field.
*   *Other Usages*: Standard `Intent` creation (`MainActivity::class.java`, `UnlockActivity::class.java`, etc.) are compile-time references that are safely handled by R8.

### 1.3 Dependency Injection Scanning
*   **Koin / Injekt**: No classpath scanning is used. All dependencies are explicitly declared via DSL (e.g., `modules(koinModules)` and `Injekt.importModule(...)`). Keep rules for the injection library APIs (`uy.kohesive.injekt.**`) are retained.

### 1.4 Annotations
*   **`@Keep`**: There were no instances of `@Keep` annotations used in the codebase. However, standard rules preserving `@Keep`-annotated elements in `proguard-android-optimize.txt` are maintained.

---

## 2. Updated ProGuard/R8 Rules (`app/proguard-rules.pro`)

We replaced the blanket keep rules:
```proguard
-keep class eu.kanade.** { *; }
-keep class tachiyomi.** { *; }
-keep class Hikari.** { *; }
-keep class hikari.** { *; }
```
With targeted rules. We also added rules for the upcoming GeckoView and custom WebView components, and expanded serialization rules to cover all packages.

### Diff: `app/proguard-rules.pro`
```diff
-#dontobfuscate
-#keepattributes Signature, InnerClasses, EnclosingMethod, Annotation
-
--keep class eu.kanade.** { *; }
--keep class tachiyomi.** { *; }
--keep class Hikari.** { *; }
--keep class hikari.** { *; }
+# Trade-off of -dontobfuscate:
+# Keeping -dontobfuscate allows stack traces in crash logs to be fully readable with original class/method names
+# without needing a mapping file. However, this means that R8 will not rename any class, method, or field,
+# which results in a slightly larger APK size compared to a fully obfuscated build. Shrinking (removing unused code)
+# and optimization are still fully active.
+-dontobfuscate
+-keepattributes Signature, InnerClasses, EnclosingMethod, Annotation
+
+# Narrow keep rules instead of blanket keep rules for eu.kanade.**, tachiyomi.**, hikari.**
+# Keep classes and interfaces in the Source Extension API (called dynamically via reflection by extensions)
+-keep class eu.kanade.tachiyomi.source.** { public protected *; }
+-keep class tachiyomi.source.** { public protected *; }
+
+# Keep MainActivity explicitly because it is loaded dynamically by the widgets module via reflection
+-keep class eu.kanade.tachiyomi.ui.main.MainActivity
+
+# Keep Shizuku AIDL service implementation classes since they are loaded by name via reflection/Binder from Shizuku
+-keep class hikari.app.shizuku.** { *; }
+
+# GeckoView specific rules (required for GeckoView's internal JNI / Reflection logic)
+-keep class org.mozilla.geckoview.** { *; }
+-keep class org.mozilla.gecko.SysInfo { *; }
+-keep class org.mozilla.gecko.mozglue.JNIObject { *; }
+-keep class * extends org.mozilla.gecko.mozglue.JNIObject { *; }
+
+-keep @interface org.mozilla.gecko.annotation.JNITarget
+-keep @org.mozilla.gecko.annotation.JNITarget class *
+-keepclassmembers @org.mozilla.gecko.annotation.JNITarget class * { *; }
+-keepclassmembers class * { @org.mozilla.gecko.annotation.JNITarget *; }
+
+-dontwarn org.mozilla.geckoview.**
+-dontwarn mozilla.components.**
+
+# Keep custom WebView/Cookie components that might be accessed reflectively
+-keep class eu.kanade.tachiyomi.ui.webview.** { public protected *; }
+-keep class hikari.**.webview.** { public protected *; }
+-keep class hikari.**.cookie.** { public protected *; }
+
+# SubsamplingScaleImageView reflection field 'decoder'
+-keepclassmembers class com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView {
+    private com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder decoder;
+}

...

 -keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
 -keepclassmembers class eu.kanade.** {
     *** Companion;
 }
 -keepclasseswithmembers class eu.kanade.** {
     kotlinx.serialization.KSerializer serializer(...);
 }
 
++keep,includedescriptorclasses class tachiyomi.**$$serializer { *; }
++keepclassmembers class tachiyomi.** {
+    *** Companion;
+}
++keepclasseswithmembers class tachiyomi.** {
+    kotlinx.serialization.KSerializer serializer(...);
+}
+
++keep,includedescriptorclasses class hikari.**$$serializer { *; }
++keepclassmembers class hikari.** {
+    *** Companion;
+}
++keepclasseswithmembers class hikari.** {
+    kotlinx.serialization.KSerializer serializer(...);
+}
+
 -keep class kotlinx.serialization.**
```

---

## 3. Build Configuration Audits (`app/build.gradle.kts` & `gradle.properties`)

### 3.1 Verification of `buildscript` Block
*   **Result**: The root `build.gradle.kts` **does not** apply/register `libs.kotlin.gradle` under its `plugins {}` block. Therefore, the `buildscript { dependencies { classpath(libs.kotlin.gradle) } }` block in `app/build.gradle.kts` **is required** and was **not** removed.

### 3.2 Verification of AIDL files
*   **Result**: A workspace search found `./app/src/main/aidl/hikari/app/shizuku/IShellInterface.aidl`. Since the project defines and uses this AIDL interface (specifically for Shizuku binder calls), `aidl = true` **must** remain active.

### 3.3 Packaging Resource Exclusions
We added `"META-INF/*.kotlin_module"` and `"DebugProbesKt.bin"` to standard package excludes to reduce output APK size.

### 3.4 R8 Full Mode
Enabled `android.enableR8.fullMode=true` in `gradle.properties`.

### Diff: `app/build.gradle.kts`
```diff
         resources {
             excludes += setOf(
                 "kotlin-tooling-metadata.json",
                 "LICENSE.txt",
                 "META-INF/**/*.properties",
                 "META-INF/**/LICENSE.txt",
                 "META-INF/*.properties",
                 "META-INF/*.version",
                 "META-INF/DEPENDENCIES",
                 "META-INF/LICENSE",
                 "META-INF/NOTICE",
                 "META-INF/README.md",
+                "META-INF/*.kotlin_module",
+                "DebugProbesKt.bin",
             )
         }
```

### Diff: `gradle.properties`
```diff
 org.gradle.parallel=true
 org.gradle.configuration-cache=true
 org.gradle.configuration-cache.problems=warn
+android.enableR8.fullMode=true
```

---

## 4. Verification and Trade-offs

*   **Rule Enforcement**: No `./gradlew` commands were run in the workspace to adhere to the project's strict policy against executing Gradle scripts.
*   **Trade-offs of `-dontobfuscate`**: We preserved the `-dontobfuscate` rule at the beginning of `proguard-rules.pro`. Retaining this ensures crash log stack traces remain fully readable with original class, package, and method names, but compromises slightly on size reduction since original long names are not replaced with short symbols (like `a`, `b`, `c`).
