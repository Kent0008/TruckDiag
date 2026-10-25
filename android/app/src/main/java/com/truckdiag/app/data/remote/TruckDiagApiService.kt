package com.truckdiag.app.data.remote

import com.truckdiag.app.data.model.*
import retrofit2.http.*

interface TruckDiagApiService {

    @GET("api/v1/health")
    suspend fun getHealth(): HealthStatus

    @GET("api/v1/system/info")
    suspend fun getSystemInfo(): SystemInfo

    @GET("api/v1/trucks")
    suspend fun getTrucks(): List<Truck>

    @GET("api/v1/trucks/{id}")
    suspend fun getTruck(@Path("id") id: String): Truck

    @POST("api/v1/sessions")
    suspend fun createSession(@Body body: SessionCreateDto): DiagnosticSession

    @GET("api/v1/sessions")
    suspend fun getSessions(@Query("limit") limit: Int = 50): List<DiagnosticSession>

    @GET("api/v1/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): DiagnosticSession

    @POST("api/v1/sessions/{id}/start")
    suspend fun startSession(@Path("id") id: String): DiagnosticSession

    @POST("api/v1/sessions/{id}/stop")
    suspend fun stopSession(@Path("id") id: String): DiagnosticSession

    @GET("api/v1/sessions/{id}/status")
    suspend fun getSessionStatus(@Path("id") id: String): SessionStatus

    @GET("api/v1/telemetry/latest")
    suspend fun getLatestTelemetry(@Query("session_id") sessionId: String? = null): TelemetryMessage

    @GET("api/v1/faults")
    suspend fun getFaults(@Query("session_id") sessionId: String? = null): List<Fault>

    @GET("api/v1/sessions/{id}/faults")
    suspend fun getSessionFaults(@Path("id") sessionId: String): List<Fault>

    @POST("api/v1/sessions/{id}/faults/scan")
    suspend fun scanFaults(@Path("id") sessionId: String): FaultScanResult

    @GET("api/v1/can/frames")
    suspend fun getCanFrames(
        @Query("session_id") sessionId: String? = null,
        @Query("limit") limit: Int = 50
    ): List<CanFrameDto>

    @POST("api/v1/can/decode")
    suspend fun decodeCanFrame(@Body body: CanDecodeRequestDto): CanDecodeResponseDto

    @POST("api/v1/reports")
    suspend fun createReport(@Body body: ReportCreateDto): DiagnosticReport

    @GET("api/v1/reports")
    suspend fun getReports(@Query("limit") limit: Int = 50): List<DiagnosticReport>

    @GET("api/v1/reports/{id}")
    suspend fun getReportDetails(@Path("id") id: String): ReportDetail
}
