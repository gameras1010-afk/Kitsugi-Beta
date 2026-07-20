package com.kitsugi.animelist.core.companion

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utility that resolves the device's active LAN IPv4 address.
 *
 * Iterates all available network interfaces and picks the first non-loopback,
 * site-local IPv4 address (e.g. 192.168.x.x or 10.x.x.x).
 * Returns null when the device has no active LAN connection.
 */
object DeviceIpAddress {

    /**
     * Returns the active LAN IPv4 address as a String (e.g. "192.168.1.42"),
     * or null if no suitable interface is found.
     */
    fun get(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { iface -> iface.isUp && !iface.isLoopback }
                ?.flatMap { iface -> iface.inetAddresses.asSequence() }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { addr -> addr.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
