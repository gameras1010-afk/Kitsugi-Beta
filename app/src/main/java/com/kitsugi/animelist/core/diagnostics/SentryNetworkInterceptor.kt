package com.kitsugi.animelist.core.diagnostics

import okhttp3.Interceptor
import okhttp3.Response

class SentryNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Stub: no-op network breadcrumb logging
        return chain.proceed(chain.request())
    }
}
