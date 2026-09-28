package com.wallee.terminallinker.core.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/** Debug-only request log: method, path and status. Never headers (the token) and never bodies. */
class DebugLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val start = System.nanoTime()
        val response = chain.proceed(request)
        val ms = (System.nanoTime() - start) / 1_000_000
        Log.d(TAG, "${request.method} ${request.url.encodedPath} -> ${response.code} ($ms ms)")
        return response
    }

    private companion object {
        const val TAG = "wallee-api"
    }
}
