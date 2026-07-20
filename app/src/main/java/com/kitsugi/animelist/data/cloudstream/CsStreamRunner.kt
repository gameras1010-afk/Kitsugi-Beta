package com.kitsugi.animelist.data.cloudstream

import android.util.Log
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.remote.KitsugiIdResolver
import com.kitsugi.animelist.core.player.SubtitleInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Cloudflare veya ağ koruması nedeniyle içerik alınamadığında fırlatılır.
 * [KitsugiStreamScreen] bu exception'ı yakalayarak kullanıcıya "Doğrula" butonunu sunar.
 */
class CloudflareBlockException(message: String, cause: Throwable? = null) : Exception(message, cause)

object CsStreamRunner {

    private const val TAG = "CsStreamRunner"

    /**
     * Arama/stream hatalarını ayırt etmek için ayrı ERROR tag.
     * Filtre: adb logcat -s CS_SEARCH_ERR
     * Combo:  adb logcat -s PLUGIN_DIAG -s CS_SEARCH_ERR
     */
    private const val SERR = "CS_SEARCH_ERR"
    private val yearRegex = Regex("\\b(19|20)\\d{2}\\b")

    /**
     * Arama isteklerini throttle eder — aynı anda en fazla 12 sağlayıcı arama yapar.
     * loadLinks/load için ayrı semaphore kullanıyoruz (deadlock önleme).
     */
    private val searchSemaphore = Semaphore(12)

    /**
     * İçerik yükleme ve stream link çekme için ayrı semaphore.
     * Cloudflare rate-limit engellerini önlemek için max 6 eşzamanlı istek.
     */
    private val loadSemaphore = Semaphore(6)

    /** Tek bir provider için stream getirme zaman aşımı — 40s yavaş/mobil ağlarda kesintileri önlemek için idealdir */
    private const val PROVIDER_TIMEOUT_MS = 40_000L

    /**
     * Bilinen domain değişikliklerini otomatik uygular.
     * Site taşındığında plugin yeniden indirilmeden arama çalışmaya devam eder.
     *
     * Format: "eski_domain" -> "yeni_domain"
     * Kaynak: https://github.com/Kraptor123/domainListesi/blob/main/eklenti_domainleri.txt
     */
    private val KNOWN_DOMAIN_FIXES = mapOf(
        // AsyaAnimeleri .pw'den .top'a taşındı
        "asyaanimeleri.pw"  to "asyaanimeleri.top",
        // TurkAnime — eski domain redirect yapıyor ama yenisini direkt ver
        "turkanime.co"      to "www.turkanime.tv",
        // Animeler — .me → .pw geçmişi, şimdilik .pw aktif
        "animeler.me"       to "animeler.pw",
    )

    /**
     * Kalıcı olarak bozuk olan plugin'ler — DNS çözümleme hatası, NPE döngüsü veya
     * sunucu tarafı tam arıza nedeniyle her sorguda başarısız olduğu biliniyor.
     * Bu plugin'ler anında atlanır; ağ çağrısı yapılmaz.
     *
     * Bir plugin buradan kaldırılırken, önce log'larda başarılı arama çıktısı
     * görüldüğünden emin olunmalıdır.
     */
    private val KNOWN_BROKEN_PLUGINS = setOf(
        // Domain çözümlemiyor: ifsalog4.club UnknownHostException
        "IfsaLog",
        // Domain çözümlemiyor: www.superfilmgeldi13.art UnknownHostException
        "SuperFilmGeldi",
        // Her aramada NullPointerException — sunucu JSON yapısı değişmiş
        "DiziKorea",
        // Domain çözümlemiyor: ugurfilm3.xyz
        "UgurFilm",
        // NoClassDefFoundError: PluginManager — CS3 kütüphane uyumsuzluğu
        "KraptorPlus",
        // Sunucu HTML döndürüyor (hata sayfası), JSON parse crash
        "TvDiziler"
    )

    /**
     * Kalıcı olarak kapalı veya ölü olduğu bilinen alan adları.
     * Eklentinin mainUrl değeri bu domainlerden birini içeriyorsa eklenti direkt atlanır.
     */
    private val KNOWN_BROKEN_DOMAINS = setOf(
        "ifsalog4.club",
        "superfilmgeldi13.art",
        "ugurfilm3.xyz"
    )

    /**
     * Cloudflare / WAF challange nedeniyle yavaş çalışan eklentiler.
     * Bu eklentiler için:
     *  1. ID doğrulaması (syncData kontrolü) atlanır — her load() çağrısı CF timeout riski taşıdığından
     *  2. Timeout PROVIDER_TIMEOUT_MS yerine WEBVIEW_PROVIDER_TIMEOUT_MS kullanılır
     * TrAnimeci: tranimaci.com Custom WAF Security Verification — CloudflareKiller bile takılıyor
     */
    private val CF_PROTECTED_PLUGINS = setOf(
        "TrAnimeci",
        "TrAnimeIzle"
    )

    /** CF korumalı eklentiler için uzatılmış timeout (90 saniye) */
    private const val CF_PROVIDER_TIMEOUT_MS = 90_000L

    private val dynamicDomains = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val isDomainListFetched = java.util.concurrent.atomic.AtomicBoolean(false)
    private val runnerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    private suspend fun fetchRemoteDomains() = withContext(Dispatchers.IO) {
        if (isDomainListFetched.getAndSet(true)) return@withContext
        try {
            Log.d(TAG, "Fetching remote domain list from GitHub...")
            val request = okhttp3.Request.Builder()
                .url("https://raw.githubusercontent.com/Kraptor123/domainListesi/main/eklenti_domainleri.txt")
                .build()
            val text = com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("HTTP error ${response.code}")
                response.body?.string() ?: ""
            }
            
            val lines = text.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("|")) {
                    val parts = trimmed.substring(1).split(":", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim().lowercase(Locale.ROOT)
                        val url = parts[1].trim()
                        if (name.isNotEmpty() && url.isNotEmpty()) {
                            dynamicDomains[name] = url
                        }
                    }
                }
            }
            Log.d(TAG, "Successfully loaded ${dynamicDomains.size} remote domains.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote domains: ${e.message}")
            // Reset flag so we can try again later
            isDomainListFetched.set(false)
        }
    }

    /**
     * Plugin'in mainUrl'sini bilinen eski→yeni domain eşlemeleriyle günceller.
     * Eğer plugin zaten doğru domain'i kullanıyorsa hiçbir şey değişmez.
     */
    private fun applyDomainFix(api: MainAPI) {
        val nameKey = api.name.lowercase(Locale.ROOT)
        val remoteUrl = dynamicDomains[nameKey]
        if (remoteUrl != null) {
            val currentUrl = api.mainUrl
            val normalize = { u: String -> u.replace("https://", "").replace("http://", "").replace("www.", "").trimEnd('/') }
            if (normalize(currentUrl) != normalize(remoteUrl)) {
                Log.w(TAG, "[${api.name}] Domain dinamik olarak güncellendi: $currentUrl -> $remoteUrl")
                api.mainUrl = remoteUrl
                return
            }
        }

        val currentUrl = api.mainUrl
        for ((oldDomain, newDomain) in KNOWN_DOMAIN_FIXES) {
            if (currentUrl.contains(oldDomain)) {
                val fixed = currentUrl.replace(oldDomain, newDomain)
                Log.w(TAG, "[${api.name}] Domain lokal kuralla düzeltildi: $currentUrl -> $fixed")
                api.mainUrl = fixed
                break
            }
        }
    }

    // Tracks unsupported methods per provider to avoid calling them repeatedly
    // Format of key: "ProviderName:methodName"
    // NOTE: This is cleared on every new fetch (startFetch) to avoid stale state from previous anime searches.
    private val unsupportedMethods = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Her yeni anime aramasında çağrılmalı — önceki oturumdan kalan
     * "desteklenmiyor" işaretlerini temizler. Aksi takdirde bir sonraki
     * anime için o eklentinin search() metodu hiç denenmez.
     */
    fun clearUnsupportedMethodsCache() {
        val count = unsupportedMethods.size
        unsupportedMethods.clear()
        if (count > 0) Log.d(TAG, "unsupportedMethods cache temizlendi ($count kayıt silindi)")
    }

    private fun isMethodOverridden(api: MainAPI, methodName: String, vararg parameterTypes: Class<*>): Boolean {
        return try {
            val method = api.javaClass.getMethod(methodName, *parameterTypes)
            method.declaringClass != MainAPI::class.java
        } catch (e: Exception) {
            // Fallback to true if reflection fails so we don't break compatibility
            true
        }
    }

    private fun encodePathQuery(query: String): String {
        return try {
            java.net.URLEncoder.encode(query, "UTF-8")
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~")
        } catch (e: Exception) {
            query
        }
    }

    suspend fun getStreams(
        api: MainAPI,
        title: String,
        alternativeTitles: List<String>,
        year: Int?,
        season: Int,
        episode: Int,
        malId: Int? = null,
        aniListId: Int? = null,
        tmdbId: Int? = null
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        Log.d(TAG, "━━━ getStreams: provider=${api.name} ━━━")
        Log.d(TAG, "  title='$title' season=$season ep=$episode year=$year")
        Log.d(TAG, "  alternativeTitles=${alternativeTitles.take(3)} mal=$malId aniList=$aniListId")

        // Fetch remote domains dynamically in background if not fetched yet
        if (!isDomainListFetched.get()) {
            runnerScope.launch {
                fetchRemoteDomains()
            }
        }

        // Bilinen domain değişikliklerini uygula (ör. AsyaAnimeleri .pw → .top)
        applyDomainFix(api)

        // Kalıcı bozuk olduğu bilinen plugin'leri direkt atla — ağ kaynağı harcama
        if (api.name in KNOWN_BROKEN_PLUGINS) {
            Log.w(TAG, "[${api.name}] KNOWN_BROKEN_PLUGINS listesinde — atlanıyor.")
            return@withContext emptyList()
        }

        // Alan adı bazında ölü domain kontrolü
        val normalizeUrl = { u: String -> u.replace("https://", "").replace("http://", "").replace("www.", "").trimEnd('/') }
        val currentDomain = normalizeUrl(api.mainUrl)
        if (KNOWN_BROKEN_DOMAINS.any { currentDomain.contains(it) }) {
            Log.w(TAG, "[${api.name}] Domain (${api.mainUrl}) ölü domain listesinde — atlanıyor.")
            return@withContext emptyList()
        }

        // Engellenen plugin'leri atla — tekrarlı NotImplementedError veya 3+ hata sonrası oluşur
        if (CsPluginStatusTracker.isBlocked(api.name)) {
            val reason = CsPluginStatusTracker.getErrorMessage(api.name)
            Log.w(TAG, "[${api.name}] Engellendi (session block). Sebep: $reason — atlanıyor.")
            return@withContext emptyList()
        }

        val isCfProtected = api.name in CF_PROTECTED_PLUGINS
        val effectiveTimeout = if (isCfProtected) CF_PROVIDER_TIMEOUT_MS else PROVIDER_TIMEOUT_MS
        if (isCfProtected) {
            Log.w(TAG, "[${api.name}] CF korumalı eklenti — timeout ${effectiveTimeout}ms olarak uzatıldı, ID doğrulaması devre dışı.")
            Log.e(SERR, "🔐 CF_PROTECTED [${api.name}] — ${effectiveTimeout}ms timeout ile çalışıyor. title='$title' S${season}E${episode}")
        }

        val result = runGetStreams(api, title, alternativeTitles, year, season, episode, malId, aniListId, tmdbId)
        result
    }

    private suspend fun runGetStreams(
        api: MainAPI,
        title: String,
        alternativeTitles: List<String>,
        year: Int?,
        season: Int,
        episode: Int,
        malId: Int? = null,
        aniListId: Int? = null,
        tmdbId: Int? = null
    ): List<StreamSource> {
        // Resolve external IDs for validation
        val resolvedIds = if (malId != null || aniListId != null || tmdbId != null) {
            try {
                KitsugiIdResolver.resolveIds(malId, aniListId, tmdbId)
            } catch (e: Exception) {
                null
            }
        } else null
        
        val targetImdb = resolvedIds?.imdbId
        val targetTmdb = resolvedIds?.tmdbId
        val targetKitsu = resolvedIds?.kitsuId
        val isWebViewPlugin = api.usesWebView ||
            api.name in CF_PROTECTED_PLUGINS ||
            api.name.contains("TrAnimeIzle", ignoreCase = true)

        // Build title variants: original + normalized + alts
        val titleVariants = buildTitleVariants(title, alternativeTitles)
        Log.d(TAG, "[${api.name}] Arama varyantları (${titleVariants.size}): ${titleVariants.take(6)}")

        // Search all variants sequentially until we get results
        var results: List<SearchResponse> = emptyList()
        var searchedVariant = ""

        for (variant in titleVariants) {
            results = safeSearch(api, variant)
            if (results.isNotEmpty()) {
                searchedVariant = variant
                Log.d(TAG, "[${api.name}] ✓ '${variant}' için ${results.size} sonuç bulundu")
                break
            }
        }

        // BRUTE-FORCE FALLBACK: try single first meaningful word
        // Skip common English/Turkish generic words that cause false positives from unrelated providers
        if (results.isEmpty()) {
            val GENERIC_WORDS = setOf(
                "attack", "titan", "season", "final", "the", "and", "from", "into", "with",
                "sezon", "bölüm", "film", "dizi", "izle", "part", "new", "world", "slayer",
                "shippuden", "naruto", "boruto", "piece", "clover", "academy", "academia",
                "kaisen", "hunter", "online", "game", "free", "live", "movie", "series",
                "turkce", "dublaj", "altyazi", "hd", "full", "tek", "parca", "anime"
            )
            val fallbackWord = title.split(Regex("\\s+"))
                .map { word ->
                    val cleaned = word.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]"), "").lowercase(Locale.ROOT)
                    Pair(word, cleaned)
                }
                .firstOrNull { (_, cleaned) -> cleaned.length >= 4 && cleaned !in GENERIC_WORDS }
                ?.let { (orig, _) ->
                    orig.replace(Regex("^[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]+|[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]+$"), "")
                }
            if (fallbackWord != null) {
                Log.d(TAG, "[${api.name}] Tek kelime fallback: '$fallbackWord'")
                results = safeSearch(api, fallbackWord)
                if (results.isNotEmpty()) searchedVariant = fallbackWord
            }
        }

        if (results.isEmpty()) {
            Log.w(TAG, "[${api.name}] ✗ ARAMA BAŞARISIZ: Hiçbir varyant sonuç döndürmedi. Site erişilemez veya CF korumalı.")
            Log.e(SERR, "❌ ARAMA SIFIR [${api.name}] — title='$title' S${season}E${episode} — Tüm ${titleVariants.size} varyant boş döndü. Site ölü/CF korumalı olabilir.")
            // CF korumalı site tespiti: son hata mesajını kontrol et
            val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
            if (lastErr != null && isCloudflareLikelyBlocking(lastErr)) {
                Log.e(SERR, "🔐 CLOUDFLARE BLOK [${api.name}] — Son hata: $lastErr")
                throw CloudflareBlockException(
                    "🔐 Cloudflare koruması tespit edildi. Doğrulama gerekiyor. (${api.name})"
                )
            }
            return emptyList()
        }

        Log.d(TAG, "[${api.name}] Arama sonuçları ('$searchedVariant' için ${results.size} adet):")
        results.take(5).forEachIndexed { i, r -> Log.d(TAG, "  [$i] '${r.name}' → ${r.url}") }

        // Find best match via title similarity first
        val bestMatch = findBestMatch(results, title, alternativeTitles, year, season, episode)

        // HIGH-CONFIDENCE SHORTCUT: if best match similarity is very high (>=0.85), trust it directly.
        // This prevents plugins like AnimeciX (which return null on early load() calls) from wasting
        // the entire timeout budget on serial candidate scanning and ID validation.
        val bestMatchScore = if (bestMatch != null) getBestTitleSimilarity(bestMatch.name, title, alternativeTitles) else 0.0
        val skipIdValidation = bestMatchScore >= 0.85
        if (skipIdValidation && bestMatch != null) {
            Log.d(TAG, "[ID-Mapping] ⚡ Yüksek güven skoru (${"%.2f".format(bestMatchScore)}) — ID doğrulaması atlanıyor, direkt '${bestMatch.name}' kullanılıyor.")
        }

        // Validate bestMatch using ID mapping if target IDs are available
        var validatedMatch: SearchResponse? = null
        var bestLoadResponse: LoadResponse? = null

        if (!skipIdValidation && resolvedIds != null && (targetImdb != null || targetTmdb != null || malId != null || aniListId != null || targetKitsu != null)) {
            Log.d(TAG, "[ID-Mapping] Validating candidates using resolved IDs: IMDb=$targetImdb TMDB=$targetTmdb MAL=$malId AniList=$aniListId Kitsu=$targetKitsu")
            
            var isSyncDataSupported = true

            // 1. Try validating the best match first
            if (bestMatch != null) {
                val resp = safeLoad(api, bestMatch.url)
                if (resp != null) {
                    isSyncDataSupported = hasSyncDataSupport(resp)
                    if (isSyncDataSupported) {
                        if (loadResponseMatches(resp, targetImdb, malId, aniListId, targetTmdb, targetKitsu)) {
                            Log.d(TAG, "[ID-Mapping] ✓ Best match '${bestMatch.name}' validated successfully via ID syncData.")
                            validatedMatch = bestMatch
                            bestLoadResponse = resp
                        } else {
                            Log.w(TAG, "[ID-Mapping] ✗ Best match '${bestMatch.name}' failed ID validation.")
                        }
                    } else {
                        // Eklentide syncData desteği yoksa, doğrudan bu sonucu eşleşme olarak kabul et (Türkçe eklentiler için fallback)
                        Log.d(TAG, "[ID-Mapping] ! '${bestMatch.name}' has no syncData support, falling back to title similarity.")
                        validatedMatch = bestMatch
                        bestLoadResponse = resp
                    }
                }
            }

            // 2. If best match failed ID validation (or was null), try other candidates (skip if WebView/slow plugin to prevent timeout)
            if (validatedMatch == null && !isWebViewPlugin && isSyncDataSupported) {
                // Sort candidates by simple similarity score so we check the most promising ones first
                val candidates = results.filter { it != bestMatch }
                    .map { r ->
                        val score = getBestTitleSimilarity(r.name, title, alternativeTitles)
                        Pair(r, score)
                    }
                    .sortedByDescending { it.second }
                    .take(3) // check top 3 alternatives at most to prevent high network overhead

                for ((candidate, score) in candidates) {
                    if (score < 0.10) continue // skip completely unrelated titles
                    Log.d(TAG, "[ID-Mapping] Checking candidate '${candidate.name}' (similarity: $score)...")
                    val resp = safeLoad(api, candidate.url)
                    if (resp != null) {
                        if (!hasSyncDataSupport(resp)) {
                            Log.d(TAG, "[ID-Mapping] Plugin ${api.name} does not support syncData. Using candidate directly.")
                            validatedMatch = candidate
                            bestLoadResponse = resp
                            break
                        }
                        if (loadResponseMatches(resp, targetImdb, malId, aniListId, targetTmdb, targetKitsu)) {
                            Log.d(TAG, "[ID-Mapping] ✓ Candidate '${candidate.name}' matches target IDs! Using it.")
                            validatedMatch = candidate
                            bestLoadResponse = resp
                            break
                        }
                    }
                }
            }
        }

        // Final match selection
        val finalMatch = validatedMatch ?: bestMatch
        if (finalMatch == null) {
            val first = results.firstOrNull() ?: return emptyList()
            Log.w(TAG, "[${api.name}] ✗ EŞLEŞTİRME BAŞARISIZ. İlk sonuç kullanılıyor: '${first.name}'")
            return loadAndExtractStreams(api, first, season, episode)
        }

        Log.d(TAG, "[${api.name}] ✓ Eşleşme: '${finalMatch.name}' → ${finalMatch.url}")
        
        // If we already loaded the correct LoadResponse, reuse it instead of reloading!
        return if (bestLoadResponse != null && finalMatch == validatedMatch) {
            val episodeData = findEpisodeData(bestLoadResponse, season, episode)
            if (episodeData == null) {
                Log.w(TAG, "[${api.name}] S${season}E${episode} bulunamadı.")
                emptyList()
            } else {
                extractStreamsFromEpisode(api, bestLoadResponse, episodeData)
            }
        } else {
            loadAndExtractStreams(api, finalMatch, season, episode)
        }
    }

    private suspend fun loadAndExtractStreams(
        api: MainAPI,
        match: SearchResponse,
        season: Int,
        episode: Int
    ): List<StreamSource> {
        val loadResponse = safeLoad(api, match.url) ?: run {
            Log.w(TAG, "[${api.name}] safeLoad null döndü: ${match.url}")
            return emptyList()
        }

        val episodeData = findEpisodeData(loadResponse, season, episode)
        if (episodeData == null) {
            Log.w(TAG, "[${api.name}] S${season}E${episode} bulunamadı. LoadResponse tipi: ${loadResponse.javaClass.simpleName}")
            return emptyList()
        }

        Log.d(TAG, "[${api.name}] S${season}E${episode} için episodeData bulundu")
        return extractStreamsFromEpisode(api, loadResponse, episodeData)
    }
    /**
     * URL'nin bir embed/iframe video oynatıcı sayfası olup olmadığını kontrol eder.
     * Doğrudan .mp4/.m3u8/.mpd dosyaları veya bilinen video akışları false döndürür.
     */
    internal fun isEmbedUrl(url: String): Boolean {
        val clean = resolveHrefLi(url).lowercase(Locale.ROOT)
        val isDirectMedia = clean.contains(".m3u8") ||
                            clean.contains(".mp4") ||
                            clean.contains(".mpd") ||
                            clean.contains(".mkv") ||
                            clean.contains(".avi") ||
                            clean.contains(".webm") ||
                            clean.contains("master.txt") ||
                            clean.contains("playlist.txt")
        if (isDirectMedia && !clean.contains("href.li/?")) {
            return false
        }
        return clean.contains("vk.com/video") ||
               clean.contains("vkvideo.ru") ||
               clean.contains("sibnet") ||
               clean.contains("vidmoly") ||
               clean.contains("filemoon") ||
               clean.contains("ok.ru") ||
               clean.contains("odnoklassniki") ||
               clean.contains("streamtape") ||
               clean.contains("streamwish") ||
               clean.contains("swdyu") ||
               clean.contains("sfastwish") ||
               clean.contains("wishfast") ||
               clean.contains("dood") ||
               clean.contains("ds2play") ||
               clean.contains("mixdrop") ||
               clean.contains("sendvid") ||
               clean.contains("voe.sx") ||
               clean.contains("embed") ||
               clean.contains("shell.php") ||
               clean.contains("video_ext.php") ||
               clean.contains("player.php") ||
               url.contains("href.li/?")
    }

    /**
     * href.li/?https://... şeklindeki URL'lerden gerçek hedef URL'yi çıkarır.
     */
    internal fun resolveHrefLi(url: String): String {
        if (url.contains("href.li/?", ignoreCase = true)) {
            val idx = url.indexOf("href.li/?")
            val target = url.substring(idx + "href.li/?".length)
            Log.d(TAG, "[HrefLi] href.li resolved: $url → $target")
            return target
        }
        return url
    }

    /**
     * Embed URL'yi (VK, Sibnet, Vidmoly, Filemoon, Okru, StreamWish vb.) CS3 loadExtractor sistemiyle çözer.
     * CS3 ExtractorApi'leri gerçek .mp4/.m3u8 URL'lerini ve gerekli HTTP başlıklarını döndürür.
     */
    internal suspend fun resolveEmbedUrl(
        providerName: String,
        rawUrl: String,
        rawLinkName: String,
        rawHeaders: Map<String, String>,
        referer: String,
        subtitleCallback: (SubtitleInput) -> Unit
    ): List<StreamSource> {
        val resolvedUrl = resolveHrefLi(rawUrl)
        Log.d(TAG, "[$providerName] Embed URL çözümleniyor: $resolvedUrl")

        val resolvedStreams = mutableListOf<StreamSource>()
        try {
            withTimeoutOrNull(20_000L) {
                loadExtractor(
                    url      = resolvedUrl,
                    referer  = referer,
                    subtitleCallback = { sub ->
                        subtitleCallback(
                            SubtitleInput(
                                url  = sub.url,
                                name = sub.lang,
                                lang = sub.langTag ?: detectLanguageCode(sub.lang)
                            )
                        )
                    },
                    callback = { link ->
                        Log.d(TAG, "[$providerName] Extractor çözümlendi → ${link.name}: ${link.url}")
                        val headers = mutableMapOf<String, String>()
                        if (link.headers.isNotEmpty()) {
                            headers.putAll(link.headers)
                        } else if (rawHeaders.isNotEmpty()) {
                            headers.putAll(rawHeaders)
                        }
                        if (!headers.keys.any { it.equals("referer", ignoreCase = true) }) {
                            if (link.referer.isNotBlank()) {
                                headers["Referer"] = link.referer
                            } else {
                                val linkHost = runCatching { java.net.URI(link.url).host }.getOrNull()
                                headers["Referer"] = if (!linkHost.isNullOrBlank()) "https://$linkHost/" else referer
                            }
                        }
                        if (!headers.keys.any { it.equals("user-agent", ignoreCase = true) }) {
                            headers["User-Agent"] = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
                        }
                        resolvedStreams.add(
                            StreamSource(
                                addonName      = providerName,
                                name           = "$providerName • ${link.name}",
                                title          = link.name,
                                url            = link.url,
                                infoHash       = null,
                                fileIndex      = null,
                                requestHeaders = headers,
                                isCS           = true,
                                quality        = getQualityString(link.quality),
                                qualityValue   = link.quality,
                                subtitles      = emptyList()
                            )
                        )
                    }
                )
            } ?: Log.w(TAG, "[$providerName] loadExtractor 20 saniyelik zaman aşımına uğradı.")
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "[$providerName] loadExtractor HATA: ${e.javaClass.simpleName}: ${e.message}")
        }
        if (resolvedStreams.isEmpty()) {
            Log.w(TAG, "[$providerName] Extractor doğrudan link bulamadı. Fallback Embed kaynağı ekleniyor: $rawUrl")
            val headers = rawHeaders.toMutableMap()
            if (!headers.keys.any { it.equals("referer", ignoreCase = true) }) {
                val rawHost = runCatching { java.net.URI(rawUrl).host }.getOrNull()
                headers["Referer"] = if (!rawHost.isNullOrBlank()) "https://$rawHost/" else referer
            }
            if (!headers.keys.any { it.equals("user-agent", ignoreCase = true) }) {
                headers["User-Agent"] = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
            }
            resolvedStreams.add(
                StreamSource(
                    addonName = providerName,
                    name = "$providerName • $rawLinkName (Embed)",
                    title = rawLinkName,
                    url = rawUrl,
                    infoHash = null,
                    fileIndex = null,
                    requestHeaders = headers,
                    isCS = true,
                    quality = "720p",
                    qualityValue = 720,
                    subtitles = emptyList()
                )
            )
        }
        return resolvedStreams
    }

    private suspend fun extractStreamsFromEpisode(
        api: MainAPI,
        loadResponse: LoadResponse,
        episodeData: String
    ): List<StreamSource> {
        val streams = mutableListOf<StreamSource>()
        val subtitleList = mutableListOf<SubtitleInput>()
        val pendingEmbedUrls = mutableListOf<Triple<String, String, Map<String, String>>>() // (rawUrl, linkName, headers)

        try {
            // Uses LOAD semaphore — completely separate from searchSemaphore, no deadlock risk
            loadSemaphore.withPermit {
                // Throttling: kısa gecikme Cloudflare tetiklenmesini önler
                kotlinx.coroutines.delay(500)
                Log.d(TAG, "[${api.name}] loadLinks çağrılıyor...")
                
                // Wrap the loadLinks in a timeout so that if it takes too long, we return the streams we already got
                withTimeoutOrNull(25_000L) {
                    api.loadLinks(
                        data = episodeData,
                        isCasting = false,
                        subtitleCallback = { subtitleFile ->
                            Log.d(TAG, "[${api.name}] Altyazı: ${subtitleFile.lang} → ${subtitleFile.url}")
                            subtitleList.add(
                                SubtitleInput(
                                    url = subtitleFile.url,
                                    name = subtitleFile.lang,
                                    lang = subtitleFile.langTag ?: detectLanguageCode(subtitleFile.lang)
                                )
                            )
                        },
                        callback = { link ->
                            val cleanUrl = resolveHrefLi(link.url)
                            Log.d(TAG, "[${api.name}] Link bulundu: ${link.name} → $cleanUrl")
                            if (isEmbedUrl(link.url)) {
                                Log.d(TAG, "[${api.name}] Embed URL tespit edildi — extractor kuyruğuna alınıyor: $cleanUrl")
                                pendingEmbedUrls.add(Triple(link.url, link.name, link.headers))
                            } else {
                                val headers = link.headers.toMutableMap()
                                val providerReferer = try { api.mainUrl } catch (_: Exception) { "https://google.com" }
                                if (!headers.keys.any { it.equals("referer", ignoreCase = true) }) {
                                    if (link.referer.isNotBlank()) {
                                        headers["Referer"] = link.referer
                                    } else {
                                        val urlHost = runCatching { java.net.URI(cleanUrl).host }.getOrNull()
                                        headers["Referer"] = if (!urlHost.isNullOrBlank()) "https://$urlHost/" else providerReferer
                                    }
                                }
                                if (!headers.keys.any { it.equals("user-agent", ignoreCase = true) }) {
                                    headers["User-Agent"] = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
                                }
                                streams.add(
                                    StreamSource(
                                        addonName = api.name,
                                        name = "${api.name} • ${link.name}",
                                        title = link.name,
                                        url = cleanUrl,
                                        infoHash = null,
                                        fileIndex = null,
                                        requestHeaders = headers,
                                        isCS = true,
                                        quality = getQualityString(link.quality),
                                        qualityValue = link.quality,
                                        subtitles = emptyList()
                                    )
                                )
                            }
                        }
                    )
                } ?: Log.w(TAG, "[${api.name}] loadLinks 25 saniyelik zaman aşımına uğradı, elde edilen ${streams.size} link döndürülüyor.")
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "[${api.name}] loadLinks HATA: ${e.javaClass.simpleName}: ${e.message}", e)
            Log.e(SERR, "💥 LOAD_LINKS CRASH [${api.name}] — ${e.javaClass.simpleName}: ${e.message}\n${android.util.Log.getStackTraceString(e)}")
        }

        // ── Embed URL'leri çözümleme aşaması (VK, Sibnet, Vidmoly, Filemoon, Okru vb.) ─────
        if (pendingEmbedUrls.isNotEmpty()) {
            Log.d(TAG, "[${api.name}] ${pendingEmbedUrls.size} embed URL'si loadExtractor ile çözümleniyor...")
            val referer = try { api.mainUrl } catch (_: Exception) { "https://google.com" }
            for ((rawUrl, linkName, rawHeaders) in pendingEmbedUrls) {
                val resolved = resolveEmbedUrl(
                    providerName    = api.name,
                    rawUrl          = rawUrl,
                    rawLinkName     = linkName,
                    rawHeaders      = rawHeaders,
                    referer         = referer,
                    subtitleCallback = { sub -> subtitleList.add(sub) }
                )
                if (resolved.isNotEmpty()) {
                    streams.addAll(resolved)
                } else {
                    val clean = resolveHrefLi(rawUrl)
                    if (!clean.contains("shell.php") && !clean.contains("video_ext.php") && !clean.contains("/embed/")) {
                        Log.w(TAG, "[${api.name}] Extractor fallback: raw URL doğrudan ekleniyor: $clean")
                        streams.add(
                            StreamSource(
                                addonName      = api.name,
                                name           = "${api.name} • $linkName",
                                title          = linkName,
                                url            = clean,
                                infoHash       = null,
                                fileIndex      = null,
                                requestHeaders = mapOf(
                                    "Referer"    to referer,
                                    "User-Agent" to com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
                                ),
                                isCS           = true,
                                quality        = "HD",
                                qualityValue   = 1080,
                                subtitles      = emptyList()
                            )
                        )
                    } else {
                        Log.w(TAG, "[${api.name}] Extractor çözemedi ve HTML iframe URL'si — oynatılamayacağı için atlandı: $clean")
                    }
                }
            }
        }

        Log.d(TAG, "[${api.name}] Sonuç: ${streams.size} stream, ${subtitleList.size} altyazı")
        return streams.map { stream ->
            stream.copy(subtitles = subtitleList.toList())
        }
    }

    // Delegated to CsLanguageDetector — see CsLanguageDetector.kt
    private fun detectLanguageCode(lang: String): String =
        CsLanguageDetector.detectLanguageCode(lang)
    private fun buildTitleVariants(main: String, alts: List<String>): List<String> =
        CsTitleMatcher.buildTitleVariants(main, alts)

    private suspend fun safeSearch(api: MainAPI, query: String): List<SearchResponse> {
        // Session bloklist kontrolü
        if (CsPluginStatusTracker.isBlocked(api.name)) {
            Log.w(TAG, "[${api.name}] safeSearch: Engellendi — atlanıyor.")
            return emptyList()
        }
        return searchSemaphore.withPermit {
            withTimeoutOrNull(25_000L) {
                try {
                // Hafif throttle — aggressive rate-limiting'i önler
                kotlinx.coroutines.delay(300)

                val providerName = api.name

                // TRanimaci runtime override: WebView JavaScript injection ile __NEXT_DATA__ JSON parse et.
                //
                // Sorun: OkHttp + CloudflareKiller yaklaşımı → proceedWithCookies sonrası yine WAF sayfası
                // dönüyor çünkü WebView cookie'leri OkHttp HTTP Header'larına doğru aktarılamıyor.
                //
                // Çözüm: WebViewResolver'ın script parametresini kullanarak arama sayfasını doğrudan
                // WebView içinde yüklüyoruz ve JavaScript ile document.getElementById('__NEXT_DATA__')
                // içeriğini okuyoruz. Tüm WAF challenge, cookie yönetimi ve sayfa yüklemesi aynı
                // WebView session'ında gerçekleşiyor — cookie aktarım sorunu yok.
                if (providerName.equals("TrAnimeci", ignoreCase = true)) {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20")
                    val searchUrl = "${api.mainUrl}/ara?q=$encodedQuery"
                    
                    Log.d(TAG, "[$providerName] WebView JS injection ile arama başlatılıyor: $searchUrl")
                    
                    try {
                        // WebView'dan dönen __NEXT_DATA__ JSON'unu tutacak container
                        val nextDataHolder = java.util.concurrent.atomic.AtomicReference<String?>(null)
                        val latch = java.util.concurrent.CountDownLatch(1)
                        
                        // JavaScript: Next.js App Router → DOM scraping
                        // Site CSR veya SSR kullanabilir. Arama sayfasındaki /anime/ linklerini toplayıp döndürüyoruz.
                        val encodedQueryForJs = query.replace("\"", "\\\"").replace("'", "\\'")
                        val jsScript = """
                            (function() {
                                try {
                                    var searchQuery = '$encodedQueryForJs';
                                    var results = [];
                                    var seen = {};
                                    // Sayfadaki tüm /anime/ linklerini bul
                                    var links = document.querySelectorAll('a[href*="/anime/"]');
                                    for (var i = 0; i < links.length && results.length < 40; i++) {
                                        var a = links[i];
                                        var href = a.href || a.getAttribute('href') || '';
                                        if (!href) continue;
                                        // Absolute path'e çevir
                                        if (href.startsWith('/')) {
                                            href = window.location.origin + href;
                                        }
                                        if (seen[href]) continue;
                                        seen[href] = 1;
                                        
                                        // Başlığı bul (title attribute, textContent veya h2/h3/h4/span içindeki metin)
                                        var title = (a.getAttribute('title') || '').trim();
                                        if (!title) {
                                            var header = a.querySelector('h1, h2, h3, h4, h5, h6, p, span, div');
                                            title = header ? header.textContent.trim() : a.textContent.trim();
                                        }
                                        title = title.replace(/\s+/g, ' ').trim();
                                        if (!title || title.length < 2) continue;
                                        
                                        // Görseli bul
                                        var img = a.querySelector('img');
                                        var poster = img ? (img.src || img.getAttribute('data-src') || img.getAttribute('lazy-src') || '') : '';
                                        
                                        results.push({n: title, u: href, p: poster});
                                    }
                                    
                                    if (results.length > 0) {
                                        return 'DOM:' + JSON.stringify(results);
                                    }
                                    
                                    var hasQuery = document.body.innerHTML.toLowerCase().indexOf(searchQuery.toLowerCase()) !== -1;
                                    var matchingTags = [];
                                    if (hasQuery) {
                                        var allElements = document.body.getElementsByTagName('*');
                                        for (var k = 0; k < allElements.length && matchingTags.length < 10; k++) {
                                            var elText = allElements[k].textContent || '';
                                            if (elText.toLowerCase().indexOf(searchQuery.toLowerCase()) !== -1 && allElements[k].children.length === 0) {
                                                matchingTags.push(allElements[k].tagName + ':' + elText.substring(0, 100).trim());
                                            }
                                        }
                                    }
                                    
                                    var debugLinks = [];
                                    var allLinks = document.querySelectorAll('a');
                                    for (var j = 0; j < Math.min(allLinks.length, 12); j++) {
                                        debugLinks.push(allLinks[j].outerHTML);
                                    }
                                    return 'NOT_FOUND:' + document.title + ':HTML_LEN:' + document.body.innerHTML.length + ':HAS_QUERY:' + hasQuery + ':TAGS:' + JSON.stringify(matchingTags) + ':LINKS:' + JSON.stringify(debugLinks);
                                } catch(e) {
                                    return 'JS_ERROR:' + e.message;
                                }
                            })()
                        """.trimIndent()
                        
                        // Main thread'de WebView çalıştır (Android WebView main thread zorunluluğu)
                        // resolveUsingWebView suspending fonksiyon: WebView timeout (60s) sonunda dönüyor.
                        // scriptCallback her resource yüklenince çalışıyor — en son değeri tutuyoruz.
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            try {
                                com.lagradost.cloudstream3.network.WebViewResolver(
                                    interceptUrl = Regex(".^"),  // Hiçbir URL intercept etme
                                    additionalUrls = listOf(Regex(".")),
                                    userAgent = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT,
                                    useOkhttp = false,
                                    script = jsScript,
                                    scriptCallback = { result ->
                                        if (result.startsWith("DOM:")) {
                                            Log.d("TR_WV", "[$providerName] WebView JS DOM sonucu alındı: $result")
                                        } else {
                                            Log.d("TR_WV", "[$providerName] WebView JS sonucu alındı (${result.length} karakter): ${result.take(2000)}")
                                        }
                                        val prev = nextDataHolder.get()
                                        val prevLen = prev?.substringAfter("HTML_LEN:")?.substringBefore(":")?.toIntOrNull() ?: -1
                                        val currLen = result.substringAfter("HTML_LEN:")?.substringBefore(":")?.toIntOrNull() ?: -1
                                        
                                        if (result.startsWith("DOM:") || (prev == null || !prev.startsWith("DOM:")) && currLen >= prevLen) {
                                            nextDataHolder.set(result)
                                        }
                                        if (result.startsWith("DOM:")) {
                                            latch.countDown()
                                        }
                                    }
                                ).resolveUsingWebView(searchUrl) { false }
                            } catch (e: Exception) {
                                Log.e(TAG, "[$providerName] WebView resolve hatası: ${e.message}")
                            }
                            // WebView bitti — latch'i düşür (eğer heniz düşürmediyse)
                            latch.countDown()
                        }
                        
                        // WebView'ın tamamlanmasını bekle (maks 45 saniye — WAF PoW çözümü için yeterli)
                        val completed = latch.await(45, java.util.concurrent.TimeUnit.SECONDS)
                        if (!completed) {
                            Log.w(TAG, "[$providerName] WebView 45s içinde tamamlanmadı — boş sonuç dönülüyor")
                            return@withTimeoutOrNull emptyList()
                        }
                        
                        val rawResult = nextDataHolder.get()
                        val jsResult = rawResult?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"")
                        Log.d("TR_WV", "[$providerName] JS sonuç preview: ${jsResult?.take(500)}")
                        
                        if (jsResult.isNullOrBlank() || jsResult.startsWith("NOT_FOUND:") || jsResult.startsWith("JS_ERROR:") || jsResult == "null") {
                            Log.w(TAG, "[$providerName] DOM scraping başarısız: $jsResult")
                            return@withTimeoutOrNull emptyList()
                        }
                        
                        // Handle DOM scraping results (DOM: prefix)
                        if (jsResult.startsWith("DOM:")) {
                            val domJson = jsResult.removePrefix("DOM:")
                            Log.d("TR_WV", "[$providerName] DOM sonuçları parse ediliyor...")
                            val parsed = mutableListOf<com.lagradost.cloudstream3.SearchResponse>()
                            try {
                                val arr = org.json.JSONArray(domJson)
                                Log.d("TR_WV", "[$providerName] DOM array length=${arr.length()}")
                                for (i in 0 until arr.length()) {
                                    val item = arr.optJSONObject(i) ?: continue
                                    val name = item.optString("n").takeIf { it.isNotBlank() } ?: continue
                                    val itemUrl = item.optString("u").takeIf { it.isNotBlank() } ?: continue
                                    val poster = item.optString("p").takeIf { it.isNotBlank() }
                                    val sr = try {
                                        val cls = Class.forName("com.lagradost.cloudstream3.AnimeSearchResponse")
                                        val ctor = cls.constructors.firstOrNull { it.parameterCount >= 3 } ?: continue
                                        val args = Array<Any?>(ctor.parameterCount) { null }
                                        args[0] = name; args[1] = itemUrl; args[2] = api.name
                                        if (ctor.parameterCount > 3) args[3] = com.lagradost.cloudstream3.TvType.Anime
                                        if (ctor.parameterCount > 4) args[4] = poster
                                        ctor.newInstance(*args) as? com.lagradost.cloudstream3.SearchResponse
                                    } catch (ex: Exception) { null }
                                    if (sr != null) parsed.add(sr)
                                }
                            } catch (ex: Exception) {
                                Log.w(TAG, "[$providerName] DOM JSON parse hatası: ${ex.message}")
                            }
                            Log.d(TAG, "[$providerName] DOM scraping '$query' → ${parsed.size} sonuç")
                            return@withTimeoutOrNull parsed
                        }
                        
                        // nextDataJson = __NEXT_DATA__ JSON (Pages Router legacy path)
                        val nextDataJson = jsResult
                        
                        // JSON parse et ve sonuçları çıkar
                        val parsed = mutableListOf<com.lagradost.cloudstream3.SearchResponse>()
                        try {
                            val root = org.json.JSONObject(nextDataJson)
                            val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
                            Log.d("TR_WV", "[$providerName] pageProps keys: ${pageProps?.keys()?.asSequence()?.toList()}")
                            
                            // Tüm olası arama sonuç array key'lerini dene
                            val animeArray = pageProps?.optJSONArray("animes")
                                ?: pageProps?.optJSONArray("results")
                                ?: pageProps?.optJSONArray("data")
                                ?: pageProps?.optJSONArray("searchResults")
                                ?: pageProps?.optJSONArray("items")
                                ?: pageProps?.optJSONArray("anime")
                                ?: pageProps?.optJSONArray("list")
                            
                            if (animeArray != null) {
                                Log.d("TR_WV", "[$providerName] animeArray length=${animeArray.length()}")
                                for (i in 0 until animeArray.length()) {
                                    val item = animeArray.optJSONObject(i) ?: continue
                                    val name = item.optString("name").takeIf { it.isNotBlank() }
                                        ?: item.optString("title").takeIf { it.isNotBlank() }
                                        ?: item.optString("adi").takeIf { it.isNotBlank() }
                                        ?: item.optString("anime_adi").takeIf { it.isNotBlank() }
                                        ?: continue
                                    val slug = item.optString("slug").takeIf { it.isNotBlank() }
                                        ?: item.optString("url").takeIf { it.isNotBlank() }
                                        ?: item.optString("id").takeIf { it.isNotBlank() }
                                        ?: continue
                                    val itemUrl = if (slug.startsWith("http")) slug else "${api.mainUrl}/anime/$slug"
                                    val poster = item.optString("poster").takeIf { it.isNotBlank() }
                                        ?: item.optString("image").takeIf { it.isNotBlank() }
                                        ?: item.optString("img").takeIf { it.isNotBlank() }
                                        ?: item.optString("kapak").takeIf { it.isNotBlank() }
                                    
                                    val searchResponse = try {
                                        val cls = Class.forName("com.lagradost.cloudstream3.AnimeSearchResponse")
                                        val ctor = cls.constructors.firstOrNull { it.parameterCount >= 3 } ?: continue
                                        val args = Array<Any?>(ctor.parameterCount) { null }
                                        args[0] = name; args[1] = itemUrl; args[2] = api.name
                                        if (ctor.parameterCount > 3) args[3] = com.lagradost.cloudstream3.TvType.Anime
                                        if (ctor.parameterCount > 4) args[4] = poster
                                        ctor.newInstance(*args) as? com.lagradost.cloudstream3.SearchResponse
                                    } catch (refEx: Exception) {
                                        Log.w(TAG, "[$providerName] Reflection create failed: ${refEx.message}"); null
                                    }
                                    if (searchResponse != null) parsed.add(searchResponse)
                                }
                            } else {
                                // pageProps direkt bir item mi? (tek sonuç sayfası)
                                Log.d("TR_WV", "[$providerName] animeArray null — pageProps raw (1KB): ${pageProps?.toString()?.take(1000)}")
                            }
                        } catch (jsonEx: Exception) {
                            Log.w(TAG, "[$providerName] __NEXT_DATA__ JSON parse hatası: ${jsonEx.message}")
                            Log.d("TR_WV", "[$providerName] Ham JSON (1KB): ${nextDataJson.take(1000)}")
                        }
                        
                        Log.d(TAG, "[$providerName] WebView JS search '$query' → ${parsed.size} sonuç")
                        return@withTimeoutOrNull parsed
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "[$providerName] WebView JS search HATA: ${e.message}", e)
                        return@withTimeoutOrNull emptyList()
                    }
                }
                
                // Encode the search query if the provider requires path/custom API queries
                val isTurkishPathSearch = providerName.contains("AnimeciX", ignoreCase = true) ||
                                          providerName.contains("Animely", ignoreCase = true) ||
                                          providerName.contains("Elysium", ignoreCase = true)
                val finalQuery = if (isTurkishPathSearch) {
                    encodePathQuery(query)
                } else {
                    query
                }

                var results: List<SearchResponse>? = null

                // 1. Try paginated search if overridden and not marked unsupported
                val supportsPaginated = isMethodOverridden(api, "search", String::class.java, Int::class.javaPrimitiveType ?: Int::class.java, kotlin.coroutines.Continuation::class.java)
                if (supportsPaginated && !unsupportedMethods.contains("$providerName:search_paginated")) {
                    try {
                        Log.d(TAG, "[$providerName] api.search('$finalQuery', 1) çağrılıyor...")
                        results = api.search(finalQuery, 1)?.items
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        val isNotImpl = t is kotlin.NotImplementedError || t.message?.contains("NotImplementedError", ignoreCase = true) == true
                        if (isNotImpl) {
                            unsupportedMethods.add("$providerName:search_paginated")
                            Log.d(TAG, "[$providerName] search(query, 1) desteklenmiyor (NotImplementedError).")
                        } else {
                            CsPluginStatusTracker.recordFailure(providerName, t)
                            Log.w(TAG, "[$providerName] search(query, 1) hata verdi: ${t.message}")
                            Log.e(SERR, "❌ SEARCH(q,1) HATA [$providerName] — ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                }

                // 2. Try standard search if overridden and not marked unsupported
                val supportsStandard = isMethodOverridden(api, "search", String::class.java, kotlin.coroutines.Continuation::class.java)
                if (results == null && supportsStandard && !unsupportedMethods.contains("$providerName:search")) {
                    try {
                        Log.d(TAG, "[$providerName] api.search('$finalQuery') çağrılıyor...")
                        results = api.search(finalQuery)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        val isNotImpl = t is kotlin.NotImplementedError || t.message?.contains("NotImplementedError", ignoreCase = true) == true
                        if (isNotImpl) {
                            unsupportedMethods.add("$providerName:search")
                            Log.d(TAG, "[$providerName] search(query) desteklenmiyor (NotImplementedError).")
                        } else {
                            CsPluginStatusTracker.recordFailure(providerName, t)
                            Log.w(TAG, "[$providerName] search(query) hata verdi: ${t.message}")
                            Log.e(SERR, "❌ SEARCH(q) HATA [$providerName] — ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                }

                // 3. Try quickSearch if overridden and not marked unsupported
                val supportsQuick = isMethodOverridden(api, "quickSearch", String::class.java, kotlin.coroutines.Continuation::class.java)
                if (results == null && supportsQuick && !unsupportedMethods.contains("$providerName:quickSearch")) {
                    try {
                        Log.d(TAG, "[$providerName] api.quickSearch('$finalQuery') çağrılıyor...")
                        results = api.quickSearch(finalQuery)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        val isNotImpl = t is kotlin.NotImplementedError || t.message?.contains("NotImplementedError", ignoreCase = true) == true
                        if (isNotImpl) {
                            unsupportedMethods.add("$providerName:quickSearch")
                            Log.d(TAG, "[$providerName] quickSearch(query) desteklenmiyor (NotImplementedError).")
                        } else {
                            CsPluginStatusTracker.recordFailure(providerName, t)
                            Log.w(TAG, "[$providerName] quickSearch(query) hata verdi: ${t.message}")
                            Log.e(SERR, "❌ QUICK_SEARCH HATA [$providerName] — ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                }

                // 4. Fallback if reflection returned false for all methods but we haven't succeeded
                if (results == null && !supportsPaginated && !supportsStandard && !supportsQuick) {
                    Log.d(TAG, "[$providerName] Metot tespiti yapılamadı, tüm varyantlar sırayla deneniyor...")
                    try {
                        results = api.search(finalQuery, 1)?.items
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        val isNotImpl = t is kotlin.NotImplementedError || t.message?.contains("NotImplementedError", ignoreCase = true) == true
                        if (!isNotImpl) CsPluginStatusTracker.recordFailure(providerName, t)
                        try {
                            results = api.search(finalQuery)
                        } catch (cancel2: kotlinx.coroutines.CancellationException) {
                            throw cancel2
                        } catch (t2: Throwable) {
                            val isNotImpl2 = t2 is kotlin.NotImplementedError || t2.message?.contains("NotImplementedError", ignoreCase = true) == true
                            if (!isNotImpl2) CsPluginStatusTracker.recordFailure(providerName, t2)
                            try {
                                results = api.quickSearch(finalQuery)
                            } catch (cancel3: kotlinx.coroutines.CancellationException) {
                                throw cancel3
                            } catch (t3: Throwable) {
                                val isNotImpl3 = t3 is kotlin.NotImplementedError || t3.message?.contains("NotImplementedError", ignoreCase = true) == true
                                if (!isNotImpl3) CsPluginStatusTracker.recordFailure(providerName, t3)
                            }
                        }
                    }
                }

                val finalResults = results ?: emptyList()
                if (finalResults.isEmpty()) {
                    Log.d(TAG, "[$providerName] search('$query') → 0 sonuç")
                } else {
                    Log.d(TAG, "[$providerName] search('$query') → ${finalResults.size} sonuç")
                }
                finalResults
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel // always rethrow so coroutine scope can cancel properly
            } catch (e: Throwable) {
                // Catches both Exception AND Error subclasses (including NotImplementedError)
                CsPluginStatusTracker.recordFailure(api.name, e)
                Log.e(TAG, "[${api.name}] search('$query') HATA: ${e.javaClass.simpleName}: ${e.message}")
                Log.e(SERR, "💥 SEARCH CRASH [${api.name}] query='$query' — ${e.javaClass.simpleName}: ${e.message}\n${android.util.Log.getStackTraceString(e)}")
                emptyList()
            }
            } ?: emptyList()
        }
    }

    private suspend fun safeLoad(api: MainAPI, url: String): LoadResponse? {
        // Session bloklist kontrolü
        if (CsPluginStatusTracker.isBlocked(api.name)) {
            Log.w(TAG, "[${api.name}] safeLoad: Engellendi — atlanıyor.")
            return null
        }
        return loadSemaphore.withPermit {
            withTimeoutOrNull(25_000L) {
                try {
                kotlinx.coroutines.delay(300)
                Log.d(TAG, "[${api.name}] load('$url') çağrılıyor...")
                val resp = api.load(url)
                Log.d(TAG, "[${api.name}] load tamamlandı: ${resp?.javaClass?.simpleName}")
                resp
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Throwable) {
                // Catches both Exception AND Error subclasses (including NotImplementedError)
                CsPluginStatusTracker.recordFailure(api.name, e)
                Log.e(TAG, "[${api.name}] load('$url') HATA: ${e.javaClass.simpleName}: ${e.message}", e)
                Log.e(SERR, "💥 LOAD CRASH [${api.name}] url='$url' — ${e.javaClass.simpleName}: ${e.message}\n${android.util.Log.getStackTraceString(e)}")
                null
            }
            }  // withTimeoutOrNull
        }  // withPermit
    }

    private fun hasSyncDataSupport(resp: LoadResponse): Boolean {
        return try {
            resp.javaClass.getDeclaredField("syncData")
            true
        } catch (_: Exception) {
            try {
                resp.javaClass.getMethod("getSyncData")
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun loadResponseMatches(
        resp: LoadResponse?,
        targetImdb: String?,
        targetMal: Int?,
        targetAniList: Int?,
        targetTmdb: Int?,
        targetKitsu: Int?
    ): Boolean {
        if (resp == null) return false
        
        // Use reflection to access syncData from LoadResponse if available
        val syncData = try {
            val field = resp.javaClass.getDeclaredField("syncData")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            field.get(resp) as? Map<String, String>
        } catch (e: Exception) {
            try {
                val method = resp.javaClass.getMethod("getSyncData")
                @Suppress("UNCHECKED_CAST")
                method.invoke(resp) as? Map<String, String>
            } catch (e2: Exception) {
                null
            }
        } ?: return false

        Log.d(TAG, "[ID-Mapping] Candidate syncData: $syncData")

        // Parse any JSON string values in syncData to support nested mappings (e.g. {"" -> "{\"Tmdb\":\"209867\"}"})
        val mergedSyncData = syncData.toMutableMap()
        for ((key, value) in syncData) {
            val trimmed = value.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    val json = org.json.JSONObject(trimmed)
                    for (jsonKey in json.keys()) {
                        mergedSyncData[jsonKey.lowercase(Locale.ROOT)] = json.optString(jsonKey)
                    }
                } catch (_: Exception) {}
            }
        }

        // Check TMDB ID
        val candidateTmdb = mergedSyncData["tmdb"]?.toIntOrNull()
        if (candidateTmdb != null && targetTmdb != null) {
            if (candidateTmdb == targetTmdb) return true
        }

        // Check MAL ID
        val candidateMal = (mergedSyncData["mal"] ?: mergedSyncData["myanimelist"])?.toIntOrNull()
        if (candidateMal != null && targetMal != null) {
            if (candidateMal == targetMal) return true
        }

        // Check AniList ID
        val candidateAniList = mergedSyncData["anilist"]?.toIntOrNull()
        if (candidateAniList != null && targetAniList != null) {
            if (candidateAniList == targetAniList) return true
        }

        // Check Kitsu ID
        val candidateKitsu = mergedSyncData["kitsu"]?.toIntOrNull()
        if (candidateKitsu != null && targetKitsu != null) {
            if (candidateKitsu == targetKitsu) return true
        }

        // Check IMDb ID
        val candidateImdb = mergedSyncData["imdb"]
        if (!candidateImdb.isNullOrBlank() && !targetImdb.isNullOrBlank()) {
            if (candidateImdb.trim().equals(targetImdb.trim(), ignoreCase = true)) return true
        }

        return false
    }

    // Delegated to CsTitleMatcher — see CsTitleMatcher.kt
    private fun getBestTitleSimilarity(candidateName: String, mainTitle: String, altTitles: List<String>): Double =
        CsTitleMatcher.getBestTitleSimilarity(candidateName, mainTitle, altTitles)

    /**
     * HTTP yanıt kodu veya hata mesajından Cloudflare/ağ engelinin olup olmadığını tahmin eder.
     * Tier 2 eklentiler (TurkAnime, Dizilla, FilmMakinesi gibi) CF korumalı sitelere bağlanır.
     *
     * @param errorMsg Önceki istekte yakalanan hata mesajı veya exception açıklaması
     */
    // Delegated to CsTitleMatcher — see CsTitleMatcher.kt
    private fun isCloudflareLikelyBlocking(errorMsg: String): Boolean {
        val lower = errorMsg.lowercase(Locale.ROOT)
        return lower.contains("403") ||
               lower.contains("503") ||
               lower.contains("cloudflare") ||
               lower.contains("challenge") ||
               lower.contains("cf-ray") ||
               lower.contains("just a moment") ||
               lower.contains("connection refused") ||
               lower.contains("timeout") ||
               lower.contains("ssl handshake") ||
               lower.contains("unable to resolve host")
    }

    // Delegated to CsTitleMatcher — see CsTitleMatcher.kt
    private fun findBestMatch(
        results: List<SearchResponse>,
        mainTitle: String,
        altTitles: List<String>,
        targetYear: Int?,
        targetSeason: Int? = null,
        targetEpisode: Int? = null
    ): SearchResponse? = CsTitleMatcher.findBestMatch(results, mainTitle, altTitles, targetYear, targetSeason, targetEpisode)

    // ─── Episode extraction helpers — Delegated to CsEpisodeMatcher ──────────

    // Delegated to CsEpisodeMatcher — see CsEpisodeMatcher.kt
    private fun findEpisodeData(response: LoadResponse, season: Int, episode: Int): String? =
        CsEpisodeMatcher.findEpisodeData(response, season, episode)

    private fun getQualityString(quality: Int): String = when (quality) {
        4000, 2160 -> "4K"
        1080 -> "1080p"
        720 -> "720p"
        480 -> "480p"
        360 -> "360p"
        else -> if (quality > 0) "${quality}p" else "HD"
    }
}
