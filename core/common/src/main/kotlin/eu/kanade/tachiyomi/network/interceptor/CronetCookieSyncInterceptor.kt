package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that synchronizes cookies between OkHttp's CookieJar and the request/response headers.
 * This is necessary when using Cronet via CronetInterceptor, as Cronet bypasses OkHttp's CookieJar.
 */
class CronetCookieSyncInterceptor(private val cookieJar: CookieJar) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cookies = cookieJar.loadForRequest(request.url)

        val newRequest = if (cookies.isNotEmpty()) {
            val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
            request.newBuilder()
                .header("Cookie", cookieHeader)
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)

        val setCookies = response.headers("Set-Cookie")
        if (setCookies.isNotEmpty()) {
            val parsedCookies = setCookies.mapNotNull { Cookie.parse(request.url, it) }
            cookieJar.saveFromResponse(request.url, parsedCookies)
        }

        return response
    }
}
