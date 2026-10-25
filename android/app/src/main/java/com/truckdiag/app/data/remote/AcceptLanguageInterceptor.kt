package com.truckdiag.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

internal class AcceptLanguageInterceptor(
    private val languageTagProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val language = languageTagProvider()
            .substringBefore(',')
            .substringBefore('-')
            .takeIf { it == "ky" }
            ?: "ru"

        return chain.proceed(
            chain.request()
                .newBuilder()
                .header("Accept-Language", language)
                .build()
        )
    }
}