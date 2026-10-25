package com.truckdiag.app.data.remote

import android.util.Log
import com.truckdiag.app.data.model.TelemetryMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*

class WebSocketClient {
    private val tag = "TruckDiagWebSocket"
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetryFlow = MutableSharedFlow<TelemetryMessage>(replay = 1)
    val telemetryFlow: SharedFlow<TelemetryMessage> = _telemetryFlow.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    private val adapter = ApiClient.moshi.adapter(TelemetryMessage::class.java)

    fun connect(sessionId: String) {
        disconnect()

        val baseUrl = ApiClient.getBaseUrl()
        val wsUrl = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://")
        } else {
            baseUrl.replace("http://", "ws://")
        } + "api/v1/telemetry/ws/$sessionId"

        Log.d(tag, "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = ApiClient.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened")
                scope.launch { _connectionState.emit(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = adapter.fromJson(text)
                    if (msg != null) {
                        scope.launch { _telemetryFlow.emit(msg) }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse telemetry JSON: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $code / $reason")
                scope.launch { _connectionState.emit(false) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure: ${t.message}")
                scope.launch { _connectionState.emit(false) }
            }
        })
    }

    fun sendCommand(action: String, key: String, value: String) {
        val payload = """{"action":"$action","$key":"$value"}"""
        webSocket?.send(payload)
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (_: Exception) {}
        webSocket = null
        scope.launch { _connectionState.emit(false) }
    }
}
