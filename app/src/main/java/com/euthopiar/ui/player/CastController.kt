package com.euthopiar.ui.player

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class CastDevice(
    val id: String,
    val name: String,
    val location: String,
    val ipAddress: String,
    val deviceType: String = "DLNA / Smart TV"
)

object CastController {

    private val _discoveredDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<CastDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeCastDevice = MutableStateFlow<CastDevice?>(null)
    val activeCastDevice: StateFlow<CastDevice?> = _activeCastDevice.asStateFlow()

    /**
     * Scans local Wi-Fi network using SSDP (UPnP / DLNA Discovery).
     */
    suspend fun scanDevices(context: Context) = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("EuthopiarCastLock")
        multicastLock?.acquire()

        val found = mutableListOf<CastDevice>()

        try {
            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"

            val group = InetAddress.getByName("239.255.255.250")
            val socket = DatagramSocket()
            socket.soTimeout = 2500

            val sendPacket = DatagramPacket(ssdpQuery.toByteArray(), ssdpQuery.length, group, 1900)
            socket.send(sendPacket)

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 3000) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = receivePacket.address.hostAddress ?: ""

                    // Extract LOCATION header
                    val location = response.lines()
                        .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim() ?: "http://$ip"

                    // Extract Server / USN
                    val serverName = response.lines()
                        .firstOrNull { it.startsWith("SERVER:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim() ?: "Smart TV / Media Player ($ip)"

                    val dev = CastDevice(
                        id = ip,
                        name = serverName,
                        location = location,
                        ipAddress = ip
                    )

                    if (found.none { it.ipAddress == ip }) {
                        found.add(dev)
                        _discoveredDevices.value = found.toList()
                    }
                } catch (_: SocketTimeoutException) {
                    break
                } catch (_: Exception) {}
            }
            socket.close()
        } catch (_: Exception) {
        } finally {
            try {
                if (multicastLock?.isHeld == true) multicastLock.release()
            } catch (_: Exception) {}
            _isScanning.value = false
        }
    }

    fun selectDevice(device: CastDevice?) {
        _activeCastDevice.value = device
    }

    fun disconnect() {
        _activeCastDevice.value = null
    }
}
