package com.kitsugi.animelist.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

/**
 * T1.12: DohProviders
 *
 * DNS over HTTPS provider'ları. CloudStream referansından adapte edildi.
 * NuvioOkHttpProvider'a entegre edilebilir, AppSettings.dnsChoice ile yönetilir.
 *
 * Değerler:
 * 0 = Sistem DNS (varsayılan)
 * 1 = Cloudflare (1.1.1.1)
 * 2 = Google (8.8.8.8)
 * 3 = AdGuard
 * 4 = Quad9
 */
object DohProviders {

    private val bootstrapClient by lazy {
        OkHttpClient.Builder().build()
    }

    /** Cloudflare 1.1.1.1 DoH */
    fun cloudflare(): Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1")
        )
        .includeIPv6(false)
        .build()

    /** Google 8.8.8.8 DoH */
    fun google(): Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4")
        )
        .includeIPv6(false)
        .build()

    /** AdGuard DoH */
    fun adGuard(): Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.adguard-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("94.140.14.14"),
            InetAddress.getByName("94.140.15.15")
        )
        .includeIPv6(false)
        .build()

    /** Quad9 (malware engelleyen) DoH */
    fun quad9(): Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.quad9.net/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("149.112.112.112")
        )
        .includeIPv6(false)
        .build()

    /**
     * AppSettings.dnsChoice değerine göre uygun DNS provider'ı döndürür.
     * @param choice 0=System, 1=Cloudflare, 2=Google, 3=AdGuard, 4=Quad9
     */
    fun fromChoice(choice: Int): Dns? = when (choice) {
        1 -> cloudflare()
        2 -> google()
        3 -> adGuard()
        4 -> quad9()
        else -> null // Sistem DNS
    }

    /** choice integer → kullanıcı dostu display ismi */
    fun displayName(choice: Int): String = when (choice) {
        0 -> "Sistem DNS"
        1 -> "Cloudflare (1.1.1.1)"
        2 -> "Google (8.8.8.8)"
        3 -> "AdGuard"
        4 -> "Quad9"
        else -> "Bilinmiyor"
    }

    val choices: List<Pair<Int, String>> = listOf(
        0 to "Sistem DNS",
        1 to "Cloudflare (1.1.1.1)",
        2 to "Google (8.8.8.8)",
        3 to "AdGuard",
        4 to "Quad9"
    )
}
