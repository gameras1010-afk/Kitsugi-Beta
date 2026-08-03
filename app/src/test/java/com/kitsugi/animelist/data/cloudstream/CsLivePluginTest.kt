package com.kitsugi.animelist.data.cloudstream

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Kitsugi Turkish Cloudstream Providers Live Probing Diagnostic Suite.
 *
 * Dinamik olarak 7 Türk reposundan TÜM eklentileri çeker ve her birini probe eder:
 * - .cs3 dosyası indirilebilir mi? (HTTP HEAD)
 * - Provider'ın web sitesi ayakta mı?
 * - Cloudflare / WAF koruması var mı?
 *
 * Sonuç: plugin_diagnostic_report.md
 */
class CsLivePluginTest {

    // ── HTTP Client ────────────────────────────────────────────────────────────
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // ── Türk Cloudstream Depoları ─────────────────────────────────────────────
    private val REPOS = listOf(
        RepoEntry("Kitsugi Plugins (Önerilen)",
            "https://raw.githubusercontent.com/gameras1010-afk/Kitsugi-Plugins/builds/repo.json"),
        RepoEntry("feroxx / Kekik-cloudstream",
            "https://raw.githubusercontent.com/feroxx/Kekik-cloudstream/refs/heads/builds/repo.json"),
        RepoEntry("Kraptor123 / cs-kraptor",
            "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/refs/heads/master/repo.json"),
        RepoEntry("Kraptor123 / Cs-Karma",
            "https://raw.githubusercontent.com/Kraptor123/Cs-Karma/refs/heads/master/repo.json"),
        RepoEntry("nikyokki / nik-cloudstream",
            "https://raw.githubusercontent.com/nikyokki/nik-cloudstream/master/repo.json"),
        RepoEntry("ByAyzen / AyzenCS3",
            "https://raw.githubusercontent.com/ByAyzen/AyzenCS3/refs/heads/builds/repo.json"),
        RepoEntry("Kraptor123 / cs-kekikanime",
            "https://raw.githubusercontent.com/Kraptor123/cs-kekikanime/master/repo.json"),
        RepoEntry("sarapcanagii / Pitipitii",
            "https://raw.githubusercontent.com/sarapcanagii/Pitipitii/master/repo.json"),
        RepoEntry("Kraptor123 / Cs-GizliKeyif",
            "https://raw.githubusercontent.com/Kraptor123/Cs-GizliKeyif/refs/heads/master/repo.json"),
        RepoEntry("Sertel392 / Makotogecici",
            "https://raw.githubusercontent.com/Sertel392/Makotogecici/main/repo.json"),
        RepoEntry("caca1403 / cloudstream-cagi-eklenti",
            "https://raw.githubusercontent.com/caca1403/cloudstream-cagi-eklenti/main/repo.json")
    )

    // ── Bilinen Provider Domain Haritası ──────────────────────────────────────
    // İnternalName → provider'ın kendi web sitesi (mainUrl)
    private val KNOWN_DOMAINS = mapOf(
        "TurkAnime"          to "https://www.turkanime.co",
        "AnimeciX"           to "https://animecix.tv",
        "Animeler"           to "https://animeler.pw",
        "AsyaWatch"          to "https://asyawatch.com",
        "CizgiMax"           to "https://cizgimax.online",
        "AnimeElysium"       to "https://animeelysium.com",
        "TrAnimeci"          to "https://tranimaci.com",
        "TrAnimeIzle"        to "https://www.tranimeizle.io",
        "AnimPow"            to "https://animpow.com",
        "AsyaAnimeleri"      to "https://asyaanimeleri.top",
        "AsyaAnimeleri2"     to "https://asyaanimeleri.top",
        "Asyafanatiklerim"   to "https://asyafanatiklerim.com",
        "AsyaMinik"          to "https://asyaminik.com",
        "YoTurkAnime"        to "https://www.yoturkanime.com",
        "AnimeIzle"          to "https://www.animeizle.biz",
        "Anizium"            to "https://api.anizium.co",
        "Dizilla"            to "https://dizillahd.com",
        "DiziBox"            to "https://www.dizibox.live",
        "DiziPal"            to "https://dizipal.bid",
        "DiziPalOriginal"    to "https://dizipal.bid",
        "DiziPalOrijinal"    to "https://dizipal1572.com",
        "DDizi"              to "https://www.ddizi.im",
        "Ddizi"              to "https://www.ddizi.im",
        "DizifilmORG"        to "https://dizifilm.life",
        "SezonlukDizi"       to "https://sezonlukdizi.cc",
        "DiziMom"            to "https://www.dizimom.rest",
        "DiziYou"            to "https://www.diziyou.one",
        "DiziYo"             to "https://www.diziyo.so",
        "DiziPod"            to "https://dizipod.com",
        "DiziAsia"           to "https://diziasia.com",
        "DiziAsya"           to "https://diziasya.com",
        "DiziKorea"          to "https://dizikorea3.com",
        "DiziGecesi"         to "https://dizigecesi.com",
        "DiziLife"           to "https://dizi73.life",
        "TrDiziIzle"         to "https://www.trdiziizle.tv",
        "CizgiveDizi"        to "https://cizgivedizi.com",
        "HDFilmCehennemi"    to "https://www.hdfilmcehennemi.nl",
        "SinemaCX"           to "https://www.sinema.gg",
        "FilmMakinesi"       to "https://filmmakinesi.to",
        "FilmModu"           to "https://www.filmmodu.one",
        "FullHDFilm"         to "https://fullhdfilm.pro",
        "FullHDFilmizlesene" to "https://www.fullhdfilmizlesene.mx",
        "JetFilmizle"        to "https://jetfilmizle.now",
        "Jetfilmizle"        to "https://jetfilmizle.now",
        "WebteIzle"          to "https://webteizle3.xyz",
        "IzleAI"             to "https://720pizle.ai",
        "SetFilmIzle"        to "https://www.setfilmizle.uk",
        "FilmEkseni"         to "https://filmekseni.vip",
        "FilmHane"           to "https://www.filmhane.shop",
        "FilmZal"            to "https://filmzal.me",
        "Filmzal"            to "https://filmzal.me",
        "HDFilmDelisi"       to "https://hdfilmdelisi.one",
        "HDFilmizle"         to "https://www.hdfilmizle.vip",
        "WFilmizle"          to "https://www.wfilmizle.pw",
        "Sinezy"             to "https://sinezy.to",
        "SelcukFlix"         to "https://selcukflix.co",
        "SeiCode"            to "https://seiwatch.net",
        "RecTV"              to "https://b.prectv38.sbs",
        "KoreanTurk"         to "https://www.koreanturk.net",
        "KultFilmler"        to "https://kultfilmler.net",
        "BelgeselX"          to "https://belgeselx.com",
        "YabanciDizi"        to "https://yabancidizi.news",
        "YesilCamTv"         to "https://yesilcamtv.com.tr",
        "YTS"                to "https://yts.gg",
        "DiziMom"            to "https://www.dizimom.rest",
        "DiziPal"            to "https://dizipal.bid",
        "WebteIzle"          to "https://webteizle3.xyz",
    )

    // ── Data Classes ──────────────────────────────────────────────────────────
    data class RepoEntry(val name: String, val repoJsonUrl: String)

    data class PluginEntry(
        val repoName: String,
        val internalName: String,
        val displayName: String,
        val cs3Url: String,
        val tvTypes: List<String>,
        val status: Int,
        val providerUrl: String?   // web sitesi (KNOWN_DOMAINS'dan)
    )

    data class DiagResult(
        val repoName: String,
        val plugin: String,
        val displayName: String,
        val cs3Url: String,
        val cs3Status: Int,        // HTTP kodu (.cs3 indirme)
        val cs3Ok: Boolean,
        val siteUrl: String?,
        val siteStatus: Int,       // HTTP kodu (provider sitesi)
        val siteResolved: Boolean,
        val siteIp: String,
        val isCloudflare: Boolean,
        val isWafBlocked: Boolean,
        val redirectUrl: String?,
        val tvTypes: String,
        val remarks: String
    )

    // ── Part 2: Repo & Plugin Fetching ────────────────────────────────────────

    /** Tek bir repo.json URL'sinden tüm eklentileri çeker */
    private fun fetchPluginsFromRepo(repoEntry: RepoEntry): List<PluginEntry> {
        val plugins = mutableListOf<PluginEntry>()
        try {
            val repoJson = httpGet(repoEntry.repoJsonUrl) ?: run {
                println("  [REPO FETCH FAILED] ${repoEntry.repoJsonUrl}")
                return emptyList()
            }
            val repoObj = JSONObject(repoJson)
            val pluginListUrls = mutableListOf<String>()
            repoObj.optJSONArray("pluginLists")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i)
                    if (u.isNotBlank()) pluginListUrls.add(u)
                }
            }
            println("  [${repoEntry.name}] pluginLists: $pluginListUrls")

            for (listUrl in pluginListUrls) {
                val listJson = httpGet(listUrl) ?: continue
                val arr = JSONArray(listJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name    = obj.optString("name", "").trim()
                    val id      = obj.optString("internalName", name).trim()
                    val cs3Url  = obj.optString("url", "").trim()
                    val status  = obj.optInt("status", 1)
                    if (name.isBlank() || cs3Url.isBlank()) continue
                    if (status == 3) continue   // broken/deprecated, skip

                    val tvTypesArr = obj.optJSONArray("tvTypes")
                    val tvTypes = if (tvTypesArr != null)
                        (0 until tvTypesArr.length()).map { tvTypesArr.optString(it) }
                    else emptyList()

                    val providerUrl = KNOWN_DOMAINS[id] ?: KNOWN_DOMAINS[name]
                    plugins.add(PluginEntry(
                        repoName    = repoEntry.name,
                        internalName = id,
                        displayName  = name,
                        cs3Url       = cs3Url,
                        tvTypes      = tvTypes,
                        status       = status,
                        providerUrl  = providerUrl
                    ))
                }
            }
        } catch (e: Exception) {
            println("  [ERROR] fetchPluginsFromRepo ${repoEntry.name}: ${e.message}")
        }
        return plugins
    }

    /** Simple blocking GET, returns body string or null on error */
    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", ua).build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) res.body?.string() else null
            }
        } catch (e: Exception) { null }
    }

    // ── CS3 File Probe ────────────────────────────────────────────────────────
    /** .cs3 dosyasının indirilebilir olup olmadığını HTTP HEAD ile kontrol eder */
    private fun probeCs3(cs3Url: String): Int {
        return try {
            val req = Request.Builder()
                .url(cs3Url)
                .head()
                .header("User-Agent", "CloudStream/3")
                .build()
            client.newCall(req).execute().use { res -> res.code }
        } catch (e: Exception) { -1 }
    }

    // ── Provider Site Probe ───────────────────────────────────────────────────
    data class SiteProbeResult(
        val httpStatus: Int,
        val ipAddress: String,
        val resolved: Boolean,
        val isCloudflare: Boolean,
        val isWafBlocked: Boolean,
        val redirectUrl: String?,
        val remarks: String
    )

    private fun probeSite(url: String): SiteProbeResult {
        var ip = "N/A"
        var resolved = false
        try {
            val host = url.substringAfter("://").substringBefore("/").substringBefore(":")
            ip = InetAddress.getByName(host).hostAddress ?: "N/A"
            resolved = true
        } catch (_: Exception) {
            return SiteProbeResult(-1, ip, false, false, false, null, "DNS failed")
        }

        return try {
            val req = Request.Builder().url(url).header("User-Agent", ua).build()
            client.newCall(req).execute().use { res ->
                val code    = res.code
                val headers = res.headers.toString()
                val body    = res.body?.string() ?: ""
                val isCf    = res.header("CF-Ray") != null ||
                        res.header("Server")?.contains("cloudflare", ignoreCase = true) == true ||
                        body.contains("cf-browser-verification", ignoreCase = true)
                val isWaf   = code == 403 || code == 503 ||
                        body.contains("Access Denied", ignoreCase = true) ||
                        body.contains("Security check", ignoreCase = true)
                val redirect = if (code in 301..308) res.header("Location") else null
                val remarks  = buildString {
                    if (isCf)      append("[CF] ")
                    if (isWaf)     append("[WAF] ")
                    if (redirect != null) append("[→$redirect]")
                }.trim()
                SiteProbeResult(code, ip, true, isCf, isWaf, redirect, remarks)
            }
        } catch (e: Exception) {
            SiteProbeResult(-1, ip, resolved, false, false, null, "Conn error: ${e.message?.take(60)}")
        }
    }

    // ── Part 3: Ana Test & Report ─────────────────────────────────────────────

    @Test
    fun runLiveDiagnostics() {
        println("=== Kitsugi Turkish CS Plugin Live Diagnostic ===")
        println("7 repodan TÜM eklentiler çekiliyor...\n")

        // 1. Tüm 7 repodan plugin listesi topla
        val allPlugins = mutableListOf<PluginEntry>()
        for (repo in REPOS) {
            println("Repo fetch: ${repo.name}")
            val fetched = fetchPluginsFromRepo(repo)
            println("  → ${fetched.size} eklenti bulundu")
            allPlugins.addAll(fetched)
        }
        println("\nToplam ${allPlugins.size} eklenti bulundu. Probe başlıyor...\n")

        // 2. Duplicate kontrolü — aynı internalName birden fazla repoda olabilir,
        //    hepsini ayrı satır olarak raporla (hangi repodan geldiği önemli)
        val results = mutableListOf<DiagResult>()

        for ((index, plugin) in allPlugins.withIndex()) {
            println("[${"${index + 1}".padStart(3)}/${allPlugins.size}] " +
                    "${plugin.internalName} (${plugin.repoName})")

            // 2a. CS3 dosyası probe
            val cs3Code = probeCs3(plugin.cs3Url)
            val cs3Ok   = cs3Code in 200..299

            // 2b. Provider sitesi probe (eğer domain biliniyorsa)
            val siteResult = plugin.providerUrl?.let { probeSite(it) }

            val result = DiagResult(
                repoName      = plugin.repoName,
                plugin        = plugin.internalName,
                displayName   = plugin.displayName,
                cs3Url        = plugin.cs3Url,
                cs3Status     = cs3Code,
                cs3Ok         = cs3Ok,
                siteUrl       = plugin.providerUrl,
                siteStatus    = siteResult?.httpStatus ?: -1,
                siteResolved  = siteResult?.resolved ?: false,
                siteIp        = siteResult?.ipAddress ?: "N/A",
                isCloudflare  = siteResult?.isCloudflare ?: false,
                isWafBlocked  = siteResult?.isWafBlocked ?: false,
                redirectUrl   = siteResult?.redirectUrl,
                tvTypes       = plugin.tvTypes.joinToString(", ").ifBlank { "?" },
                remarks       = buildString {
                    if (!cs3Ok)                    append("[CS3:$cs3Code] ")
                    if (siteResult?.remarks?.isNotBlank() == true)
                        append(siteResult.remarks)
                }.trim()
            )
            results.add(result)

            println("    CS3=$cs3Code  Site=${siteResult?.httpStatus ?: "N/A"}  " +
                    "CF=${siteResult?.isCloudflare ?: false}  " +
                    "WAF=${siteResult?.isWafBlocked ?: false}")
        }

        // 3. Rapor oluştur
        generateReport(results)
        println("\n=== Diagnostic tamamlandı. ${results.size} eklenti probe edildi. ===")
    }

    // ── Report Generator ──────────────────────────────────────────────────────
    private fun generateReport(results: List<DiagResult>) {
        val sb = StringBuilder()
        sb.appendLine("# Kitsugi CS Plugin Live Diagnostics — Tam Rapor")
        sb.appendLine()
        sb.appendLine("**Oluşturulma:** ${java.time.LocalDateTime.now()}")
        sb.appendLine("**Toplam probe edilen eklenti:** ${results.size}")
        sb.appendLine()

        // Özet
        val cs3Ok     = results.count { it.cs3Ok }
        val cs3Bad    = results.count { !it.cs3Ok }
        val siteOk    = results.count { it.siteStatus in 200..299 }
        val siteCf    = results.count { it.isCloudflare }
        val siteWaf   = results.count { it.isWafBlocked }
        val noSite    = results.count { it.siteUrl == null }

        sb.appendLine("## Özet")
        sb.appendLine("| Metrik | Sayı |")
        sb.appendLine("|---|---|")
        sb.appendLine("| ✅ CS3 İndirilebilir | $cs3Ok |")
        sb.appendLine("| ❌ CS3 Erişilemiyor | $cs3Bad |")
        sb.appendLine("| 🟢 Site 200 OK | $siteOk |")
        sb.appendLine("| ⚠️ Cloudflare Korumalı | $siteCf |")
        sb.appendLine("| 🚨 WAF Engeli | $siteWaf |")
        sb.appendLine("| ❓ Domain Bilinmiyor | $noSite |")
        sb.appendLine()

        // Repo bazlı gruplama
        val byRepo = results.groupBy { it.repoName }
        for ((repoName, repoResults) in byRepo) {
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## 📦 $repoName (${repoResults.size} eklenti)")
            sb.appendLine()
            sb.appendLine("| Eklenti | TV Türleri | CS3 | Site URL | Site Kodu | CF | WAF | Açıklama |")
            sb.appendLine("|---|---|---|---|---|---|---|---|")
            for (r in repoResults.sortedBy { it.plugin }) {
                val cs3Icon  = if (r.cs3Ok) "✅${r.cs3Status}" else "❌${r.cs3Status}"
                val siteCode = if (r.siteUrl != null) r.siteStatus.toString() else "N/A"
                val cfIcon   = if (r.isCloudflare) "⚠️" else "-"
                val wafIcon  = if (r.isWafBlocked) "🚨" else "-"
                val siteStr  = r.siteUrl?.let { "`$it`" } ?: "_bilinmiyor_"
                sb.appendLine("| **${r.plugin}** | ${r.tvTypes} | $cs3Icon | $siteStr | $siteCode | $cfIcon | $wafIcon | ${r.remarks} |")
            }
            sb.appendLine()
        }

        // Dosyaya yaz
        val content = sb.toString()
        val paths = listOf(
            File("C:\\Kitsugi-Beta\\plugin_diagnostic_report.md"),
            File("C:\\Kitsugi-Beta\\app\\build\\reports\\plugin_diagnostic_report.md"),
            File("C:\\Users\\Administrator\\.gemini\\antigravity\\brain\\d03b5594-3544-4c98-8ccf-e6c313b5ad26\\plugin_diagnostic_report.md")
        )
        for (f in paths) {
            try {
                f.parentFile?.mkdirs()
                f.writeText(content)
                println("Rapor yazıldı: ${f.absolutePath}")
            } catch (e: Exception) {
                println("Rapor yazılamadı: ${f.absolutePath} — ${e.message}")
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VIDEO CDN / EMBED EXTRACTOR HOST PROBE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Türk CS eklentilerinin kullandığı tüm embed/video CDN host'larını probe eder.
     *
     * Her host için kontrol edilenler:
     *  - HTTP status kodu (HEAD request)
     *  - Cloudflare koruması var mı? (CF-Ray header / Server: cloudflare)
     *  - WAF / Access Denied (403, 503, body pattern)
     *  - DNS çözümlenebiliyor mu?
     *  - Yönlendirme (Location header)
     *
     * Sonuç: video_extractor_report.md
     */
    @Test
    fun testVideoSourceExtractors() {
        println("=== Kitsugi Video CDN / Embed Extractor Host Probe ===")

        // ── Bilinen tüm embed/video CDN host'ları ─────────────────────────────
        // Format: "EklentiAdı" to "https://host-url.com"
        val extractorHosts = listOf(
            // ── Genel video CDN'leri ──────────────────────────────────────────
            "Doodstream"        to "https://dood.la",
            "Doodstream (re)"   to "https://doodstream.com",
            "Doodstream (wf)"   to "https://dood.wf",
            "Doodstream (pm)"   to "https://dood.pm",
            "Vidmoly"           to "https://vidmoly.to",
            "Filemoon"          to "https://filemoon.sx",
            "Filemoon (nl)"     to "https://filemoon.nl",
            "Streamtape"        to "https://streamtape.com",
            "Streamtape (to)"   to "https://streamtape.to",
            "Mixdrop"           to "https://mixdrop.ag",
            "Okru"              to "https://ok.ru",
            "Sibnet"            to "https://video.sibnet.ru",
            "Mail.ru"           to "https://my.mail.ru",
            "Mp4Upload"         to "https://mp4upload.com",
            "SendVid"           to "https://sendvid.com",
            "Upstream"          to "https://upstream.to",
            "Voe"               to "https://voe.sx",
            "StreamWish"        to "https://streamwish.to",
            "StreamWish (com)"  to "https://streamwish.com",
            "Fembed"            to "https://fembed.com",
            "Vtube"             to "https://vtube.to",
            "Vidplay"           to "https://vidplay.online",
            "Vidsrc"            to "https://vidsrc.to",
            "VTubE"             to "https://vtb.to",
            "Videa"             to "https://videa.hu",
            "Dailymotion"       to "https://www.dailymotion.com",
            "Odysee"            to "https://odysee.com",
            "Rumble"            to "https://rumble.com",

            // ── Türkiye odaklı CDN'ler ────────────────────────────────────────
            "CloseLoad"         to "https://closeload.top",
            "GStore"            to "https://gstore.one",
            "TRsTX"             to "https://trstx.org",
            "TürkAnime CDN"     to "https://cdn.turkanime.co",
            "Anizium API"       to "https://api.anizium.co",
            "SeiCode CDN"       to "https://seiwatch.net",

            // ── Yedek / alternatif embed host'ları ────────────────────────────
            "FileMoon (in)"     to "https://filemoon.in",
            "DropLoad"          to "https://dropload.io",
            "BuzzHeavier"       to "https://buzzheavier.com",
            "Chillx"            to "https://chillx.top",
            "Boosterx"          to "https://boosterx.stream",
            "Uqload"            to "https://uqload.co",
            "Uqload (com)"      to "https://uqload.com",
            "Vidia"             to "https://vidia.tv",
            "Playtaku"          to "https://playtaku.net",
            "Playtaku (online)" to "https://playtaku.online",
            "Kwik"              to "https://kwik.si",
            "Kwik (cx)"         to "https://kwik.cx",
            "GogoAnime CDN"     to "https://gogohd.net",
            "AniHLS"            to "https://anihls.io",
            "HiAnime CDN"       to "https://megacloud.tv",
            "Rapidcloud"        to "https://rapidcloud.cc",
            "Embtaku"           to "https://embtaku.pro",
        )

        data class ExtractorResult(
            val name: String,
            val url: String,
            val httpCode: Int,
            val ipAddress: String,
            val dnsOk: Boolean,
            val isCloudflare: Boolean,
            val isWafBlocked: Boolean,
            val redirectUrl: String?,
            val status: String,   // OK | CF | WAF | DEAD | DNS_FAIL | REDIRECT
            val remarks: String
        )

        val results = mutableListOf<ExtractorResult>()

        for ((name, url) in extractorHosts) {
            print("  Probe: ${name.padEnd(24)} $url ... ")
            val probe = probeSite(url)

            val status = when {
                !probe.resolved            -> "DNS_FAIL"
                probe.redirectUrl != null  -> "REDIRECT"
                probe.isWafBlocked         -> "WAF"
                probe.isCloudflare && probe.httpStatus in 200..299 -> "CF_OK"
                probe.isCloudflare         -> "CF"
                probe.httpStatus in 200..299 -> "OK"
                probe.httpStatus == -1     -> "DEAD"
                else                       -> "HTTP_${probe.httpStatus}"
            }
            println("[$status] HTTP=${probe.httpStatus} CF=${probe.isCloudflare} WAF=${probe.isWafBlocked}")

            results.add(ExtractorResult(
                name          = name,
                url           = url,
                httpCode      = probe.httpStatus,
                ipAddress     = probe.ipAddress,
                dnsOk         = probe.resolved,
                isCloudflare  = probe.isCloudflare,
                isWafBlocked  = probe.isWafBlocked,
                redirectUrl   = probe.redirectUrl,
                status        = status,
                remarks       = probe.remarks
            ))
        }

        // ── Rapor Oluştur ──────────────────────────────────────────────────────
        val sb = StringBuilder()
        sb.appendLine("# Kitsugi Video CDN / Embed Extractor Host Diagnostic")
        sb.appendLine()
        sb.appendLine("**Oluşturulma:** ${java.time.LocalDateTime.now()}")
        sb.appendLine("**Toplam probe edilen host:** ${results.size}")
        sb.appendLine()

        val ok       = results.count { it.status == "OK" || it.status == "CF_OK" }
        val cfOnly   = results.count { it.status == "CF" }
        val waf      = results.count { it.status == "WAF" }
        val redirect = results.count { it.status == "REDIRECT" }
        val dead     = results.count { it.status == "DEAD" || it.status == "DNS_FAIL" }

        sb.appendLine("## Özet")
        sb.appendLine("| Durum | Sayı |")
        sb.appendLine("|---|---|")
        sb.appendLine("| ✅ Erişilebilir (OK / CF-OK) | $ok |")
        sb.appendLine("| ⚠️ Sadece CF Korumalı (muhtemelen çalışır) | $cfOnly |")
        sb.appendLine("| 🚨 WAF Engeli (muhtemelen çalışmaz) | $waf |")
        sb.appendLine("| 🔀 Yönlendirme | $redirect |")
        sb.appendLine("| 💀 Erişilemiyor (DNS / DEAD) | $dead |")
        sb.appendLine()

        sb.appendLine("## Detaylı Sonuçlar")
        sb.appendLine()
        sb.appendLine("| Host | URL | HTTP | IP | CF | WAF | Durum | Açıklama |")
        sb.appendLine("|---|---|---|---|---|---|---|---|")

        for (r in results.sortedWith(compareBy({ it.status }, { it.name }))) {
            val icon = when (r.status) {
                "OK", "CF_OK"  -> "✅"
                "CF"           -> "⚠️"
                "WAF"          -> "🚨"
                "REDIRECT"     -> "🔀"
                else           -> "💀"
            }
            val cfIcon  = if (r.isCloudflare) "⚠️" else "-"
            val wafIcon = if (r.isWafBlocked) "🚨" else "-"
            val remark  = buildString {
                if (r.redirectUrl != null) append("→${r.redirectUrl} ")
                append(r.remarks)
            }.trim()
            sb.appendLine("| **${r.name}** | `${r.url}` | ${r.httpCode} | ${r.ipAddress} | $cfIcon | $wafIcon | $icon ${r.status} | $remark |")
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("### 🔴 Çalışmayan Kaynaklar (WAF / DEAD / DNS_FAIL)")
        sb.appendLine()
        val failing = results.filter { it.status in listOf("WAF", "DEAD", "DNS_FAIL", "HTTP_404", "HTTP_503", "HTTP_502") }
        if (failing.isEmpty()) {
            sb.appendLine("_Tüm host'lar erişilebilir!_")
        } else {
            for (r in failing) {
                sb.appendLine("- **${r.name}** (`${r.url}`) — ${r.status} (HTTP ${r.httpCode}): ${r.remarks}")
            }
        }

        sb.appendLine()
        sb.appendLine("### 🟡 Yönlendirme Gerektiren Kaynaklar")
        sb.appendLine()
        val redirecting = results.filter { it.status == "REDIRECT" }
        if (redirecting.isEmpty()) {
            sb.appendLine("_Yönlendirme yok._")
        } else {
            for (r in redirecting) {
                sb.appendLine("- **${r.name}** (`${r.url}`) → `${r.redirectUrl}`")
            }
        }

        // Dosyaya yaz
        val report = sb.toString()
        val paths = listOf(
            File("C:\\Kitsugi-Beta\\video_extractor_report.md"),
            File("C:\\Kitsugi-Beta\\app\\build\\reports\\video_extractor_report.md"),
            File("C:\\Users\\Administrator\\.gemini\\antigravity\\brain\\d03b5594-3544-4c98-8ccf-e6c313b5ad26\\video_extractor_report.md")
        )
        for (f in paths) {
            try {
                f.parentFile?.mkdirs()
                f.writeText(report)
                println("Video extractor raporu yazıldı: ${f.absolutePath}")
            } catch (e: Exception) {
                println("Video extractor raporu yazılamadı: ${f.absolutePath} — ${e.message}")
            }
        }

        println("\n=== Video CDN Probe tamamlandı. ${results.size} host test edildi. ===")
        println("  OK/CF_OK: $ok  |  CF: $cfOnly  |  WAF: $waf  |  Redirect: $redirect  |  Dead: $dead")
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LEVEL 4-6: PROVIDER STREAM SOURCE DISCOVERY
    // Her provider sitesinde gerçek HTTP arama yaparak hangi video CDN'lerini
    // kullandığını tespit eder ve o CDN'lerin erişilebilir olup olmadığını kontrol eder.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Test Seviyesi:
     *  L4 — Provider sitesinde gerçek arama isteği (örn. "naruto")
     *  L5 — Arama sonucu sayfasından embed/iframe URL'lerini parse et
     *  L6 — Bulunan embed host'larını HEAD isteğiyle erişilebilirlik testi yap
     *
     * Bu test CS3 dosyasını yüklemez (DEX = Android runtime gerektirir).
     * Bunun yerine raw HTTP + regex ile provider'ın kendi sitesini tarar.
     *
     * Sonuç: provider_stream_source_report.md
     */
    @Test
    fun testProviderStreamSources() {
        println("=== Kitsugi Provider Stream Source Discovery (L4-L6) ===")
        println("Her provider sitesinde 'naruto' araması yapılıyor ve embed kaynakları keşfediliyor...\n")

        // Arama için test animesi — Türk sitelerinde çok yaygın, hemen sonuç çıkar
        val TEST_QUERIES = listOf("naruto", "one piece", "attack on titan", "dragon ball")

        // Bilinen embed host kalıpları — URL içinde bu string'ler geçerse embed tespit edilir
        val EMBED_PATTERNS = mapOf(
            "doodstream.com" to "Doodstream",
            "dood.la"        to "Doodstream",
            "dood.wf"        to "Doodstream",
            "playmogo.com"   to "Doodstream (playmogo)",
            "vidmoly"        to "Vidmoly",
            "filemoon"       to "Filemoon",
            "streamtape"     to "Streamtape",
            "streamwish"     to "StreamWish",
            "ok.ru"          to "Okru",
            "vk.com"         to "VK",
            "vkvideo.ru"     to "VK Video",
            "sibnet"         to "Sibnet",
            "mixdrop"        to "Mixdrop",
            "mp4upload"      to "Mp4Upload",
            "sendvid"        to "SendVid",
            "upstream"       to "Upstream",
            "voe.sx"         to "Voe",
            "swdyu"          to "StreamWish (alt)",
            "wishfast"       to "StreamWish (alt2)",
            "sfastwish"      to "StreamWish (alt3)",
            "fembed"         to "Fembed",
            "vtube"          to "Vtube",
            "vidplay"        to "Vidplay",
            "vidoy.com"      to "Vidplay (vidoy)",
            "vidsrc"         to "Vidsrc",
            "videa"          to "Videa",
            "dailymotion"    to "Dailymotion",
            "youtube.com"    to "YouTube",
            "rumble.com"     to "Rumble",
            "closeload"      to "CloseLoad (TR)",
            "trstx.org"      to "TRsTX (TR)",
            "gstore"         to "GStore (TR)",
            "anizium"        to "Anizium (TR)",
            "seiwatch"       to "SeiCode (TR)",
            "megacloud"      to "MegaCloud",
            "rapidcloud"     to "Rapidcloud",
            "kwik"           to "Kwik",
            "chillx"         to "Chillx",
            "mail.ru"        to "Mail.ru",
            "odnoklassniki"  to "Okru (alt)",
            "playtaku"       to "Playtaku",
            "uqload"         to "Uqload",
            "dropload"       to "Dropload",
            "embtaku"        to "Embtaku",
            "shell.php"      to "Generic Player",
            "player.php"     to "Generic Player",
            "video_ext.php"  to "Generic Player",
            ".m3u8"          to "Direct HLS",
            ".mp4"           to "Direct MP4",
        )

        // Redirect-aware client
        val followClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        data class EmbedHit(
            val cdnName: String,
            val embedUrl: String,
            val cdnAccessible: Boolean,  // HEAD probe sonucu
            val cdnHttpCode: Int
        )

        data class ProviderStreamResult(
            val providerName: String,
            val siteUrl: String,
            val searchQuery: String,
            val searchStatus: Int,     // HTTP kodu arama sayfasında
            val isCloudflare: Boolean,
            val resultsFound: Int,     // kaç adet sonuç linki bulundu
            val embedsFound: List<EmbedHit>,
            val rawEmbedUrls: List<String>
        )

        // Regex kalıpları — iframe src, embed src, player kaynaklarını çekmek için
        val iframeSrcRegex = Regex("""iframe[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val embedSrcRegex  = Regex("""embed[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val sourceSrcRegex = Regex("""<source[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val dataPlayerRegex= Regex("""data-(?:src|url|player)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val hrefLiRegex    = Regex("""href\.li/\?([^\s"'<>]+)""", RegexOption.IGNORE_CASE)
        // arama sonucu linkleri
        val resultLinkRegex = Regex("""href=["'](/(?:dizi|film|anime|izle|watch|video|episode|sezon)[^"'#?]{3,60})["']""", RegexOption.IGNORE_CASE)

        // Türkçe site arama URL kalıpları
        // Her site farklı arama endpoint kullanır, en yaygın pattern'ları dene
        fun buildSearchUrls(baseUrl: String, query: String): List<String> {
            val enc = java.net.URLEncoder.encode(query, "UTF-8")
            return listOf(
                "$baseUrl/?s=$enc",
                "$baseUrl/search?q=$enc",
                "$baseUrl/arama?q=$enc",
                "$baseUrl/search/$enc",
                "$baseUrl/?search=$enc",
                "$baseUrl/ara?s=$enc",
            )
        }

        // Bir URL'den HTML body çeker, redirect takip eder
        fun fetchHtml(url: String): Pair<Int, String?> {
            return try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", ua)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.8")
                    .build()
                followClient.newCall(req).execute().use { res ->
                    res.code to (res.body?.string())
                }
            } catch (e: Exception) {
                -1 to null
            }
        }

        // Provider sitesinde arama yapıp HTML bulunca ilk sonuç sayfasını yükler
        fun searchProviderAndGetEpisodePage(baseUrl: String, query: String): Triple<Int, Boolean, String?> {
            // 1. Arama sayfasını dene
            for (searchUrl in buildSearchUrls(baseUrl, query)) {
                val (code, html) = fetchHtml(searchUrl)
                if (code == 200 && html != null && html.length > 500) {
                    val isCf = html.contains("cf-browser-verification", ignoreCase = true) ||
                            html.contains("Just a moment", ignoreCase = true)
                    if (isCf) return Triple(code, true, null)

                    // İlk sonuç linkini bul
                    val firstResult = resultLinkRegex.find(html)?.groupValues?.get(1)
                    if (firstResult != null) {
                        val resultUrl = if (firstResult.startsWith("http")) firstResult else "$baseUrl$firstResult"
                        println("    → Arama sonucu bulundu: $resultUrl")
                        val (epCode, epHtml) = fetchHtml(resultUrl)
                        if (epCode == 200 && epHtml != null) {
                            return Triple(code, false, epHtml)
                        }
                    }
                    // Sonuç linki bulunamadı ama sayfa yüklendi
                    return Triple(code, false, html)
                }
            }
            return Triple(-1, false, null)
        }

        // HTML'den embed URL'lerini çıkar
        fun extractEmbedUrls(html: String, baseUrl: String): List<String> {
            val urls = mutableSetOf<String>()
            for (regex in listOf(iframeSrcRegex, embedSrcRegex, sourceSrcRegex, dataPlayerRegex)) {
                regex.findAll(html).forEach { m ->
                    val u = m.groupValues[1].trim()
                    if (u.isNotBlank() && !u.startsWith("data:") && !u.startsWith("#")) {
                        val resolved = when {
                            u.startsWith("//")   -> "https:$u"
                            u.startsWith("/")    -> "$baseUrl$u"
                            u.startsWith("http") -> u
                            else                 -> u
                        }
                        urls.add(resolved)
                    }
                }
            }
            // href.li wrapper'ları çöz
            hrefLiRegex.findAll(html).forEach { m ->
                val target = m.groupValues[1].trim()
                if (target.startsWith("http")) urls.add(target)
            }
            return urls.toList()
        }

        // Embed URL'sini bilinen CDN kalıplarıyla eşleştir
        fun matchCdn(url: String): String? {
            val lower = url.lowercase()
            return EMBED_PATTERNS.entries.firstOrNull { (pattern, _) ->
                lower.contains(pattern)
            }?.value
        }

        // CDN host'una HEAD probe yap
        fun probeEmbedHost(url: String): Pair<Int, Boolean> {
            return try {
                val host = java.net.URI(url).host ?: return -1 to false
                val headUrl = "https://$host"
                val req = Request.Builder()
                    .url(headUrl)
                    .head()
                    .header("User-Agent", ua)
                    .build()
                client.newCall(req).execute().use { res ->
                    res.code to (res.code in 200..399)
                }
            } catch (e: Exception) { -1 to false }
        }

        // ── Ana probe döngüsü ──────────────────────────────────────────────────
        val allResults = mutableListOf<ProviderStreamResult>()

        // Sadece KNOWN_DOMAINS'daki (bilinen sitesi olan) provider'ları tara
        val uniqueProviders = KNOWN_DOMAINS.entries
            .distinctBy { it.value }   // aynı URL'yi birden fazla eklenti paylaşıyorsa bir kez tara
            .sortedBy { it.key }

        println("Toplam ${uniqueProviders.size} benzersiz provider taranacak.\n")

        for ((name, siteUrl) in uniqueProviders) {
            print("[$name] $siteUrl ... ")

            val provedQueryResult = TEST_QUERIES.firstNotNullOfOrNull { query ->
                val (code, isCf, html) = searchProviderAndGetEpisodePage(siteUrl, query)
                if (html != null) Triple(query, code, html)
                else if (code == 200 && isCf) {
                    println("[CF-BLOCKED] Cloudflare challenge")
                    null
                } else null
            }

            if (provedQueryResult == null) {
                println("[SEARCH-FAILED]")
                allResults.add(ProviderStreamResult(
                    providerName  = name,
                    siteUrl       = siteUrl,
                    searchQuery   = "—",
                    searchStatus  = -1,
                    isCloudflare  = false,
                    resultsFound  = 0,
                    embedsFound   = emptyList(),
                    rawEmbedUrls  = emptyList()
                ))
                continue
            }

            val (successQuery, httpCode, html) = provedQueryResult
            val rawEmbeds = extractEmbedUrls(html, siteUrl)

            // CDN eşleştirme + erişilebilirlik
            val cdnHits = mutableListOf<EmbedHit>()
            val seenCdns = mutableSetOf<String>()
            for (embedUrl in rawEmbeds) {
                val cdnName = matchCdn(embedUrl) ?: continue
                if (cdnName in seenCdns) continue
                seenCdns.add(cdnName)
                val (cdnCode, cdnOk) = probeEmbedHost(embedUrl)
                cdnHits.add(EmbedHit(cdnName, embedUrl, cdnOk, cdnCode))
            }

            println("✓ '$successQuery' → ${rawEmbeds.size} embed URL, ${cdnHits.size} CDN bulundu: ${cdnHits.map { it.cdnName }}")

            allResults.add(ProviderStreamResult(
                providerName  = name,
                siteUrl       = siteUrl,
                searchQuery   = successQuery,
                searchStatus  = httpCode,
                isCloudflare  = html.contains("cf-browser-verification", ignoreCase = true),
                resultsFound  = rawEmbeds.size,
                embedsFound   = cdnHits,
                rawEmbedUrls  = rawEmbeds.take(10) // debug için ilk 10
            ))
        }

        // ── Rapor oluştur ──────────────────────────────────────────────────────
        val sb = StringBuilder()
        sb.appendLine("# Kitsugi Provider Stream Source Discovery — L4/L5/L6 Raporu")
        sb.appendLine()
        sb.appendLine("**Oluşturulma:** ${java.time.LocalDateTime.now()}")
        sb.appendLine("**Taranan provider:** ${allResults.size}")
        sb.appendLine()

        // Özet istatistikler
        val withEmbeds  = allResults.count { it.embedsFound.isNotEmpty() }
        val withSearch  = allResults.count { it.searchStatus == 200 }
        val noSearch    = allResults.count { it.searchStatus == -1 }
        val cfBlocked   = allResults.count { it.isCloudflare }

        // Tüm CDN'lerin kaç farklı provider tarafından kullanıldığı
        val cdnUsageMap = mutableMapOf<String, MutableList<String>>()
        for (r in allResults) {
            for (hit in r.embedsFound) {
                cdnUsageMap.getOrPut(hit.cdnName) { mutableListOf() }.add(r.providerName)
            }
        }

        sb.appendLine("## Özet")
        sb.appendLine("| Metrik | Sayı |")
        sb.appendLine("|---|---|")
        sb.appendLine("| 🔍 Arama başarılı (200 OK + embed bulundu) | $withEmbeds |")
        sb.appendLine("| ✅ Arama sayfası 200 OK | $withSearch |")
        sb.appendLine("| ❌ Arama başarısız (timeout/CF/404) | $noSearch |")
        sb.appendLine("| 🔐 Cloudflare challenge (JS bypass gerekli) | $cfBlocked |")
        sb.appendLine()

        sb.appendLine("## CDN Kullanım Haritası (hangi CDN kaç provider tarafından kullanılıyor)")
        sb.appendLine()
        sb.appendLine("| CDN Host | Kullanan Provider Sayısı | Provider Listesi |")
        sb.appendLine("|---|---|---|")
        for ((cdn, providers) in cdnUsageMap.entries.sortedByDescending { it.value.size }) {
            sb.appendLine("| **$cdn** | ${providers.size} | ${providers.joinToString(", ")} |")
        }
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## Provider Detay Tablosu")
        sb.appendLine()
        sb.appendLine("| Provider | Site | Arama | Embed Sayısı | Kullanılan CDN'ler | Durum |")
        sb.appendLine("|---|---|---|---|---|---|")
        for (r in allResults.sortedBy { it.providerName }) {
            val cdnList = r.embedsFound.joinToString(", ") { h ->
                val icon = if (h.cdnAccessible) "✅" else "❌"
                "$icon ${h.cdnName}"
            }.ifBlank { "—" }
            val status = when {
                r.isCloudflare  -> "🔐 CF Challenge"
                r.searchStatus == 200 && r.embedsFound.isNotEmpty() -> "✅ Stream Bulundu"
                r.searchStatus == 200 -> "⚠️ Sayfa OK, embed yok"
                else                  -> "❌ Erişilemiyor"
            }
            sb.appendLine("| **${r.providerName}** | `${r.siteUrl}` | ${r.searchQuery} | ${r.embedsFound.size} | $cdnList | $status |")
        }
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## 🔴 Çalışmayan Video Kaynakları (embed bulundu ama CDN erişilemiyor)")
        sb.appendLine()
        var foundDeadCdns = false
        for (r in allResults) {
            val deadHits = r.embedsFound.filter { !it.cdnAccessible }
            if (deadHits.isNotEmpty()) {
                foundDeadCdns = true
                sb.appendLine("### ${r.providerName} (`${r.siteUrl}`)")
                for (h in deadHits) {
                    sb.appendLine("- ❌ **${h.cdnName}** — HTTP ${h.cdnHttpCode} — `${h.embedUrl.take(100)}`")
                }
                sb.appendLine()
            }
        }
        if (!foundDeadCdns) sb.appendLine("_Tespit edilen tüm CDN'ler erişilebilir!_")

        sb.appendLine()
        sb.appendLine("## 🟢 Provider başına çalışan video kaynakları")
        sb.appendLine()
        for (r in allResults.filter { it.embedsFound.any { h -> h.cdnAccessible } }) {
            val working = r.embedsFound.filter { it.cdnAccessible }.map { it.cdnName }
            sb.appendLine("- **${r.providerName}**: ${working.joinToString(", ")}")
        }

        // Dosyaya yaz
        val report = sb.toString()
        val paths = listOf(
            File("C:\\Kitsugi-Beta\\provider_stream_source_report.md"),
            File("C:\\Users\\Administrator\\.gemini\\antigravity\\brain\\b8f0a5d7-cbae-47e1-8a10-3c580dd77d6b\\provider_stream_source_report.md")
        )
        for (f in paths) {
            try {
                f.parentFile?.mkdirs()
                f.writeText(report)
                println("\nL4-L6 Stream Source raporu yazıldı: ${f.absolutePath}")
            } catch (e: Exception) {
                println("\nRapor yazılamadı: ${f.absolutePath} — ${e.message}")
            }
        }

        println("\n=== Provider Stream Source Discovery tamamlandı ===")
        println("  Stream bulundu: $withEmbeds / ${allResults.size} provider")
        println("  Benzersiz CDN: ${cdnUsageMap.size}")
    }
}
