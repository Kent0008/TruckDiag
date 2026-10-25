package com.truckdiag.app.data.remote

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class ApiClientTest {
    @After
    fun resetBaseUrl() {
        ApiClient.setBaseUrl(ApiClient.DEFAULT_BASE_URL)
    }

    @Test
    fun setBaseUrlAddsHttpSchemeAndTrailingSlash() {
        ApiClient.setBaseUrl("  diag.local:8000  ")

        assertEquals("http://diag.local:8000/", ApiClient.getBaseUrl())
    }

    @Test
    fun setBaseUrlPreservesHttpsAndNormalizesTrailingSlash() {
        ApiClient.setBaseUrl("https://diag.example/api")

        assertEquals("https://diag.example/api/", ApiClient.getBaseUrl())
    }

    @Test
    fun acceptLanguageInterceptorSendsKyrgyzTag() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse())
            val client = OkHttpClient.Builder()
                .addInterceptor(AcceptLanguageInterceptor { "ky-KG" })
                .build()

            client.newCall(Request.Builder().url(server.url("/health")).build()).execute().close()

            assertEquals("ky", server.takeRequest().getHeader("Accept-Language"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun acceptLanguageInterceptorDefaultsToRussian() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse())
            val client = OkHttpClient.Builder()
                .addInterceptor(AcceptLanguageInterceptor { "" })
                .build()

            client.newCall(Request.Builder().url(server.url("/health")).build()).execute().close()

            assertEquals("ru", server.takeRequest().getHeader("Accept-Language"))
        } finally {
            server.shutdown()
        }
    }
}