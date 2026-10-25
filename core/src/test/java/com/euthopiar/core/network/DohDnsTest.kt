package com.euthopiar.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DohDnsTest {

    private val dohDns = DohDns()

    @Test
    fun testResolveDomains() {
        val googleIps = dohDns.lookup("dns.google")
        println("Resolved dns.google: $googleIps")
        assertTrue("Should resolve dns.google", googleIps.isNotEmpty())

        val xvideosIps = dohDns.lookup("www.xvideos2.com")
        println("Resolved www.xvideos2.com: $xvideosIps")
        assertTrue("Should resolve www.xvideos2.com", xvideosIps.isNotEmpty())

        val epornerIps = dohDns.lookup("www.eporner.com")
        println("Resolved www.eporner.com: $epornerIps")
        assertTrue("Should resolve www.eporner.com", epornerIps.isNotEmpty())
    }

    @Test
    fun testDoHOkHttpClient() {
        val client = DohDns.createOkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("https://www.xvideos2.com/")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            println("xvideos2 response code: ${response.code}")
            assertTrue("Should connect to xvideos2 successfully", response.isSuccessful)
        }
    }

    @Test
    fun testLoopbackAddressesBypassDoh() {
        val localhostIps = dohDns.lookup("localhost")
        assertTrue("localhost must return loopback", localhostIps.any { it.isLoopbackAddress })

        val v4Loopback = dohDns.lookup("127.0.0.1")
        org.junit.Assert.assertEquals(1, v4Loopback.size)
        org.junit.Assert.assertEquals("127.0.0.1", v4Loopback[0].hostAddress)

        val v6Loopback = dohDns.lookup("::1")
        org.junit.Assert.assertEquals(1, v6Loopback.size)
        assertTrue("::1 must be loopback", v6Loopback[0].isLoopbackAddress)

        val v6Bracketed = dohDns.lookup("[::1]")
        org.junit.Assert.assertEquals(1, v6Bracketed.size)
        assertTrue("[::1] must be loopback", v6Bracketed[0].isLoopbackAddress)
    }

    @Test
    fun testBootstrapIpMapping() {
        val cfPrimary = dohDns.lookup("1.1.1.1")
        org.junit.Assert.assertEquals("1.1.1.1", cfPrimary[0].hostAddress)

        val cfSecondary = dohDns.lookup("1.0.0.1")
        org.junit.Assert.assertEquals("1.0.0.1", cfSecondary[0].hostAddress)

        val googlePrimary = dohDns.lookup("8.8.8.8")
        org.junit.Assert.assertEquals("8.8.8.8", googlePrimary[0].hostAddress)

        val googleSecondary = dohDns.lookup("8.8.4.4")
        org.junit.Assert.assertEquals("8.8.4.4", googleSecondary[0].hostAddress)

        val cfHost = dohDns.lookup("cloudflare-dns.com")
        org.junit.Assert.assertEquals("1.1.1.1", cfHost[0].hostAddress)

        val googleHost = dohDns.lookup("dns.google")
        org.junit.Assert.assertEquals("8.8.8.8", googleHost[0].hostAddress)
    }
}
