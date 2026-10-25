package com.truckdiag.app.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebSocketClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: WebSocketClient
    private lateinit var receivedCommand: CompletableDeferred<String>

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = WebSocketClient()
        receivedCommand = CompletableDeferred()
        ApiClient.setBaseUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        client.disconnect()
        ApiClient.setBaseUrl(ApiClient.DEFAULT_BASE_URL)
        server.shutdown()
    }

    @Test
    fun connectsReceivesTelemetryAndSendsFaultCommand() = runBlocking {
        val telemetryJson = """{"timestamp":1.0,"session_id":"session-1","source":"SIMULATED_J1939","parameters":[],"engine_state":"IDLE","simulated":true}"""
        val expectedCommand = """{"action":"inject_fault","fault_key":"COOLANT_OVERHEAT"}"""
        server.enqueue(
            MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        webSocket.send(telemetryJson)
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                        receivedCommand.complete(text)
                    }
                }
            )
        )

        val telemetry = async { withTimeout(5_000) { client.telemetryFlow.first() } }
        val connected = async { withTimeout(5_000) { client.connectionState.first { it } } }
        client.connect("session-1")

        assertTrue(connected.await())
        assertEquals("session-1", telemetry.await().sessionId)
        assertEquals("/api/v1/telemetry/ws/session-1", server.takeRequest().path)

        client.sendCommand("inject_fault", "fault_key", "COOLANT_OVERHEAT")
        assertEquals(expectedCommand, withTimeout(5_000) { receivedCommand.await() })
    }
}