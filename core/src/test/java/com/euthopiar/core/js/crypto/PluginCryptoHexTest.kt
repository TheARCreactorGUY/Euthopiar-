package com.euthopiar.core.js.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginCryptoHexTest {

    @Test
    fun testPluginUtf8ToHexAndHexToUtf8Roundtrip() {
        val original = "Hello, QuickJS polyfill 2026! 🚀"
        val hex = pluginUtf8ToHex(original)
        val decoded = pluginHexToUtf8(hex)

        assertEquals(original, decoded)
    }

    @Test
    fun testEmptyStringHexConversion() {
        val hex = pluginUtf8ToHex("")
        assertEquals("", hex)
        assertEquals("", pluginHexToUtf8(""))
    }

    @Test
    fun testKnownHexRepresentation() {
        val text = "abc"
        // 'a' = 0x61, 'b' = 0x62, 'c' = 0x63
        assertEquals("616263", pluginUtf8ToHex(text))
        assertEquals("abc", pluginHexToUtf8("616263"))
    }
}
