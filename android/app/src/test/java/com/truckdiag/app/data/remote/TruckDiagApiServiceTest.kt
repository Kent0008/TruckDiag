package com.truckdiag.app.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
    import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
    import retrofit2.HttpException
import retrofit2.converter.moshi.MoshiConverterFactory

class TruckDiagApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TruckDiagApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TruckDiagApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun systemEndpointsDecodeHealthInfoAndTruckList() = runBlocking {
        enqueue("""{"status":"ok","timestamp":1.0,"database":"connected","simulator":"ready","version":"1.0.0"}""")
        enqueue("""{"project_name":"TruckDiag API","api_version":"1.0.0","protocol_version":"SAE J1939-2023","supported_protocols":["SAE J1939"],"available_adapters":["SIMULATOR_VIRTUAL_BUS"],"supported_truck_platforms":["Scania R500"],"environment":"local_development"}""")
        enqueue("""[{"id":"truck-1","brand":"Scania","model":"R500","year":2021,"vin":"VIN-1","engine_code":"DC13","transmission_type":"Opticruise","ecu_list":["EMS"],"simulated":true}]""")
        enqueue(truckResponse())

        assertEquals("ok", api.getHealth().status)
        assertEquals("SAE J1939-2023", api.getSystemInfo().protocolVersion)
        assertEquals("Scania", api.getTrucks().single().brand)
            assertEquals("VIN-1", api.getTruck("truck-1").vin)
        assertEquals("/api/v1/health", server.takeRequest().path)
        assertEquals("/api/v1/system/info", server.takeRequest().path)
        assertEquals("/api/v1/trucks", server.takeRequest().path)
    }

    @Test
    fun sessionLifecycleUsesExpectedRoutesAndJsonNames() = runBlocking {
        enqueue(sessionResponse("CREATED"))
        enqueue("[${sessionResponse("CREATED")}]" )
        enqueue(sessionResponse("CREATED"))
        enqueue(sessionResponse("RUNNING"))
        enqueue(sessionResponse("STOPPED"))
        enqueue("""{"session_id":"session-1","status":"RUNNING","connection_state":"CONNECTED","data_source":"SIMULATED_J1939","uptime_seconds":4.0,"active_ecus":6,"frames_per_second":10.0,"simulated":true}""")

        api.createSession(
            com.truckdiag.app.data.model.SessionCreateDto(
                truckId = "truck-1",
                operatorName = "Диагност",
                notes = "Проверка",
            )
        )
        assertEquals("session-1", api.getSessions().single().id)
        assertEquals("session-1", api.getSession("session-1").id)
        api.startSession("session-1")
        api.stopSession("session-1")
        assertEquals("CONNECTED", api.getSessionStatus("session-1").connectionState)

        val createRequest = server.takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("/api/v1/sessions", createRequest.path)
        val createBody = createRequest.body.readUtf8()
        assertTrue(createBody.contains("\"truck_id\":\"truck-1\""))
        assertTrue(createBody.contains("\"operator_name\":\"Диагност\""))
        assertEquals("/api/v1/sessions?limit=50", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1/start", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1/stop", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1/status", server.takeRequest().path)
    }

    @Test
    fun telemetryMapsRussianFieldsAndQueryParameters() = runBlocking {
        enqueue("""{"timestamp":1.0,"session_id":"session-1","source":"SIMULATED_J1939","engine_state":"IDLE","simulated":true,"parameters":[{"name":"Engine Speed","name_ru":"Частота вращения двигателя","value":600.0,"unit":"об/мин","quality":"GOOD","category":"ENGINE","description_ru":"Обороты двигателя","normal_range":"500–650 об/мин","simulated":true}]}""")

        val message = api.getLatestTelemetry("session-1")

        assertEquals("Частота вращения двигателя", message.parameters.single().nameRu)
        assertEquals("об/мин", message.parameters.single().unit)
        assertEquals("/api/v1/telemetry/latest?session_id=session-1", server.takeRequest().path)
    }

    @Test
    fun faultEndpointsDecodeScanResultsAndUseSessionRoutes() = runBlocking {
        val fault = """{"id":"fault-1","session_id":"session-1","code":"SPN 110 FMI 0","source_ecu":"EMS","spn":110,"fmi":0,"oc":1,"severity":"CRITICAL","description":"Перегрев","possible_causes":"Нагрузка","status":"ACTIVE","timestamp":1.0,"simulated":true}"""
        enqueue("[$fault]")
        enqueue("[$fault]")
        enqueue("[$fault]")
        enqueue("""{"session_id":"session-1","scanned_ecus":["EMS"],"total_found":1,"faults":[$fault],"simulated":true,"disclaimer":"Тестовая симуляция"}""")

        assertEquals(1, api.getFaults().size)
        assertEquals(1, api.getFaults("session-1").size)
        assertEquals("SPN 110 FMI 0", api.getSessionFaults("session-1").single().code)
        val scan = api.scanFaults("session-1")

        assertEquals(1, scan.totalFound)
        assertEquals(110, scan.faults.single().spn)
        assertEquals("/api/v1/faults", server.takeRequest().path)
        assertEquals("/api/v1/faults?session_id=session-1", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1/faults", server.takeRequest().path)
        assertEquals("/api/v1/sessions/session-1/faults/scan", server.takeRequest().path)
    }

    @Test
    fun canEndpointsDecodeFramesAndSubmitHexPayload() = runBlocking {
        enqueue("""[{"id":"frame-1","session_id":"session-1","can_id":217056256,"can_id_hex":"0x0CF00400","priority":3,"pgn":61444,"pgn_hex":"0x00F004","pgn_name":"EEC1","source_address":0,"source_name":"EMS","destination_address":255,"dlc":8,"payload_hex":"F07D0020FFFFFFFF","timestamp":1.0,"simulated":true}]""")
        enqueue("""{"can_id":217056256,"can_id_hex":"0x0CF00400","priority":3,"pgn":61444,"pgn_hex":"0x00F004","pgn_name":"EEC1","source_address":0,"source_name":"EMS","destination_address":255,"dlc":8,"payload_hex":"F07D0020FFFFFFFF","fidelity":"VERIFIED_SAE_J1939_STANDARD","decoded_spns":[{"spn":190,"name":"Engine Speed","raw_value":8192,"scaled_value":1024.0,"unit":"RPM","description":"Engine speed","fidelity":"VERIFIED_SAE_J1939_STANDARD","is_valid":true}],"simulated":true}""")

        assertEquals(61444, api.getCanFrames("session-1", 10).single().pgn)
        val decoded = api.decodeCanFrame(
            com.truckdiag.app.data.model.CanDecodeRequestDto("0x0CF00400", "F07D0020FFFFFFFF")
        )

        assertEquals(1024.0, decoded.decodedSpns.single().scaledValue)
        assertEquals("/api/v1/can/frames?session_id=session-1&limit=10", server.takeRequest().path)
        val decodeRequest = server.takeRequest()
        assertEquals("POST", decodeRequest.method)
        assertEquals("/api/v1/can/decode", decodeRequest.path)
        assertTrue(decodeRequest.body.readUtf8().contains("\"payload\":\"F07D0020FFFFFFFF\""))
    }

    @Test
    fun reportEndpointsCreateListAndReadDetails() = runBlocking {
        enqueue(reportResponse())
        enqueue("[${reportResponse()}]")
        enqueue("""{"id":"report-1","session_id":"session-1","created_at":"2026-10-25T10:00:00Z","title":"Отчёт","summary":"Проверка","truck_model":"Scania R500","truck_vin":"VIN-1","operator_name":"Диагност","total_faults":1,"active_faults":1,"report_data":{"disclaimer":"Демо"}}""")

        api.createReport(com.truckdiag.app.data.model.ReportCreateDto("session-1", "Отчёт", "Заметка"))
        assertEquals("report-1", api.getReports().single().id)
        assertEquals("Демо", api.getReportDetails("report-1").reportData["disclaimer"])

        val createRequest = server.takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("/api/v1/reports", createRequest.path)
        assertTrue(createRequest.body.readUtf8().contains("\"technician_notes\":\"Заметка\""))
        assertEquals("/api/v1/reports?limit=50", server.takeRequest().path)
        assertEquals("/api/v1/reports/report-1", server.takeRequest().path)
    }

    @Test
    fun nonSuccessfulResponsePreservesHttpStatus() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Service unavailable"))

        try {
            api.getHealth()
            fail("Expected an HTTP exception")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }
    }

    private fun enqueue(body: String) {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    private fun sessionResponse(status: String): String =
        """{"id":"session-1","truck_id":"truck-1","operator_name":"Диагност","notes":"","connection_mode":"SIMULATOR","status":"$status","created_at":"2026-10-25T10:00:00Z"}"""

    private fun truckResponse(): String =
        """{"id":"truck-1","brand":"Scania","model":"R500","year":2021,"vin":"VIN-1","engine_code":"DC13","transmission_type":"Opticruise","ecu_list":["EMS"],"simulated":true}"""

    private fun reportResponse(): String =
        """{"id":"report-1","session_id":"session-1","created_at":"2026-10-25T10:00:00Z","title":"Отчёт","summary":"Проверка","truck_model":"Scania R500","truck_vin":"VIN-1","operator_name":"Диагност","total_faults":1,"active_faults":1}"""
}