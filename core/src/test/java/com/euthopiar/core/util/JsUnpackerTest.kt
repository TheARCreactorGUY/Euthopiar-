package com.euthopiar.core.util

import org.junit.Assert.*
import org.junit.Test

class JsUnpackerTest {

    @Test
    fun testIsPackedWithPackedScript() {
        val sample = "eval(function(p,a,c,k,e,d){...}('0 1=2;',3,3,'var|x|10'.split('|')))"
        assertTrue("Should detect packed script", JsUnpacker.isPacked(sample))
    }

    @Test
    fun testIsPackedWithNormalScript() {
        val normal = "var x = 10; console.log(x);"
        assertFalse("Should return false for normal script", JsUnpacker.isPacked(normal))
    }

    @Test
    fun testUnpackBasicString() {
        val packed = "eval(function(p,a,c,k,e,d){...}('0 1=2;',3,3,'var|x|10'.split('|'))"
        val unpacked = JsUnpacker.unpack(packed)
        assertNotNull("Unpacked script should not be null", unpacked)
        assertEquals("var x=10;", unpacked)
    }

    @Test
    fun testUnpackWithBase62Radix() {
        val packed = "eval(function(p,a,c,k,e,d){...}('0 1 = \"2\";',62,3,'let|greeting|Hello World'.split('|'))"
        val unpacked = JsUnpacker.unpack(packed)
        assertNotNull(unpacked)
        assertEquals("let greeting = \"Hello World\";", unpacked)
    }

    @Test
    fun testUnpackEmbeddedInHtml() {
        val html = """
            <html>
            <head><title>Player</title></head>
            <body>
            <script>
            eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c]);return p;}('0 1=2;',3,3,'var|videoUrl|https://cdn.example.com/master.m3u8'.split('|')))
            </script>
            </body>
            </html>
        """.trimIndent()

        assertTrue(JsUnpacker.isPacked(html))
        val unpacked = JsUnpacker.unpack(html)
        assertNotNull(unpacked)
        assertTrue(unpacked!!.contains("var videoUrl=https://cdn.example.com/master.m3u8;"))
    }

    @Test
    fun testUnpackInvalidOrMalformedReturnsNull() {
        val invalid = "eval(function(invalid){"
        assertNull("Invalid format should return null", JsUnpacker.unpack(invalid))
    }
}
