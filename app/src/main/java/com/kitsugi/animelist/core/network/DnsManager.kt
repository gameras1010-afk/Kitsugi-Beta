package com.kitsugi.animelist.core.network

import android.content.Context
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages custom DNS-over-HTTPS (DoH) providers for the application.
 */
object DnsManager {
    // Choice representation:
    // 0: System Default, 1: Google DNS, 2: Cloudflare DNS, 3: AdGuard DNS, 4: DNS.WATCH, 5: Quad9, 6: DNS.SB, 7: Canadian Shield
    @Volatile
    var dnsChoice: Int = 0
        set(value) {
            field = value
            activeDns = getDnsForChoice(value)
        }

    private var context: Context? = null
    
    // Bootstrap client to resolve DNS over HTTPS URLs.
    // It should NOT use our DohDns to avoid infinite recursion/loops.
    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .dns(IPv4FirstDns(Dns.SYSTEM)) // prefer IPv4 to avoid broken IPv6 bootstrap
            .build()
    }

    private val dnsCache = ConcurrentHashMap<Int, Dns>()

    @Volatile
    private var activeDns: Dns = Dns.SYSTEM

    fun init(context: Context, initialChoice: Int) {
        this.context = context.applicationContext
        this.dnsChoice = initialChoice
    }

    fun getActiveDns(): Dns {
        return activeDns
    }

    private fun getDnsForChoice(choice: Int): Dns {
        if (choice == 0) return Dns.SYSTEM
        
        return dnsCache.getOrPut(choice) {
            createDnsForChoice(choice)
        }
    }

    private fun createDnsForChoice(choice: Int): Dns {
        val (url, ips) = when (choice) {
            1 -> "https://dns.google/dns-query" to listOf("8.8.4.4", "8.8.8.8")
            2 -> "https://cloudflare-dns.com/dns-query" to listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
            3 -> "https://dns.adguard.com/dns-query" to listOf("94.140.14.140", "94.140.14.141")
            4 -> "https://resolver2.dns.watch/dns-query" to listOf("84.200.69.80", "84.200.70.40")
            5 -> "https://dns.quad9.net/dns-query" to listOf("9.9.9.9", "149.112.112.112")
            6 -> "https://doh.dns.sb/dns-query" to listOf("185.222.222.222", "45.11.45.11")
            7 -> "https://private.canadianshield.cira.ca/dns-query" to listOf("149.112.121.10", "149.112.122.10")
            else -> return Dns.SYSTEM
        }

        return try {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(url.toHttpUrl())
                .bootstrapDnsHosts(ips.map { InetAddress.getByName(it) })
                .build()
        } catch (e: Exception) {
            android.util.Log.e("DnsManager", "Failed to build DNS over HTTPS client for choice $choice", e)
            Dns.SYSTEM
        }
    }
}
