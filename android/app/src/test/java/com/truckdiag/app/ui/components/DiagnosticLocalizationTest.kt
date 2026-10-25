package com.truckdiag.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticLocalizationTest {
    @Test
    fun localizesImplementedSpnNamesAndDescriptions() {
        assertEquals("Частота вращения двигателя", LocalizedSpnName(190, "Engine Speed"))
        assertEquals("Давление моторного масла", LocalizedSpnName(100, "Engine Oil Pressure"))
        assertEquals(
            "Фактическая частота вращения коленчатого вала двигателя.",
            LocalizedSpnDescription(190, "Actual engine speed")
        )
    }

    @Test
    fun localizesKnownSourceAddressesAndUsesRussianFallback() {
        assertEquals("Двигатель №1 (EMS)", LocalizedSourceName(0x00))
        assertEquals("Коробка передач (GMS)", LocalizedSourceName(0x03))
        assertEquals("Блок управления 0x42", LocalizedSourceName(0x42))
    }
}