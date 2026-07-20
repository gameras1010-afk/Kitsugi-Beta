package com.kitsugi.animelist.core.player

import com.kitsugi.animelist.core.player.DolbyVisionBaseLayerPolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DolbyVisionBaseLayerPolicyTest {

    private fun resolve(
        hdrCapsKnown: Boolean = true,
        displayDv: Boolean = false,
        displayHdr10: Boolean = false,
        displayHdr10Plus: Boolean = false,
        displayHlg: Boolean = false,
        codecSupportsDvheDtb: Boolean = false,
        codecSupportsDvheStn: Boolean = false,
        codecSupportsDvheSt: Boolean = false,
        isAmazonFireTv: Boolean = false,
        isSamsung: Boolean = false,
        isXiaomi: Boolean = false,
        bridgeReady: Boolean = false,
        apiLevel: Int = 30
    ) = DolbyVisionBaseLayerPolicy.resolveFromCapabilities(
        hdrCapsKnown = hdrCapsKnown,
        displayDv = displayDv,
        displayHdr10 = displayHdr10,
        displayHdr10Plus = displayHdr10Plus,
        displayHlg = displayHlg,
        codecSupportsDvheDtb = codecSupportsDvheDtb,
        codecSupportsDvheStn = codecSupportsDvheStn,
        codecSupportsDvheSt = codecSupportsDvheSt,
        isAmazonFireTv = isAmazonFireTv,
        isSamsung = isSamsung,
        isXiaomi = isXiaomi,
        bridgeReady = bridgeReady,
        apiLevel = apiLevel
    )

    @Test
    fun `unknown caps returns STRIP_BEST_EFFORT regardless of other flags`() {
        val r = resolve(
            hdrCapsKnown = false,
            displayDv = true, displayHdr10 = true,
            codecSupportsDvheDtb = true, codecSupportsDvheSt = true,
            isAmazonFireTv = true, bridgeReady = true
        )
        assertEquals(Decision.STRIP_BEST_EFFORT, r.decision)
        assertTrue(r.divertsFromNativeDv7)
        assertTrue(r.mapToHevc)
    }

    @Test
    fun `DV display with native DvheDtb decoder returns NATIVE_DV7`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheDtb = true, bridgeReady = true)
        assertEquals(Decision.NATIVE_DV7, r.decision)
        assertFalse(r.divertsFromNativeDv7)
        assertFalse(r.mapToHevc)
    }

    @Test
    fun `Amazon device with native DvheDtb prefers NATIVE_DV7 over conversion`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheDtb = true, codecSupportsDvheSt = true, isAmazonFireTv = true, bridgeReady = true)
        assertEquals(Decision.NATIVE_DV7, r.decision)
    }

    @Test
    fun `Xiaomi device with native DvheDtb prefers NATIVE_DV7 over conversion`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheDtb = true, isXiaomi = true, bridgeReady = true)
        assertEquals(Decision.NATIVE_DV7, r.decision)
    }

    @Test
    fun `Fire TV on DV display with DV81 decoder and bridge converts`() {
        val r = resolve(displayDv = true, displayHdr10 = true, displayHdr10Plus = true, codecSupportsDvheSt = true, isAmazonFireTv = true, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
        assertTrue(r.divertsFromNativeDv7)
        assertFalse(r.mapToHevc)
    }

    @Test
    fun `Fire TV on DV display without bridge falls through to NATIVE_DV7`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheSt = true, isAmazonFireTv = true, bridgeReady = false)
        assertEquals(Decision.NATIVE_DV7, r.decision)
    }

    @Test
    fun `Xiaomi on DV display with bridge converts even without DvheSt`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheSt = false, isXiaomi = true, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
        assertTrue(r.divertsFromNativeDv7)
        assertFalse(r.mapToHevc)
    }

    @Test
    fun `Xiaomi on DV display without bridge falls through to NATIVE_DV7`() {
        val r = resolve(displayDv = true, displayHdr10 = true, isXiaomi = true, bridgeReady = false)
        assertEquals(Decision.NATIVE_DV7, r.decision)
    }

    @Test
    fun `Xiaomi on HDR10 display with bridge converts`() {
        val r = resolve(displayDv = false, displayHdr10 = true, isXiaomi = true, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
    }

    @Test
    fun `Xiaomi on HDR10 display without bridge strips to HDR10`() {
        val r = resolve(displayDv = false, displayHdr10 = true, isXiaomi = true, bridgeReady = false)
        assertEquals(Decision.STRIP_TO_HDR10, r.decision)
    }

    @Test
    fun `non-Amazon device on DV display with DV81 decoder converts to DV81`() {
        val r = resolve(displayDv = true, displayHdr10 = true, codecSupportsDvheSt = true, isAmazonFireTv = false, isSamsung = false, isXiaomi = false, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
        assertTrue(r.divertsFromNativeDv7)
        assertFalse(r.mapToHevc)
    }

    @Test
    fun `Samsung HDR10 panel with DV81 decoder and bridge converts`() {
        val r = resolve(displayDv = false, displayHdr10 = true, codecSupportsDvheSt = true, isSamsung = true, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
        assertTrue(r.divertsFromNativeDv7)
        assertFalse(r.mapToHevc)
    }

    @Test
    fun `Samsung HDR10 panel without bridge strips to HDR10`() {
        val r = resolve(displayHdr10 = true, codecSupportsDvheSt = true, isSamsung = true, bridgeReady = false)
        assertEquals(Decision.STRIP_TO_HDR10, r.decision)
    }

    @Test
    fun `Fire TV on HDR10 panel with DV81 decoder and bridge converts`() {
        val r = resolve(displayDv = false, displayHdr10 = true, codecSupportsDvheSt = true, isAmazonFireTv = true, bridgeReady = true)
        assertEquals(Decision.CONVERT_TO_DV81, r.decision)
    }

    @Test
    fun `Generic HDR10 panel strips when non-Samsung non-Amazon non-Xiaomi`() {
        val r = resolve(displayDv = false, displayHdr10 = true, codecSupportsDvheSt = true, isAmazonFireTv = false, isSamsung = false, isXiaomi = false, bridgeReady = true)
        assertEquals(Decision.STRIP_TO_HDR10, r.decision)
        assertTrue(r.mapToHevc)
    }

    @Test
    fun `HDR10 panel with nothing useful strips to HDR10`() {
        val r = resolve(displayHdr10 = true)
        assertEquals(Decision.STRIP_TO_HDR10, r.decision)
        assertTrue(r.mapToHevc)
    }

    @Test
    fun `SDR-only display returns STRIP_AND_TONEMAP`() {
        val r = resolve(codecSupportsDvheSt = true, bridgeReady = true)
        assertEquals(Decision.STRIP_AND_TONEMAP, r.decision)
        assertTrue(r.mapToHevc)
    }

    @Test
    fun `HLG-only display returns STRIP_AND_TONEMAP`() {
        val r = resolve(displayHlg = true, bridgeReady = true)
        assertEquals(Decision.STRIP_AND_TONEMAP, r.decision)
    }

    @Test
    fun `result preserves all input fields`() {
        val r = resolve(
            hdrCapsKnown = true, displayDv = true, displayHdr10 = true,
            displayHdr10Plus = true, displayHlg = false,
            codecSupportsDvheDtb = false, codecSupportsDvheStn = true,
            codecSupportsDvheSt = true, isAmazonFireTv = true,
            isSamsung = false, isXiaomi = false, bridgeReady = true, apiLevel = 33
        )
        assertTrue(r.hdrCapsKnown); assertTrue(r.displayDv); assertTrue(r.displayHdr10)
        assertTrue(r.displayHdr10Plus); assertFalse(r.displayHlg)
        assertFalse(r.codecSupportsDvheDtb); assertTrue(r.codecSupportsDvheStn)
        assertTrue(r.codecSupportsDvheSt); assertTrue(r.isAmazonFireTv)
        assertFalse(r.isSamsung); assertFalse(r.isXiaomi); assertTrue(r.bridgeReady)
        assertEquals(33, r.apiLevel)
    }
}
