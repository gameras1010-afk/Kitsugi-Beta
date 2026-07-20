# ══════════════════════════════════════════════════════════════════════════════
# Kitsugi — ProGuard / R8 keep rules
# T3-02: isMinifyEnabled = true için kapsamlı kural seti
# ══════════════════════════════════════════════════════════════════════════════

# ── Debug bilgisi (crash report için satır numaraları) ────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations

# ── Kotlin ────────────────────────────────────────────────────────────────────
# Coroutines & reflection (extensions yükleme için)
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory
-keep class kotlin.coroutines.Continuation

# Kotlinx Serialization generated serializers
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# DataStore protobuf stubs
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers class * {
    @dagger.* <methods>;
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── OkHttp / OkIO ─────────────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-keep class okio.** { *; }
-keepclassmembers class okio.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn okhttp3.internal.sse.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# ── Gson ──────────────────────────────────────────────────────────────────────
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Jackson ───────────────────────────────────────────────────────────────────
-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class com.fasterxml.jackson.** { *; }
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# ── Supabase / Ktor ───────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── ExoPlayer / Media3 ────────────────────────────────────────────────────────
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class androidx.media.** { *; }
# Kitsugi Engine native JNI handles (custom Media3 forks)
-keep class androidx.media3.exoplayer.upstream.DefaultAllocatorNative {
    native <methods>;
}
-keep class androidx.media3.exoplayer.source.SampleDataQueueNative {
    native <methods>;
}
-keep class androidx.media3.exoplayer.upstream.Allocation {
    <init>(java.nio.ByteBuffer, int, long);
    public long nativeHandle;
}

# ── MPV (native JNI callbacks) ────────────────────────────────────────────────
# is.xyz.mpv'nin tüm native callback yöntemleri reflection ile çözülür.
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** { *; }
# KitsugiMpvSurfaceView — native bridge
-keep class com.kitsugi.animelist.core.player.engine.KitsugiMpvSurfaceView { *; }
-keep class com.kitsugi.animelist.core.player.engine.MpvPlayerEngine { *; }

# ── CloudStream / Lagradost (plugin system — DexClassLoader) ──────────────────
# Eklentiler runtime'da host classloader'dan bu sınıfları isimle çözer.
# R8 bunları yeniden adlandırmamalı veya silmemeli.
-keep class com.lagradost.cloudstream3.** { *; }
-keepclassmembers class com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.nicehttp.** { *; }
-keepclassmembers class com.lagradost.nicehttp.** { *; }
-keep class com.lagradost.api.** { *; }
-keepclassmembers class com.lagradost.api.** { *; }
# Jsoup (eklentilerin kullandığı)
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn com.google.re2j.**
-dontwarn javax.script.**
-dontwarn org.jsoup.helper.Re2jRegex
# RxJava (manga extension compat)
-keep class io.reactivex.** { *; }
-keepclassmembers class io.reactivex.** { *; }
# Kotlin reflect (extension loading)
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.reflect.** { *; }

# ── Kotatsu Parsers Redo (manga kaynakları) ───────────────────────────────────
# Kotatsu manga parsers kendi sınıflarını reflection ile yükler.
-keep class org.koitharu.kotatsu.parsers.** { *; }
-keepclassmembers class org.koitharu.kotatsu.parsers.** { *; }

# ── NanoHTTPD (TV Companion Server) ───────────────────────────────────────────
-keep class fi.iki.elonen.** { *; }
-keep class org.nanohttpd.** { *; }

# ── ZXing (QR Code) ───────────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }
-keepclassmembers class com.google.zxing.** { *; }

# ── Coil (image loader) ───────────────────────────────────────────────────────
-dontwarn coil.**

# ── androidx.preference (manga extension ConfigurableSource) ──────────────────
-keep class androidx.preference.** { *; }
-keepclassmembers class androidx.preference.** { *; }

# ── AndroidX / Compose genel ──────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.lifecycle.** { *; }

# ── Uygulama veri sınıfları (Gson/Jackson ile JSON'a yazılanlar) ──────────────
-keep class com.kitsugi.animelist.data.** { *; }
-keepclassmembers class com.kitsugi.animelist.data.** { *; }

# ── FileProvider manifest bağlantısı ──────────────────────────────────────────
-keep class androidx.core.content.FileProvider

# ── WebView JS interface (splash animasyonu) ──────────────────────────────────
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Missing class stubs ───────────────────────────────────────────────────────
-dontwarn org.mozilla.javascript.**
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.checkerframework.**