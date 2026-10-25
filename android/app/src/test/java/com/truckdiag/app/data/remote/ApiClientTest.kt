package com.truckdiag.app.data.remote

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

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
}