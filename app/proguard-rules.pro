# Trade-off of -dontobfuscate:
# Keeping -dontobfuscate allows stack traces in crash logs to be fully readable with original class/method names
# without needing a mapping file.
# However, this means that R8 will not rename any class, method, or field,
# which results in a slightly larger APK size compared to a fully obfuscated build.
# Shrinking (removing unused code)
# and optimization are still fully active.
-dontobfuscate
-keepattributes Signature, InnerClasses, EnclosingMethod, Annotation

# Narrow keep rules instead of blanket keep rules for eu.kanade.**, tachiyomi.**, hikari.**
-keep class eu.kanade.tachiyomi.source.** { public protected *; }
-keep class tachiyomi.source.** { public protected *; }

# [TAMBAHKAN INI] Lindungi Source API untuk package reader barumu
-keep class com.package.readerx.source.** { public protected *; }

# Keep MainActivity explicitly
-keep class eu.kanade.tachiyomi.ui.main.MainActivity
-keep class com.package.readerx.ui.main.MainActivity

# Keep Shizuku AIDL service implementation classes
-keep class hikari.app.shizuku.** { *; }

# GeckoView specific rules
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.SysInfo { *; }
-keep class org.mozilla.gecko.mozglue.JNIObject { *; }
-keep class * extends org.mozilla.gecko.mozglue.JNIObject { *; }
-keep @interface org.mozilla.gecko.annotation.JNITarget
-keep @org.mozilla.gecko.annotation.JNITarget class *
-keepclassmembers @org.mozilla.gecko.annotation.JNITarget class * { *; }
-keepclassmembers class * { @org.mozilla.gecko.annotation.JNITarget *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn mozilla.components.**

# Keep custom WebView/Cookie components
-keep class eu.kanade.tachiyomi.ui.webview.** { public protected *; }
-keep class hikari.**.webview.** { public protected *; }
-keep class hikari.**.cookie.** { public protected *; }

# SubsamplingScaleImageView reflection field 'decoder'
-keepclassmembers class com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView {
    private com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder decoder;
}

# Keep common dependencies used in extensions
-keep,allowoptimization class androidx.preference.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class kotlin.time.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class rx.** { public protected *; }
-keep,allowoptimization class app.cash.quickjs.** { public protected *; }

# INJEKT DI RULES (Sangat krusial untuk mencegah force close di App.onCreate)
-keep class uy.kohesive.injekt.** { *; }
-keep class dev.mihon.injekt.** { *; }
-keep class * extends uy.kohesive.injekt.api.FullTypeReference { *; }
-keep class * extends uy.kohesive.injekt.api.TypeReference { *; }

# Domain / DI Modules
-keep class eu.kanade.tachiyomi.di.** { *; }
-keep class eu.kanade.domain.** { *; }
-keep class tachiyomi.domain.** { *; }
-keep class hikari.domain.** { *; }
# [TAMBAHKAN INI] Lindungi domain dan DI module milik aplikasimu
-keep class com.package.readerx.di.** { *; }
-keep class com.package.readerx.domain.** { *; }

# Service & Preferences
-keep class tachiyomi.domain.download.service.** { *; }
-keep class tachiyomi.domain.library.service.** { *; }
-keep class tachiyomi.domain.backup.service.** { *; }
-keep class tachiyomi.domain.storage.service.** { *; }
-keep class tachiyomi.domain.updates.service.** { *; }
-keep class eu.kanade.domain.base.** { *; }
-keep class eu.kanade.domain.source.service.** { *; }
-keep class eu.kanade.domain.track.service.** { *; }
-keep class eu.kanade.domain.ui.** { *; }
-keep class eu.kanade.tachiyomi.core.security.** { *; }
-keep class eu.kanade.tachiyomi.network.NetworkPreferences { *; }
-keep class eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences { *; }

# Network Helpers (Extensions)
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.RateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.SpecificHostRateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.NetworkHelper { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.OkHttpExtensionsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.RequestsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.AppInfo { public protected *; }

# RxJava 1.x
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontnote rx.internal.util.PlatformDependent

# OkHttp
-keepclasseswithmembers class okhttp3.MultipartBody$Builder { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
-keepclassmembers class eu.kanade.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class tachiyomi.**$$serializer { *; }
-keepclassmembers class tachiyomi.** {
    *** Companion;
}
-keepclasseswithmembers class tachiyomi.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class hikari.**$$serializer { *; }
-keepclassmembers class hikari.** {
    *** Companion;
}
-keepclasseswithmembers class hikari.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# [TAMBAHKAN INI] Serialization rules untuk package barumu
-keep,includedescriptorclasses class com.package.readerx.**$$serializer { *; }
-keepclassmembers class com.package.readerx.** {
    *** Companion;
}
-keepclasseswithmembers class com.package.readerx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}

# XmlUtil
-keep public enum nl.adaptivity.xmlutil.EventType { *; }

# Firebase
-keep class com.google.firebase.installations.** { *; }
-keep interface com.google.firebase.installations.** { *; }

# Android Window Extensions
-dontwarn androidx.window.**

# JSoup optional re2j dependency
-dontwarn com.google.re2j.**
