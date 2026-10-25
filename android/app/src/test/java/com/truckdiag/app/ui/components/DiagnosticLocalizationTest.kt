package com.truckdiag.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import com.truckdiag.app.R

class DiagnosticLocalizationTest {
    @Test
    fun localizesImplementedSpnNamesAndDescriptions() {
        assertEquals(R.string.spn_190_name, spnNameResourceId(190))
        assertEquals(R.string.spn_100_name, spnNameResourceId(100))
        assertEquals(R.string.spn_190_description, spnDescriptionResourceId(190))
    }

    @Test
    fun localizesKnownSourceAddressesAndUsesRussianFallback() {
        assertEquals(R.string.source_engine_1, sourceNameResourceId(0x00))
        assertEquals(R.string.source_transmission, sourceNameResourceId(0x03))
        assertEquals(R.string.source_unknown, sourceNameResourceId(0x42))
    }
}