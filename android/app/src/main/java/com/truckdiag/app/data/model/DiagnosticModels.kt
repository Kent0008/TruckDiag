package com.truckdiag.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthStatus(
    val status: String,
    val timestamp: Double,
    val database: String,
    val simulator: String,
    val version: String
)

@JsonClass(generateAdapter = true)
data class SystemInfo(
    @Json(name = "project_name") val projectName: String,
    @Json(name = "api_version") val apiVersion: String,
    @Json(name = "protocol_version") val protocolVersion: String,
    @Json(name = "supported_protocols") val supportedProtocols: List<String>,
    @Json(name = "available_adapters") val availableAdapters: List<String>,
    @Json(name = "supported_truck_platforms") val supportedTruckPlatforms: List<String>,
    val environment: String
)

@JsonClass(generateAdapter = true)
data class Truck(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val vin: String,
    @Json(name = "engine_code") val engineCode: String,
    @Json(name = "transmission_type") val transmissionType: String,
    @Json(name = "ecu_list") val ecuList: List<String>,
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SessionCreateDto(
    @Json(name = "truck_id") val truckId: String,
    @Json(name = "operator_name") val operatorName: String,
    val notes: String = "",
    @Json(name = "connection_mode") val connectionMode: String = "SIMULATOR"
)

@JsonClass(generateAdapter = true)
data class DiagnosticSession(
    val id: String,
    @Json(name = "truck_id") val truckId: String,
    @Json(name = "operator_name") val operatorName: String,
    val notes: String = "",
    @Json(name = "connection_mode") val connectionMode: String,
    val status: String, // CREATED, RUNNING, STOPPED
    @Json(name = "start_time") val startTime: String? = null,
    @Json(name = "stop_time") val stopTime: String? = null,
    @Json(name = "created_at") val createdAt: String,
    val truck: Truck? = null
)

@JsonClass(generateAdapter = true)
data class SessionStatus(
    @Json(name = "session_id") val sessionId: String,
    val status: String,
    @Json(name = "connection_state") val connectionState: String,
    @Json(name = "data_source") val dataSource: String,
    @Json(name = "uptime_seconds") val uptimeSeconds: Double,
    @Json(name = "active_ecus") val activeEcus: Int,
    @Json(name = "frames_per_second") val framesPerSecond: Double,
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TelemetryParameter(
    val name: String,
    @Json(name = "name_ru") val nameRu: String = "",
    val value: Double,
    val unit: String,
    val quality: String = "GOOD",
    val category: String = "ENGINE",
    @Json(name = "description_ru") val descriptionRu: String = "",
    @Json(name = "normal_range") val normalRange: String = "",
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TelemetryMessage(
    val timestamp: Double,
    @Json(name = "session_id") val sessionId: String,
    val source: String = "SIMULATED_J1939",
    val parameters: List<TelemetryParameter>,
    @Json(name = "engine_state") val engineState: String = "IDLE",
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Fault(
    val id: String,
    @Json(name = "session_id") val sessionId: String,
    val code: String,
    @Json(name = "source_ecu") val sourceEcu: String,
    val spn: Int,
    val fmi: Int,
    val oc: Int,
    val severity: String, // INFO, WARNING, CRITICAL
    val description: String,
    @Json(name = "possible_causes") val possibleCauses: String = "",
    val status: String, // ACTIVE, PREVIOUSLY_ACTIVE, PENDING
    val timestamp: Double,
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FaultScanResult(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "scanned_ecus") val scannedEcus: List<String>,
    @Json(name = "total_found") val totalFound: Int,
    val faults: List<Fault>,
    val simulated: Boolean = true,
    val disclaimer: String
)

@JsonClass(generateAdapter = true)
data class CanFrameDto(
    val id: String? = null,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "can_id") val canId: Long,
    @Json(name = "can_id_hex") val canIdHex: String,
    val priority: Int,
    val pgn: Int,
    @Json(name = "pgn_hex") val pgnHex: String,
    @Json(name = "pgn_name") val pgnName: String,
    @Json(name = "source_address") val sourceAddress: Int,
    @Json(name = "source_name") val sourceName: String,
    @Json(name = "destination_address") val destinationAddress: Int? = 255,
    val dlc: Int,
    @Json(name = "payload_hex") val payloadHex: String,
    val timestamp: Double,
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CanDecodeRequestDto(
    @Json(name = "can_id") val canId: String,
    val payload: String
)

@JsonClass(generateAdapter = true)
data class DecodedSpnDto(
    val spn: Int,
    val name: String,
    @Json(name = "raw_value") val rawValue: Any?,
    @Json(name = "scaled_value") val scaledValue: Any?,
    val unit: String,
    val description: String,
    val fidelity: String,
    @Json(name = "is_valid") val isValid: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CanDecodeResponseDto(
    @Json(name = "can_id") val canId: Long,
    @Json(name = "can_id_hex") val canIdHex: String,
    val priority: Int,
    val pgn: Int,
    @Json(name = "pgn_hex") val pgnHex: String,
    @Json(name = "pgn_name") val pgnName: String,
    @Json(name = "source_address") val sourceAddress: Int,
    @Json(name = "source_name") val sourceName: String,
    @Json(name = "destination_address") val destinationAddress: Int?,
    val dlc: Int,
    @Json(name = "payload_hex") val payloadHex: String,
    val fidelity: String,
    @Json(name = "decoded_spns") val decodedSpns: List<DecodedSpnDto>,
    val simulated: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReportCreateDto(
    @Json(name = "session_id") val sessionId: String,
    val title: String = "Diagnostic Scan Report",
    @Json(name = "technician_notes") val technicianNotes: String = ""
)

@JsonClass(generateAdapter = true)
data class DiagnosticReport(
    val id: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "created_at") val createdAt: String,
    val title: String,
    val summary: String,
    @Json(name = "truck_model") val truckModel: String,
    @Json(name = "truck_vin") val truckVin: String,
    @Json(name = "operator_name") val operatorName: String,
    @Json(name = "total_faults") val totalFaults: Int,
    @Json(name = "active_faults") val activeFaults: Int
)

@JsonClass(generateAdapter = true)
data class ReportDetail(
    val id: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "created_at") val createdAt: String,
    val title: String,
    val summary: String,
    @Json(name = "truck_model") val truckModel: String,
    @Json(name = "truck_vin") val truckVin: String,
    @Json(name = "operator_name") val operatorName: String,
    @Json(name = "total_faults") val totalFaults: Int,
    @Json(name = "active_faults") val activeFaults: Int,
    @Json(name = "report_data") val reportData: Map<String, Any>
)
