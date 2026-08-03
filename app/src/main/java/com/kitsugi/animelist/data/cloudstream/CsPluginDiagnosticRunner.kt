package com.kitsugi.animelist.data.cloudstream

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-app CS eklenti tanı motoru.
 *
 * CsDeepStreamTest (instrumented test) ile aynı E2E mantığını uygulama içinde çalıştırır.
 * Avantajı: Uygulama açıkken çalıştığı için CloudflareKiller aktif session cookie'lerini
 * kullanır → CF/WAF korumalı 160 eklentinin büyük çoğunluğu engeli geçer.
 *
 * Kullanım:
 *   CsPluginDiagnosticRunner.startDiagnostic(context)
 *   CsPluginDiagnosticRunner.progress.collect { ... }
 */
object CsPluginDiagnosticRunner {

    private const val TAG = "CsPluginDiagnostic"
    private const val MAX_CONCURRENT = 4  // Uygulama içi — biraz daha conservative

    /** Türkçe CS eklenti depoları */
    val REPOS = listOf(
        "https://raw.githubusercontent.com/gameras1010-afk/Kitsugi-Plugins/builds/repo.json",
        "https://raw.githubusercontent.com/feroxx/Kekik-cloudstream/refs/heads/builds/repo.json",
        "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/refs/heads/master/repo.json",
        "https://raw.githubusercontent.com/Kraptor123/Cs-Karma/refs/heads/master/repo.json",
        "https://raw.githubusercontent.com/nikyokki/nik-cloudstream/master/repo.json",
        "https://raw.githubusercontent.com/ByAyzen/AyzenCS3/refs/heads/builds/repo.json",
        "https://raw.githubusercontent.com/Kraptor123/cs-kekikanime/master/repo.json",
        "https://raw.githubusercontent.com/sarapcanagii/Pitipitii/master/repo.json",
        "https://raw.githubusercontent.com/Kraptor123/Cs-GizliKeyif/refs/heads/master/repo.json",
        "https://raw.githubusercontent.com/Sertel392/Makotogecici/main/repo.json",
        "https://raw.githubusercontent.com/caca1403/cloudstream-cagi-eklenti/main/repo.json"
    )

    // ─── State ────────────────────────────────────────────────────────────────

    private val _progress  = MutableStateFlow<DiagnosticProgress?>(null)
    private val _results   = MutableStateFlow<List<DiagnosticResult>>(emptyList())
    private val _isRunning = MutableStateFlow(false)
    private val _reportPath = MutableStateFlow<String?>(null)

    val progress:   StateFlow<DiagnosticProgress?>    = _progress.asStateFlow()
    val results:    StateFlow<List<DiagnosticResult>> = _results.asStateFlow()
    val isRunning:  StateFlow<Boolean>                = _isRunning.asStateFlow()
    val reportPath: StateFlow<String?>                = _reportPath.asStateFlow()

    private val diagnosticEmbedResults = java.util.concurrent.ConcurrentHashMap<String, MutableList<EmbedResult>>()

    @Volatile
    private var cancelled = false

    // ─── Data classes ─────────────────────────────────────────────────────────

    data class DiagnosticProgress(
        val current: Int,
        val total:   Int,
        val currentPlugin: String,
        val phase: String
    ) {
        val fraction get() = if (total > 0) current.toFloat() / total else 0f
    }

    /** Her test fazında yakalanan hata kaydı */
    data class PhaseError(
        val phase:         String,   // "download" | "load" | "search" | "detail" | "episode" | "stream" | "embed"
        val errorClass:    String,   // exception sınıfı adı
        val message:       String,   // hata mesajı
        val httpCode:      Int?,     // HTTP hata kodu (403, 429, 503 vb.) — yoksa null
        val isCfPattern:   Boolean,  // Cloudflare/WAF imzası tespit edildi mi
        val isDdosGuard:   Boolean,  // DDoS-Guard imzası var mı
        val isTimeout:     Boolean,  // timeout mı
        val isNetworkFail: Boolean,  // genel ağ hatası mı (UnknownHost, ConnectException vb.)
        val stackSnippet:  String?   // ilk 3 satır stack trace
    )

    /** Tek bir embed/video kaynağı için sonuç */
    data class EmbedResult(
        val sourceUrl:   String,   // embed URL'si
        val sourceName:  String,   // host adı (Doodstream, Filemoon vb.)
        val resolved:    Boolean,  // gerçek video URL'si çıkarıldı mı
        val resolvedUrl: String?,  // çıkarılan URL
        val error:       String?   // hata varsa
    )

    data class DiagnosticResult(
        val pluginId:      String,
        val repoSlug:      String,
        val displayName:   String,
        val mainUrl:       String,           // plugin'in mainUrl'si
        val downloaded:    Boolean,
        val loaded:        Boolean,
        val apiCount:      Int,              // yüklenen API sayısı
        val searchQuery:   String,
        val searchCount:   Int,
        val topSearchHit:  String?,          // ilk arama sonucunun adı
        val loadOk:        Boolean,
        val loadedTitle:   String?,          // load() sonucu dönen içerik adı
        val episodeFound:  Boolean,          // findEpisodeData başarılı mı
        val streamCount:   Int,
        val streamUrls:    List<String>,
        val embedResults:  List<EmbedResult>, // embed kaynak bazlı sonuçlar
        val phaseErrors:   List<PhaseError>,  // fazlar bazında tüm hatalar
        val error:         String?           // genel/son hata özeti
    ) {
        val status: ResultStatus get() = when {
            !downloaded || !loaded -> ResultStatus.DEAD
            searchCount == 0       -> {
                if (hasCfBlock || hasDdosGuard) {
                    ResultStatus.CF_BLOCKED
                } else if (hasNetworkFail || hasTimeout) {
                    ResultStatus.DEAD
                } else {
                    ResultStatus.LOAD_FAILED
                }
            }
            !loadOk                -> ResultStatus.LOAD_FAILED
            streamCount == 0       -> ResultStatus.NO_STREAMS
            else                   -> ResultStatus.WORKING
        }

        /** CF/WAF engeli var mı — herhangi bir fazda tespit edilmişse true */
        val hasCfBlock: Boolean get() = phaseErrors.any { it.isCfPattern }
        val hasDdosGuard: Boolean get() = phaseErrors.any { it.isDdosGuard }
        val hasTimeout: Boolean get() = phaseErrors.any { it.isTimeout }
        val hasNetworkFail: Boolean get() = phaseErrors.any { it.isNetworkFail }
    }

    enum class ResultStatus {
        WORKING, NO_STREAMS, CF_BLOCKED, LOAD_FAILED, DEAD
    }

    // ─── Hata Analiz Yardımcıları ─────────────────────────────────────────────

    /** Cloudflare / WAF imzalarını string içinde arar */
    private fun isCfPattern(msg: String): Boolean {
        val m = msg.lowercase()
        return m.contains("cloudflare") ||
               m.contains("cf-ray") ||
               m.contains("cf_clearance") ||
               m.contains("turnstile") ||
               m.contains("just a moment") ||
               m.contains("challenge-platform") ||
               m.contains("403") && (m.contains("forbidden") || m.contains("blocked")) ||
               m.contains("captcha") ||
               m.contains("security check") ||
               m.contains("access denied")
    }

    /** DDoS-Guard imzasını kontrol eder */
    private fun isDdosGuard(msg: String): Boolean {
        val m = msg.lowercase()
        return m.contains("ddos-guard") ||
               m.contains("ddosguard") ||
               m.contains("d-d-o-s") ||
               m.contains("anti-ddos")
    }

    /** Hata mesajından HTTP durum kodunu çıkarmaya çalışır */
    private fun extractHttpCode(t: Throwable): Int? {
        val msg = t.message ?: ""
        // OkHttp "HTTP 403 Forbidden" formatı
        val httpMatch = Regex("\\bHTTP (\\d{3})\\b").find(msg)
            ?: Regex("\\b(\\d{3}) ").find(msg)
        return httpMatch?.groupValues?.get(1)?.toIntOrNull()
    }

    /** Ağ bağlantı hatası mı (DNS, connection refused, ssl, socket) */
    private fun isNetworkFail(t: Throwable): Boolean {
        val cls = t.javaClass.simpleName.lowercase()
        val msg = (t.message ?: "").lowercase()
        return cls.contains("unknownhost") ||
               cls.contains("connectexception") ||
               cls.contains("socketexception") ||
               cls.contains("socketimeout") ||
               cls.contains("sslexception") ||
               cls.contains("sslerror") ||
               msg.contains("failed to connect") ||
               msg.contains("unable to resolve host") ||
               msg.contains("connection refused") ||
               msg.contains("econnrefused") ||
               msg.contains("enetunreach")
    }

    /** Timeout hatası mı */
    private fun isTimeout(t: Throwable): Boolean {
        val cls = t.javaClass.simpleName.lowercase()
        val msg = (t.message ?: "").lowercase()
        return cls.contains("timeout") ||
               cls.contains("timedout") ||
               msg.contains("timeout") ||
               msg.contains("timed out") ||
               msg.contains("deadline exceeded")
    }

    /** Throwable'dan PhaseError oluşturur */
    private fun buildPhaseError(phase: String, t: Throwable): PhaseError {
        val resource = throwAbleToResource<Any>(t)
        val (isNetwork, errorString) = when (resource) {
            is Resource.Failure -> resource.isNetworkError to resource.errorString
            else -> false to (t.message ?: t.javaClass.simpleName)
        }
        val msg = t.message ?: t.javaClass.simpleName
        val stackSnippet = t.getStackTracePretty()
        return PhaseError(
            phase         = phase,
            errorClass    = t.javaClass.simpleName,
            message       = errorString,
            httpCode      = extractHttpCode(t),
            isCfPattern   = isCfPattern(msg) || isCfPattern(t.javaClass.name) || isCfPattern(errorString),
            isDdosGuard   = isDdosGuard(msg) || isDdosGuard(errorString),
            isTimeout     = isTimeout(t) || (resource is Resource.Failure && resource.isNetworkError && errorString.contains("Timeout", ignoreCase = true)),
            isNetworkFail = isNetworkFail(t) || isNetwork,
            stackSnippet  = stackSnippet
        )
    }

    /** String mesajdan PhaseError oluşturur (exception olmadan) */
    private fun buildPhaseErrorFromMsg(phase: String, msg: String): PhaseError {
        return PhaseError(
            phase         = phase,
            errorClass    = "RuntimeError",
            message       = msg,
            httpCode      = Regex("\\b(\\d{3})\\b").find(msg)?.groupValues?.get(1)?.toIntOrNull(),
            isCfPattern   = isCfPattern(msg),
            isDdosGuard   = isDdosGuard(msg),
            isTimeout     = msg.lowercase().contains("timeout"),
            isNetworkFail = msg.lowercase().let { it.contains("unknownhost") || it.contains("connect") },
            stackSnippet  = null
        )
    }

    /** URL'den embed host adını çıkarır — raporlamak için */
    private fun resolveEmbedHostName(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            when {
                host.contains("dood")        -> "Doodstream"
                host.contains("filemoon")    -> "Filemoon"
                host.contains("streamwish") || host.contains("swdyu") || host.contains("sfastwish") -> "StreamWish"
                host.contains("streamtape")  -> "Streamtape"
                host.contains("vidmoly")     -> "Vidmoly"
                host.contains("vidplay") || host.contains("vidoy") -> "Vidplay"
                host.contains("voe")         -> "Voe.sx"
                host.contains("mixdrop")     -> "Mixdrop"
                host.contains("ok.ru") || host.contains("odnoklassniki") -> "OK.ru"
                host.contains("vk.com") || host.contains("vkvideo") -> "VK"
                host.contains("sibnet")      -> "Sibnet"
                host.contains("uqload")      -> "Uqload"
                host.contains("dropload")    -> "Dropload"
                host.contains("vtube")       -> "Vtube"
                host.contains("molystream")  -> "Molystream"
                host.contains("rapidrame")   -> "Rapidrame"
                host.contains("pichive")     -> "Pichive"
                host.contains("streambox")   -> "Streambox"
                else -> host.removePrefix("www.").split(".").first().replaceFirstChar { it.uppercase() }
            }
        } catch (_: Exception) { url.take(30) }
    }

    // ─── HTTP client (sadece GitHub raw manifest çekimi için) ─────────────────

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ─── Entry point ──────────────────────────────────────────────────────────

    /**
     * Tüm 201 Türkçe CS eklentisini veya sadece yüklü olanları E2E olarak test eder.
     * Uygulama açıkken çağrılmalı — CF bypass için.
     */
    suspend fun startDiagnostic(context: Context, onlyInstalled: Boolean = false) = withContext(Dispatchers.IO) {
        if (_isRunning.value) {
            Log.w(TAG, "Tanı zaten çalışıyor, yeni istek yok sayıldı.")
            return@withContext
        }

        cancelled = false
        _isRunning.value = true
        _results.value = emptyList()
        _reportPath.value = null
        _progress.value = DiagnosticProgress(0, 0, "Eklentiler yükleniyor...", "Hazırlanıyor")

        diagnosticEmbedResults.clear()
        CsStreamRunner.embedResolveListener = object : CsStreamRunner.EmbedResolveListener {
            override fun onEmbedAttempt(
                providerName: String,
                rawUrl: String,
                resolved: Boolean,
                resolvedUrl: String?,
                error: String?
            ) {
                val list = diagnosticEmbedResults.getOrPut(providerName) {
                    java.util.Collections.synchronizedList(mutableListOf())
                }
                list.add(
                    EmbedResult(
                        sourceUrl = rawUrl,
                        sourceName = resolveEmbedHostName(rawUrl),
                        resolved = resolved,
                        resolvedUrl = resolvedUrl,
                        error = error
                    )
                )
            }
        }

        try {
            com.lagradost.cloudstream3.network.CloudflareKiller.ignoreCooldowns = true
            val allPlugins = mutableListOf<PluginEntry>()

            if (onlyInstalled) {
                // 1. Veritabanından yüklü eklentileri çek
                try {
                    val db = com.kitsugi.animelist.data.local.KitsugiDatabase.getDatabase(context)
                    val installed = db.csPluginDao().getAllPlugins()
                    for (entity in installed) {
                        val tvTypesList = try {
                            val arr = JSONArray(entity.tvTypes)
                            List(arr.length()) { arr.getString(it) }
                        } catch (e: Exception) {
                            emptyList<String>()
                        }
                        allPlugins.add(PluginEntry(entity.id, entity.name, entity.downloadUrl, tvTypesList, "Yerel"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Yerel eklentiler veritabanından alınamadı: ${e.message}", e)
                }
            } else {
                // 1. Tüm repoları tara — plugin listelerini çek
                for (repoUrl in REPOS) {
                    if (cancelled) break
                    val parts = repoUrl.removePrefix("https://raw.githubusercontent.com/").split("/")
                    val repoSlug = "${parts[0]}/${parts[1]}"
                    try {
                        val repoJson = JSONObject(fetch(repoUrl) ?: continue)
                        val lists = repoJson.optJSONArray("pluginLists") ?: continue
                        for (i in 0 until lists.length()) {
                            val listUrl = lists.optString(i).trim()
                            if (listUrl.isBlank()) continue
                            try {
                                val arr = JSONArray(fetch(listUrl) ?: continue)
                                for (j in 0 until arr.length()) {
                                    val obj = arr.optJSONObject(j) ?: continue
                                    val name   = obj.optString("name", "").trim()
                                    val id     = obj.optString("internalName", name).trim()
                                    val cs3Url = obj.optString("url", "").trim()
                                    val status = obj.optInt("status", 1)
                                    if (name.isBlank() || cs3Url.isBlank() || status == 3) continue
                                    val tvTypes = buildList {
                                        val ta = obj.optJSONArray("tvTypes") ?: return@buildList
                                        for (k in 0 until ta.length()) add(ta.optString(k))
                                    }
                                    allPlugins.add(PluginEntry(id, name, cs3Url, tvTypes, repoSlug))
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Liste alınamadı: $listUrl — ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Repo alınamadı: $repoUrl — ${e.message}")
                    }
                }
            }

            val uniquePlugins = allPlugins.distinctBy { it.cs3Url }
                .sortedWith(compareBy({ it.repoSlug }, { it.internalName }))

            Log.i(TAG, "📦 ${uniquePlugins.size} benzersiz eklenti bulundu.")

            // 2. Paralel E2E test
            val semaphore  = Semaphore(MAX_CONCURRENT)
            val completed  = AtomicInteger(0)
            val allResults = java.util.Collections.synchronizedList(mutableListOf<DiagnosticResult>())

            coroutineScope {
                val jobs = uniquePlugins.map { plugin ->
                    async(Dispatchers.IO) {
                        if (cancelled) return@async
                        semaphore.withPermit {
                            if (cancelled) return@withPermit
                            val idx = completed.incrementAndGet()
                            _progress.value = DiagnosticProgress(idx, uniquePlugins.size, plugin.displayName, "Test ediliyor...")
                            val result = withTimeoutOrNull(90_000L) {
                                testPlugin(context, plugin, idx, uniquePlugins.size, onlyInstalled)
                            } ?: run {
                                Log.w(TAG, "[${plugin.internalName}] Test timed out after 90s (stuck thread).")
                                DiagnosticResult(
                                    pluginId = plugin.internalName,
                                    repoSlug = plugin.repoSlug,
                                    displayName = plugin.displayName,
                                    mainUrl = plugin.cs3Url,
                                    downloaded = true,
                                    loaded = true,
                                    apiCount = 0,
                                    searchQuery = "N/A",
                                    searchCount = 0,
                                    topSearchHit = null,
                                    loadOk = false,
                                    loadedTitle = null,
                                    episodeFound = false,
                                    streamCount = 0,
                                    streamUrls = emptyList(),
                                    embedResults = emptyList(),
                                    phaseErrors = listOf(
                                        PhaseError(
                                            phase = "general",
                                            errorClass = "TimeoutException",
                                            message = "Test timed out after 90 seconds (possible infinite loop or network hang)",
                                            httpCode = null,
                                            isCfPattern = false,
                                            isDdosGuard = false,
                                            isTimeout = true,
                                            isNetworkFail = true,
                                            stackSnippet = null
                                        )
                                    ),
                                    error = "Test timed out after 90 seconds"
                                )
                            }
                            allResults.add(result)
                        }
                    }
                }
                jobs.awaitAll()
            }

            if (!cancelled) {
                val sorted = allResults.sortedWith(compareBy({ it.repoSlug }, { it.pluginId }))
                _results.value = sorted
                _progress.value = DiagnosticProgress(uniquePlugins.size, uniquePlugins.size, "Tüm testler tamamlandı!", "Tamamlandı")

                // 3. Auto-pruning — üç katmanlı strateji
                try {
                    val db = com.kitsugi.animelist.data.local.KitsugiDatabase.getDatabase(context)
                    val dao = db.csPluginDao()

                    // NOT: KNOWN_BROKEN_PLUGINS listesi sadece CsStreamRunner pipeline'ında
                    // (stream çekme, arama, detay yükleme) skip için kullanılır.
                    // Plugin yönetim ekranında kullanıcı bu eklentileri kurabilmeli —
                    // bu yüzden burada DB'de pasifleştirme YAPILMIYOR.

                    // Katman 2: Bu tanı çalışmasında DEAD durumundaki eklentileri pasifleştir.
                    // DEAD = plugin indirilemedi / yüklenemedi / ağ hatası ile tamamen çöktü.
                    // GÜNCELLEME: Sadece timeout olmayan ve yapısal olarak indirilemeyen/yüklenemeyen (dead) eklentileri pasifleştir.
                    val deadPlugins = sorted.filter { 
                        it.status == ResultStatus.DEAD && 
                        !it.hasTimeout && 
                        (!it.loaded || !it.downloaded)
                    }
                    Log.i(TAG, "[Katman-2] DEAD durumunda ${deadPlugins.size} eklenti tespit edildi.")
                    var deadPruned = 0
                    for (dead in deadPlugins) {
                        val entity = dao.getPluginById(dead.pluginId)
                        if (entity != null && entity.enabled) {
                            dao.upsert(entity.copy(enabled = false))
                            deadPruned++
                            Log.i(TAG, "[Katman-2] DEAD eklenti pasifleştirildi: ${dead.pluginId}")
                        }
                    }
                    if (deadPruned > 0)
                        Log.i(TAG, "[Katman-2] $deadPruned DEAD eklenti pasifleştirildi.")

                    // Katman 3: NO_STREAMS olan ve kesin ağ hatası (DNS/ConnectException) yaşayan
                    // eklentileri pasifleştir. CF block olanları pasifleştirme — geçici olabilir.
                    // GÜNCELLEME: Timeout yaşayanları pasifleştirme.
                    val noStreamNetFail = sorted.filter { result ->
                        result.status == ResultStatus.NO_STREAMS &&
                        result.hasNetworkFail &&
                        !result.hasTimeout &&
                        !result.hasCfBlock &&
                        !result.hasDdosGuard
                    }
                    Log.i(TAG, "[Katman-3] Ağ hatası olan NO_STREAMS ${noStreamNetFail.size} eklenti tespit edildi.")
                    var netFailPruned = 0
                    for (netFail in noStreamNetFail) {
                        val entity = dao.getPluginById(netFail.pluginId)
                        if (entity != null && entity.enabled) {
                            dao.upsert(entity.copy(enabled = false))
                            netFailPruned++
                            Log.i(TAG, "[Katman-3] NO_STREAMS+NetworkFail eklenti pasifleştirildi: ${netFail.pluginId}")
                        }
                    }
                    if (netFailPruned > 0)
                        Log.i(TAG, "[Katman-3] $netFailPruned NO_STREAMS+NetworkFail eklenti pasifleştirildi.")

                    val totalPruned = deadPruned + netFailPruned
                    Log.i(TAG, "✂️ Auto-pruning tamamlandı: $totalPruned eklenti pasifleştirildi (K2=$deadPruned K3=$netFailPruned).")
                } catch (e: Exception) {
                    Log.e(TAG, "Otomatik pasifleştirme (pruning) sırasında hata: ${e.message}", e)
                }

                // 4. Raporu yaz
                val file = writeReport(context, sorted)
                _reportPath.value = file?.absolutePath
                Log.i(TAG, "✅ Tanı tamamlandı. ${sorted.size} sonuç. Rapor: ${file?.absolutePath}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Tanı çalıştırma hatası: ${e.message}", e)
        } finally {
            _isRunning.value = false
            CsStreamRunner.embedResolveListener = null
            com.lagradost.cloudstream3.network.CloudflareKiller.ignoreCooldowns = false
        }
    }

    fun cancel() {
        cancelled = true
        Log.i(TAG, "Tanı iptal edildi.")
    }

    fun clearResults() {
        _results.value = emptyList()
        _progress.value = null
        _reportPath.value = null
    }

    // ─── Single plugin E2E test ───────────────────────────────────────────────

    private suspend fun testPlugin(
        context: Context,
        plugin:  PluginEntry,
        idx:     Int,
        total:   Int,
        onlyInstalled: Boolean
    ): DiagnosticResult {
        CsPluginStatusTracker.clearPluginStatus(plugin.internalName)

        var downloaded   = false
        var loaded       = false
        var apiCount     = 0
        var mainUrl      = ""
        var searchQueryUsed = "N/A"
        var searchCount  = 0
        var topSearchHit: String? = null
        var loadOk       = false
        var loadedTitle: String? = null
        var episodeFound = false
        var streamCount  = 0
        val streamUrls   = mutableListOf<String>()
        val embedResults = mutableListOf<EmbedResult>()
        val phaseErrors  = mutableListOf<PhaseError>()
        var errorMsg: String? = null

        fun progress(phase: String) {
            _progress.value = DiagnosticProgress(idx, total, plugin.displayName, phase)
        }
        fun err(phase: String, t: Throwable) { phaseErrors.add(buildPhaseError(phase, t)) }
        fun errMsg(phase: String, msg: String) { phaseErrors.add(buildPhaseErrorFromMsg(phase, msg)) }

        try {
            // 2a. İndir
            progress("İndiriliyor")
            val cs3File = File(context.filesDir, "cs_extensions/${plugin.internalName}.cs3")
            downloaded = if (onlyInstalled) {
                val exists = cs3File.exists()
                if (!exists) {
                    errMsg("download", "Yerel eklenti dosyası (.cs3) bulunamadı")
                }
                exists
            } else {
                try {
                    CsPluginLoader.downloadExtension(context, plugin.internalName, plugin.cs3Url)
                } catch (c: kotlinx.coroutines.CancellationException) {
                    throw c
                } catch (t: Throwable) { err("download", t); false }
            }
            if (!downloaded) {
                return DiagnosticResult(
                    pluginId = plugin.internalName, repoSlug = plugin.repoSlug,
                    displayName = plugin.displayName, mainUrl = mainUrl,
                    downloaded = false, loaded = false, apiCount = 0,
                    searchQuery = "N/A", searchCount = 0, topSearchHit = null,
                    loadOk = false, loadedTitle = null, episodeFound = false,
                    streamCount = 0, streamUrls = emptyList(),
                    embedResults = emptyList(), phaseErrors = phaseErrors.toList(),
                    error = phaseErrors.lastOrNull()?.message ?: "Download/Local check failed"
                )
            }

            // 2b. Yükle
            progress("Yükleniyor")
            val apis = try {
                CsPluginLoader.loadExtension(context, plugin.internalName, forceReload = !onlyInstalled)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (t: Throwable) { err("load", t); emptyList() }
            loaded   = apis.isNotEmpty()
            apiCount = apis.size
            if (!loaded) {
                return DiagnosticResult(
                    pluginId = plugin.internalName, repoSlug = plugin.repoSlug,
                    displayName = plugin.displayName, mainUrl = mainUrl,
                    downloaded = true, loaded = false, apiCount = 0,
                    searchQuery = "N/A", searchCount = 0, topSearchHit = null,
                    loadOk = false, loadedTitle = null, episodeFound = false,
                    streamCount = 0, streamUrls = emptyList(),
                    embedResults = emptyList(), phaseErrors = phaseErrors.toList(),
                    error = phaseErrors.lastOrNull()?.message ?: "No APIs registered"
                )
            }

            val api = apis.first()
            mainUrl = api.mainUrl
            diagnosticEmbedResults.remove(api.name)
            CsStreamRunner.clearUnsupportedMethodsCache()

            // Olası arama sorgularını belirle
            val fallbackQueries = when {
                plugin.tvTypes.any { it.contains("Anime", ignoreCase = true) || it.contains("Cartoon", ignoreCase = true) }
                    -> listOf("Solo Leveling", "One Piece", "Naruto")
                plugin.tvTypes.any { it.contains("Movie", ignoreCase = true) || it.contains("TvSeries", ignoreCase = true) }
                    -> listOf("Squid Game", "Wednesday", "Breaking Bad")
                plugin.tvTypes.any { it.contains("Documentary", ignoreCase = true) }
                    -> listOf("Life", "Earth", "Cosmos")
                else -> listOf("Solo Leveling", "Squid Game", "Wednesday")
            }

            val queriesToTry = mutableListOf<String>()
            if (api.hasMainPage) {
                try {
                    progress("Ana sayfa yükleniyor (Arama sorgusu için)")
                    val mainPages = api.mainPage
                    val mainPageList = mainPages.firstOrNull()
                    if (mainPageList != null) {
                        val homepage = withTimeoutOrNull(15_000L) {
                            api.getMainPage(1, com.lagradost.cloudstream3.MainPageRequest(mainPageList.name, mainPageList.data, mainPageList.horizontalImages))
                        }
                        val items = homepage?.items?.flatMap { it.list } ?: emptyList()
                        if (items.isNotEmpty()) {
                            val names = items.shuffled().take(3).mapNotNull {
                                it.name.split(" ").firstOrNull()?.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]"), "")
                            }.filter { it.length > 2 }
                            queriesToTry.addAll(names)
                        }
                    }
                } catch (c: kotlinx.coroutines.CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    Log.d(TAG, "[${api.name}] Ana sayfa yüklenirken hata oluştu: ${t.message}")
                }
            }

            if (queriesToTry.isEmpty()) {
                queriesToTry.addAll(fallbackQueries)
            }

            // 2c. Ara
            var searchResults = emptyList<com.lagradost.cloudstream3.SearchResponse>()
            var chosenQuery = ""
            for (q in queriesToTry) {
                progress("Aranıyor: $q")
                try {
                    val res = CsStreamRunner.safeSearch(api, q)
                    if (res.isNotEmpty()) {
                        searchResults = res
                        chosenQuery = q
                        break
                    }
                } catch (c: kotlinx.coroutines.CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    err("search", t)
                }
            }
            searchCount  = searchResults.size
            topSearchHit = searchResults.firstOrNull()?.name
            searchQueryUsed = chosenQuery.ifBlank { queriesToTry.firstOrNull() ?: "Solo Leveling" }

            if (searchResults.isEmpty()) {
                val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
                if (lastErr != null) errMsg("search", lastErr)
                return DiagnosticResult(
                    pluginId = plugin.internalName, repoSlug = plugin.repoSlug,
                    displayName = plugin.displayName, mainUrl = mainUrl,
                    downloaded = true, loaded = true, apiCount = apiCount,
                    searchQuery = searchQueryUsed, searchCount = 0, topSearchHit = null,
                    loadOk = false, loadedTitle = null, episodeFound = false,
                    streamCount = 0, streamUrls = emptyList(),
                    embedResults = emptyList(), phaseErrors = phaseErrors.toList(),
                    error = lastErr ?: phaseErrors.lastOrNull()?.message ?: "No results found"
                )
            }

            // 2d. Detay yükle
            progress("Detay yükleniyor")
            val firstResult  = searchResults.first()
            val loadResponse = try {
                CsStreamRunner.safeLoad(api, firstResult.url)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (t: Throwable) { err("detail", t); null }

            if (loadResponse == null) {
                val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
                if (lastErr != null) errMsg("detail", lastErr)
                return DiagnosticResult(
                    pluginId = plugin.internalName, repoSlug = plugin.repoSlug,
                    displayName = plugin.displayName, mainUrl = mainUrl,
                    downloaded = true, loaded = true, apiCount = apiCount,
                    searchQuery = searchQueryUsed, searchCount = searchCount, topSearchHit = topSearchHit,
                    loadOk = false, loadedTitle = null, episodeFound = false,
                    streamCount = 0, streamUrls = emptyList(),
                    embedResults = emptyList(), phaseErrors = phaseErrors.toList(),
                    error = lastErr ?: phaseErrors.lastOrNull()?.message ?: "load() returned null"
                )
            }
            loadOk      = true
            loadedTitle = try { loadResponse.name } catch (_: Exception) { null }

            // 2e. Episode data
            progress("Bölüm verisi kontrol ediliyor")
            val episodeData = try {
                CsEpisodeMatcher.findEpisodeData(loadResponse, 1, 1)
                    ?: loadResponse.url.takeIf { it.isNotBlank() }
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (t: Throwable) { err("episode", t); null }

            if (episodeData == null) {
                errMsg("episode", "findEpisodeData null, loadResponse.url boş")
                return DiagnosticResult(
                    pluginId = plugin.internalName, repoSlug = plugin.repoSlug,
                    displayName = plugin.displayName, mainUrl = mainUrl,
                    downloaded = true, loaded = true, apiCount = apiCount,
                    searchQuery = searchQueryUsed, searchCount = searchCount, topSearchHit = topSearchHit,
                    loadOk = true, loadedTitle = loadedTitle, episodeFound = false,
                    streamCount = 0, streamUrls = emptyList(),
                    embedResults = emptyList(), phaseErrors = phaseErrors.toList(),
                    error = "No episode data"
                )
            }
            episodeFound = true

            // 2f. Stream çek
            progress("Stream çekiliyor")
            val streams = try {
                CsStreamRunner.getStreamsForUrl(api, firstResult.url, 1, 1)
            } catch (c: kotlinx.coroutines.CancellationException) {
                throw c
            } catch (t: Throwable) { err("stream", t); emptyList() }

            streamCount = streams.size
            val capturedEmbeds = diagnosticEmbedResults[api.name] ?: emptyList()
            embedResults.addAll(capturedEmbeds)

            val resolvedEmbedUrls = capturedEmbeds.mapNotNull { it.resolvedUrl }.toSet()

            for (s in streams) {
                val url = s.url
                if (!url.isNullOrBlank()) {
                    streamUrls.add("${s.name}: $url")
                    if (url !in resolvedEmbedUrls) {
                        embedResults.add(EmbedResult(
                            sourceUrl = url,
                            sourceName = resolveEmbedHostName(url),
                            resolved = true,
                            resolvedUrl = url,
                            error = null
                        ))
                    }
                } else {
                    streamUrls.add("${s.name}: (null/boş)")
                }
            }
            if (streamCount == 0) {
                val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
                if (lastErr != null) errMsg("stream", lastErr)
                errorMsg = lastErr ?: phaseErrors.lastOrNull()?.message ?: "No streams resolved"
            }

            Log.i(TAG, "[$idx/$total] ${plugin.internalName} → search=$searchCount load=$loadOk ep=$episodeFound streams=$streamCount")

        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.e(TAG, "[${plugin.internalName}] Genel hata: ${t.message}", t)
            err("general", t)
            errorMsg = "${t.javaClass.simpleName}: ${t.message}"
        }

        return DiagnosticResult(
            pluginId     = plugin.internalName,
            repoSlug     = plugin.repoSlug,
            displayName  = plugin.displayName,
            mainUrl      = mainUrl,
            downloaded   = downloaded,
            loaded       = loaded,
            apiCount     = apiCount,
            searchQuery  = searchQueryUsed,
            searchCount  = searchCount,
            topSearchHit = topSearchHit,
            loadOk       = loadOk,
            loadedTitle  = loadedTitle,
            episodeFound = episodeFound,
            streamCount  = streamCount,
            streamUrls   = streamUrls,
            embedResults = embedResults,
            phaseErrors  = phaseErrors,
            error        = errorMsg
        )
    }

    // ─── Report writer ────────────────────────────────────────────────────────

    private fun writeReport(context: Context, results: List<DiagnosticResult>): File? {
        return try {
            val working    = results.count { it.streamCount > 0 }
            val noStream   = results.count { it.loaded && it.searchCount > 0 && it.streamCount == 0 }
            val cfBlocked  = results.count { it.loaded && it.searchCount == 0 }
            val dead       = results.count { !it.loaded }

            val sb = StringBuilder()
            sb.appendLine("# Kitsugi In-App CS Plugin Tanı Raporu")
            sb.appendLine()
            sb.appendLine("**Tarih:** ${java.time.LocalDateTime.now()}")
            val modeStr = if (results.any { it.repoSlug == "Yerel" }) "Yerel Eklentiler (Kurulu)" else "Tüm Havuz Eklentileri"
            sb.appendLine("**Mod:** In-App (CF bypass aktif) • $modeStr | Paralel MAX_CONCURRENT=$MAX_CONCURRENT")
            sb.appendLine("**Toplam:** ${results.size} eklenti")
            sb.appendLine()
            sb.appendLine("## Özet")
            sb.appendLine("| Durum | Sayı |")
            sb.appendLine("|---|---|")
            sb.appendLine("| ✅ Çalışıyor (stream bulundu) | $working |")
            sb.appendLine("| ⚠️ Arama var ama stream yok | $noStream |")
            sb.appendLine("| 🔍 Arama boş (CF/WAF) | $cfBlocked |")
            sb.appendLine("| ❌ Bozuk / İndirilemedi | $dead |")
            sb.appendLine()
            sb.appendLine("## Detaylı Sonuçlar")
            sb.appendLine()
            sb.appendLine("| Plugin | Repo | mainUrl | DL | Load | Sorgu | Hits | LoadOK | Ep | Str | CF | TO | Durum |")
            sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|---|")
            for (r in results) {
                val urlShort = r.mainUrl.removePrefix("https://").removePrefix("http://").trimEnd('/').take(32)
                val statusStr = when (r.status) {
                    ResultStatus.WORKING     -> "✅ ${r.streamCount}"
                    ResultStatus.NO_STREAMS  -> "⚠️ ${r.error?.take(35) ?: "No Streams"}"
                    ResultStatus.CF_BLOCKED  -> "🔍 CF/WAF"
                    ResultStatus.LOAD_FAILED -> "⚠️ Load Fail"
                    ResultStatus.DEAD        -> "❌ ${r.error?.take(35) ?: "Bozuk"}"
                }
                sb.appendLine("| **${r.pluginId}** | ${r.repoSlug} | `$urlShort` | ${if (r.downloaded) "✅" else "❌"} | ${if (r.loaded) "✅" else "❌"} | `${r.searchQuery}` | ${r.searchCount} | ${if (r.loadOk) "✅" else "❌"} | ${if (r.episodeFound) "✅" else "❌"} | ${r.streamCount} | ${if (r.hasCfBlock) "⚠️" else "-"} | ${if (r.hasTimeout) "⏱" else "-"} | $statusStr |")
            }
            // ── Hata Detayları ─────────────────────────────────────────
            sb.appendLine()
            sb.appendLine("## Hata Detayları (Faz Bazında)")
            sb.appendLine()
            val failing = results.filter { it.phaseErrors.isNotEmpty() }
            if (failing.isEmpty()) {
                sb.appendLine("_Tüm eklentiler hatasız çalıştı._")
            } else {
                for (r in failing) {
                    sb.appendLine("### ${r.pluginId}")
                    sb.appendLine("> **mainUrl:** `${r.mainUrl}` | **title:** ${r.loadedTitle ?: "—"} | **topHit:** ${r.topSearchHit ?: "—"}")
                    sb.appendLine()
                    sb.appendLine("| Faz | Sınıf | HTTP | CF | DDoS | TO | Net | Mesaj |")
                    sb.appendLine("|---|---|---|---|---|---|---|---|")
                    for (e in r.phaseErrors) {
                        sb.appendLine("| `${e.phase}` | `${e.errorClass}` | ${e.httpCode ?: "—"} | ${if (e.isCfPattern) "⚠️" else "—"} | ${if (e.isDdosGuard) "🛡" else "—"} | ${if (e.isTimeout) "⏱" else "—"} | ${if (e.isNetworkFail) "📡" else "—"} | ${e.message.take(80).replace("|", "\\|")} |")
                    }
                    val stacks = r.phaseErrors.filter { !it.stackSnippet.isNullOrBlank() }
                    if (stacks.isNotEmpty()) {
                        sb.appendLine()
                        sb.appendLine("<details><summary>Stack Trace</summary>")
                        sb.appendLine()
                        sb.appendLine("```")
                        stacks.forEach { e ->
                            sb.appendLine("[${e.phase}] ${e.errorClass}: ${e.message.take(100)}")
                            sb.appendLine(e.stackSnippet)
                        }
                        sb.appendLine("```")
                        sb.appendLine()
                        sb.appendLine("</details>")
                    }
                    sb.appendLine()
                }
            }

            // ── Embed CDN tablosu ──────────────────────────────────────
            sb.appendLine()
            sb.appendLine("## Embed/CDN Kaynak Özeti")
            sb.appendLine()
            val embedFreq = results.flatMap { it.embedResults }
                .groupBy { it.sourceName }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
            if (embedFreq.isEmpty()) {
                sb.appendLine("_Hiç stream çözümlenemedi._")
            } else {
                sb.appendLine("| CDN Host | Adet |")
                sb.appendLine("|---|---|")
                embedFreq.forEach { (host, cnt) -> sb.appendLine("| **$host** | $cnt |") }
            }

            // ── Embed/CDN Detaylı Çözümleme Sonuçları ─────────────────
            sb.appendLine()
            sb.appendLine("## Embed/CDN Detaylı Çözümleme Sonuçları")
            sb.appendLine()
            val withEmbeds = results.filter { it.embedResults.isNotEmpty() }
            if (withEmbeds.isEmpty()) {
                sb.appendLine("_Çözümlenmeye çalışılan embed kaynağı bulunamadı._")
            } else {
                for (r in withEmbeds) {
                    sb.appendLine("### ${r.pluginId}")
                    sb.appendLine()
                    sb.appendLine("| Embed URL | Host | Resolved | Hata |")
                    sb.appendLine("|---|---|---|---|")
                    for (emb in r.embedResults) {
                        val statusEmoji = if (emb.resolved) "✅" else "❌"
                        val errStr = (emb.error ?: "—").replace("|", "\\|")
                        val escapedUrl = emb.sourceUrl.replace("|", "\\|")
                        sb.appendLine("| $escapedUrl | ${emb.sourceName} | $statusEmoji | $errStr |")
                    }
                    sb.appendLine()
                }
            }

            // ── KNOWN_BROKEN_PLUGINS bloğu ─────────────────────────────
            sb.appendLine()
            sb.appendLine("## KNOWN_BROKEN_PLUGINS Adayları")
            sb.appendLine()
            sb.appendLine("Kopyala → `CsStreamRunner.kt` > `KNOWN_BROKEN_PLUGINS`:")
            sb.appendLine()
            sb.appendLine("```kotlin")
            results.filter { !it.downloaded || (!it.loaded && it.downloaded) }.forEach { r ->
                val why = r.phaseErrors.firstOrNull()
                    ?.let { "${it.phase}: ${it.errorClass} — ${it.message.take(45)}" }
                    ?: r.error?.take(60) ?: "unknown"
                sb.appendLine("    \"${r.pluginId}\", // $why")
            }
            sb.appendLine("```")

            // ── Çalışan URL'ler ────────────────────────────────────────
            sb.appendLine()
            sb.appendLine("## Bulunan Stream URL'leri")
            results.filter { it.streamCount > 0 }.forEach { r ->
                sb.appendLine()
                sb.appendLine("### ${r.pluginId} (${r.repoSlug})")
                r.streamUrls.forEach { sb.appendLine("- $it") }
            }

            val file = File(context.getExternalFilesDir(null), "cs_inapp_diagnostic_report.md")
            file.writeText(sb.toString())
            Log.i(TAG, "Rapor yazıldı: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Rapor yazılamadı: ${e.message}", e)
            null
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP fetch hatası: $url — ${e.message}")
            null
        }
    }

    private data class PluginEntry(
        val internalName: String,
        val displayName:  String,
        val cs3Url:       String,
        val tvTypes:      List<String>,
        val repoSlug:     String
    )
}
