package eu.kanade.tachiyomi.network

import android.content.Context
import com.google.net.cronet.okhttptransport.CronetInterceptor
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.CronetCookieSyncInterceptor
import eu.kanade.tachiyomi.network.interceptor.IgnoreGzipInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.chromium.net.CronetEngine
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(
    private val context: Context,
    private val preferences: NetworkPreferences,
) {

    val cookieJar = AndroidCookieJar()

    private val cronetEngine by lazy {
        CronetEngine.Builder(context)
            .enableQuic(true)
            .enableHttp2(true)
            .enableBrotli(true)
            .setStoragePath(File(context.cacheDir, "cronet_cache").apply { mkdirs() }.absolutePath)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, 100 * 1024 * 1024)
            .build()
    }

    private fun baseClientBuilder(): OkHttpClient.Builder {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }
        val connectionPool = ConnectionPool(16, 3, TimeUnit.MINUTES)

        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .cache(
                Cache(
                    directory = File(context.cacheDir, "network_cache"),
                    maxSize = 100L * 1024 * 1024,
                ),
            )
            .addInterceptor(UncaughtExceptionInterceptor())
            .addInterceptor(UserAgentInterceptor(::defaultUserAgentProvider))
            .addNetworkInterceptor(IgnoreGzipInterceptor())
            .addNetworkInterceptor(BrotliInterceptor)

        when (preferences.dohProvider.get()) {
            PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
            PREF_DOH_GOOGLE -> builder.dohGoogle()
            PREF_DOH_ADGUARD -> builder.dohAdGuard()
            PREF_DOH_QUAD9 -> builder.dohQuad9()
            PREF_DOH_ALIDNS -> builder.dohAliDNS()
            PREF_DOH_DNSPOD -> builder.dohDNSPod()
            PREF_DOH_360 -> builder.doh360()
            PREF_DOH_QUAD101 -> builder.dohQuad101()
            PREF_DOH_MULLVAD -> builder.dohMullvad()
            PREF_DOH_CONTROLD -> builder.dohControlD()
            PREF_DOH_NJALLA -> builder.dohNajalla()
            PREF_DOH_SHECAN -> builder.dohShecan()
        }

        return builder
    }

    fun addCronetInterceptor(builder: OkHttpClient.Builder) {
        if (preferences.enableCronet.get()) {
            builder.addInterceptor(CronetCookieSyncInterceptor(cookieJar))
            val cronetInterceptor = CronetInterceptor.newBuilder(cronetEngine).build()
            builder.addInterceptor { chain ->
                try {
                    cronetInterceptor.intercept(chain)
                } catch (e: Exception) {
                    var isCronetError = false
                    var current: Throwable? = e
                    while (current != null) {
                        if (current.javaClass.name.contains("org.chromium.net") ||
                            current is java.util.concurrent.ExecutionException ||
                            (
                                current is NullPointerException &&
                                    current.message?.contains("okhttp3.Response\$Builder.body") == true
                                )
                        ) {
                            isCronetError = true
                            break
                        }
                        current = current.cause
                    }

                    if (isCronetError) {
                        chain.proceed(chain.request())
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    fun addLoggingInterceptor(builder: OkHttpClient.Builder) {
        if (preferences.verboseLogging.get()) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addNetworkInterceptor(httpLoggingInterceptor)
        }
    }

    fun baseClientBuilder(authenticated: Boolean): OkHttpClient.Builder {
        val builder = baseClientBuilder()
        return builder
    }

    private val clientBuilder = baseClientBuilder().also {
        addCronetInterceptor(it)
        addLoggingInterceptor(it)
    }

    val nonCloudflareClient = clientBuilder.build()

    val client = clientBuilder
        .addInterceptor(
            CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider),
        )
        .build()

    /**
     * @deprecated Since extension-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient = client

    fun defaultUserAgentProvider() = preferences.defaultUserAgent.get().trim()
}
