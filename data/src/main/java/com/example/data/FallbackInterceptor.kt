package com.example.data

import okhttp3.Interceptor
import okhttp3.Response

class FallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        if (!response.isSuccessful) {
            // Retry with fallback host (using "latest" as date)
            val fallbackUrl = request.url.newBuilder()
                .host("latest.currency-api.pages.dev")
                .build()
            val fallbackRequest = request.newBuilder().url(fallbackUrl).build()
            response = chain.proceed(fallbackRequest)
        }
        return response
    }
}
