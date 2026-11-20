package com.kavesh.mc.util

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    /**
     * Returns the device's real local Wi-Fi/LAN IPv4 address (e.g., 192.168.1.9).
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            var fallback: String? = null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (intf.name.startsWith("wlan") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            return ip
                        }
                        fallback = ip
                    }
                }
            }
            return fallback ?: "127.0.0.1"
        } catch (_: Exception) {
            return "127.0.0.1"
        }
    }
}
