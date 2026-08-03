package com.kitsugi.animelist

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.cloudstream.CsRuntimeInit
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.data.cloudstream.CsEpisodeMatcher
import com.kitsugi.animelist.data.cloudstream.CsPluginStatusTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CsDeepStreamTest {

    companion object {
        private const val TAG = "CsDeepStreamTest"

        // Max paralel eklenti testi (app'teki semaphore mantığıyla aynı)
        private const val MAX_CONCURRENT = 6

        private val REPOS = listOf(
            "https://raw.githubusercontent.com/maarrem/cs-Kekik/master/repo.json",
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
    }

    data class PluginEntry(
        val internalName: String,
        val displayName: String,
        val cs3Url: String,
        val tvTypes: List<String>,
        val repoSlug: String
    )

    data class DeepTestResult(
        val pluginId: String,
        val repoSlug: String,
        val displayName: String,
        val downloaded: Boolean,
        val loaded: Boolean,
        val searchQuery: String,
        val searchCount: Int,
        val loadOk: Boolean,
        val streamCount: Int,
        val streamUrls: List<String>,
        val error: String?
    )

    // Manifest çekimi için CF-aware olmayan vanilla client (sadece GitHub raw)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        var activityContext: android.app.Activity? = null
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val resumedActivities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                if (resumedActivities.iterator().hasNext()) {
                    activityContext = resumedActivities.iterator().next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get resumed activity", e)
        }

        val ctx = activityContext ?: InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(TAG, "Initializing CsRuntimeInit with context: $ctx (is Activity: ${ctx is android.app.Activity})")
        CsRuntimeInit.init(ctx)

        // Trigger proactive cookie warmup for the test run so the test process gets valid cookies in savedCookies map
        runBlocking {
            try {
                Log.i(TAG, "Running proactive CF cookie warmup for test process...")
                com.kitsugi.animelist.data.cloudstream.CsCfWarmupManager.runWarmup(ctx, maxSites = 15, timeoutMs = 15000L)
            } catch (e: Exception) {
                Log.e(TAG, "Proactive cookie warmup in test setUp failed: ${e.message}", e)
            }
        }
    }

    @Test
    fun testSelectedPluginsTargeted() {
        runBlocking {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val targetNames = setOf("FilmMakinesi", "TurkAnime", "DiziBox", "Ddizi", "WebteIzle")
            val allPlugins = mutableListOf<PluginEntry>()

            Log.i(TAG, "╔══════════════════════════════════════════════════╗")
            Log.i(TAG, "║   Targeted Turkish Plugins Diagnostic Test       ║")
            Log.i(TAG, "╚══════════════════════════════════════════════════╝")

            for (repoUrl in REPOS) {
                val repoSlug = repoUrl
                    .removePrefix("https://raw.githubusercontent.com/")
                    .split("/").take(2).joinToString("/")
                try {
                    val request = Request.Builder().url(repoUrl).build()
                    val responseStr = httpClient.newCall(request).execute().use { it.body?.string() ?: "" }
                    if (responseStr.isBlank()) continue
                    val repoObj = JSONObject(responseStr)
                    val pluginListUrls = mutableListOf<String>()
                    repoObj.optJSONArray("pluginLists")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val u = arr.optString(i)
                            if (u.isNotBlank()) pluginListUrls.add(u)
                        }
                    }
                    for (listUrl in pluginListUrls) {
                        try {
                            val listReq = Request.Builder().url(listUrl).build()
                            val listStr = httpClient.newCall(listReq).execute().use { it.body?.string() ?: "" }
                            if (listStr.isBlank()) continue
                            val arr = JSONArray(listStr)
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val name = obj.optString("name", "").trim()
                                val id = obj.optString("internalName", name).trim()
                                val cs3Url = obj.optString("url", "").trim()
                                val status = obj.optInt("status", 1)
                                if (name.isBlank() || cs3Url.isBlank() || status == 3) continue
                                if (name in targetNames || id in targetNames) {
                                    val tvTypesArr = obj.optJSONArray("tvTypes")
                                    val tvTypes = if (tvTypesArr != null) {
                                        (0 until tvTypesArr.length()).map { tvTypesArr.optString(it) }
                                    } else emptyList()
                                    allPlugins.add(PluginEntry(id, name, cs3Url, tvTypes, repoSlug))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed parsing list: $listUrl - ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed fetching repo: $repoUrl - ${e.message}")
                }
            }

            val uniquePlugins = allPlugins.distinctBy { it.cs3Url + it.repoSlug }
            Log.i(TAG, "🎯 Found ${uniquePlugins.size} targeted plugins to diagnose.")

            for (plugin in uniquePlugins) {
                Log.i(TAG, "--------------------------------------------------")
                Log.i(TAG, "Testing: ${plugin.displayName} (${plugin.internalName}) from ${plugin.repoSlug}")
                
                val query = when {
                    plugin.tvTypes.any { it.contains("Anime", ignoreCase = true) || it.contains("Cartoon", ignoreCase = true) } -> "Solo Leveling"
                    plugin.tvTypes.any { it.contains("Movie", ignoreCase = true) || it.contains("TvSeries", ignoreCase = true) } -> "Squid Game"
                    else -> "Solo Leveling"
                }

                CsPluginStatusTracker.clearPluginStatus(plugin.internalName)

                var downloaded = false
                var loaded = false
                try {
                    downloaded = CsPluginLoader.downloadExtension(ctx, plugin.internalName, plugin.cs3Url)
                    if (!downloaded) {
                        Log.e(TAG, "  ❌ Download failed")
                        continue
                    }

                    val apis = CsPluginLoader.loadExtension(ctx, plugin.internalName, forceReload = true)
                    loaded = apis.isNotEmpty()
                    if (!loaded) {
                        Log.e(TAG, "  ❌ Load failed (0 APIs)")
                        continue
                    }

                    val api = apis.first()
                    Log.i(TAG, "  ✅ Loaded: ${api.name} (${api.mainUrl})")
                    
                    val searchResults = CsStreamRunner.safeSearch(api, query)
                    Log.i(TAG, "  🔍 Search count for '$query': ${searchResults.size}")
                    if (searchResults.isEmpty()) {
                        Log.e(TAG, "  ❌ Search results is empty! Last error: ${CsPluginStatusTracker.getErrorMessage(api.name)}")
                        continue
                    }

                    val firstResult = searchResults.first()
                    Log.i(TAG, "  First result: '${firstResult.name}' -> ${firstResult.url}")
                    val loadResponse = CsStreamRunner.safeLoad(api, firstResult.url)
                    if (loadResponse == null) {
                        Log.e(TAG, "  ❌ load() returned null! Last error: ${CsPluginStatusTracker.getErrorMessage(api.name)}")
                        continue
                    }
                    Log.i(TAG, "  ✅ Detail load successful: ${loadResponse.name}")

                    // Register temporary listener
                    CsStreamRunner.embedResolveListener = object : CsStreamRunner.EmbedResolveListener {
                        override fun onEmbedAttempt(
                            providerName: String,
                            rawUrl: String,
                            resolved: Boolean,
                            resolvedUrl: String?,
                            error: String?
                        ) {
                            Log.i(TAG, "    [EmbedAttempt] rawUrl='$rawUrl' resolved=$resolved resolvedUrl='$resolvedUrl' error='$error'")
                        }
                    }

                    val streams = CsStreamRunner.getStreamsForUrl(api, firstResult.url, 1, 1)
                    Log.i(TAG, "  🎬 Resolved streams count: ${streams.size}")
                    for (stream in streams) {
                        Log.i(TAG, "    Stream: '${stream.name}' -> ${stream.url}")
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "  💥 Exception: ${t.message}", t)
                }
            }
        }
    }

    @Test
    fun testAllTurkishPluginsE2E() {
        runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val allPlugins = mutableListOf<PluginEntry>()

        Log.i(TAG, "╔══════════════════════════════════════════════════╗")
        Log.i(TAG, "║   Kitsugi E2E Turkish Plugins Diagnostic Test    ║")
        Log.i(TAG, "║   Paralel Mod: MAX $MAX_CONCURRENT eş zamanlı test      ║")
        Log.i(TAG, "╚══════════════════════════════════════════════════╝")

        // 1. Tüm repolardan manifest çek (sıralı — GitHub bant genişliği için)
        for (repoUrl in REPOS) {
            val repoSlug = repoUrl
                .removePrefix("https://raw.githubusercontent.com/")
                .split("/").take(2).joinToString("/")
            try {
                val request = Request.Builder().url(repoUrl).build()
                val responseStr = httpClient.newCall(request).execute().use { it.body?.string() ?: "" }
                if (responseStr.isBlank()) continue
                val repoObj = JSONObject(responseStr)
                val pluginListUrls = mutableListOf<String>()
                repoObj.optJSONArray("pluginLists")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val u = arr.optString(i)
                        if (u.isNotBlank()) pluginListUrls.add(u)
                    }
                }
                for (listUrl in pluginListUrls) {
                    try {
                        val listReq = Request.Builder().url(listUrl).build()
                        val listStr = httpClient.newCall(listReq).execute().use { it.body?.string() ?: "" }
                        if (listStr.isBlank()) continue
                        val arr = JSONArray(listStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val name = obj.optString("name", "").trim()
                            val id = obj.optString("internalName", name).trim()
                            val cs3Url = obj.optString("url", "").trim()
                            val status = obj.optInt("status", 1)
                            if (name.isBlank() || cs3Url.isBlank() || status == 3) continue
                            val tvTypesArr = obj.optJSONArray("tvTypes")
                            val tvTypes = if (tvTypesArr != null) {
                                (0 until tvTypesArr.length()).map { tvTypesArr.optString(it) }
                            } else emptyList()
                            allPlugins.add(PluginEntry(id, name, cs3Url, tvTypes, repoSlug))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing list: $listUrl - ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed fetching repo: $repoUrl - ${e.message}")
            }
        }

        val uniquePlugins = allPlugins.distinctBy { it.cs3Url }.sortedWith(
            compareBy({ it.repoSlug }, { it.internalName })
        )
        Log.i(TAG, "📦 Toplam ${uniquePlugins.size} benzersiz eklenti bulundu.")

        // 2. PARALEl E2E test — MAX_CONCURRENT semaphore ile
        val semaphore = Semaphore(MAX_CONCURRENT)
        val completedCount = AtomicInteger(0)
        val results = java.util.Collections.synchronizedList(mutableListOf<DeepTestResult>())

        val jobs = uniquePlugins.mapIndexed { index, plugin ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val done = completedCount.incrementAndGet()
                    Log.i(TAG, "[$done/${uniquePlugins.size}] ▶ ${plugin.internalName} (repo: ${plugin.repoSlug})")

                    val query = when {
                        plugin.tvTypes.any { it.contains("Anime", ignoreCase = true) || it.contains("Cartoon", ignoreCase = true) } -> "Solo Leveling"
                        plugin.tvTypes.any { it.contains("Movie", ignoreCase = true) || it.contains("TvSeries", ignoreCase = true) } -> "Squid Game"
                        plugin.tvTypes.any { it.contains("Documentary", ignoreCase = true) } -> "Life"
                        else -> "Solo Leveling"
                    }

                    CsPluginStatusTracker.clearPluginStatus(plugin.internalName)

                    var downloaded = false
                    var loaded = false
                    var searchCount = 0
                    var loadOk = false
                    var streamCount = 0
                    val streamUrls = mutableListOf<String>()
                    var errorMsg: String? = null

                    try {
                        // 2a. İndir
                        downloaded = CsPluginLoader.downloadExtension(ctx, plugin.internalName, plugin.cs3Url)
                        if (!downloaded) {
                            Log.w(TAG, "  ❌ [${plugin.internalName}] İndirme başarısız.")
                            results.add(DeepTestResult(plugin.internalName, plugin.repoSlug, plugin.displayName, false, false, query, 0, false, 0, emptyList(), "Download failed"))
                            return@withPermit
                        }

                        // 2b. Yükle (DEX → Android runtime)
                        val apis = CsPluginLoader.loadExtension(ctx, plugin.internalName, forceReload = true)
                        loaded = apis.isNotEmpty()
                        if (!loaded) {
                            Log.w(TAG, "  ❌ [${plugin.internalName}] Yükleme 0 API döndürdü.")
                            results.add(DeepTestResult(plugin.internalName, plugin.repoSlug, plugin.displayName, true, false, query, 0, false, 0, emptyList(), "No APIs registered"))
                            return@withPermit
                        }

                        val api = apis.first()
                        Log.d(TAG, "  ✅ [${plugin.internalName}] API yüklendi: ${api.name} (${api.mainUrl})")
                        CsStreamRunner.clearUnsupportedMethodsCache()

                        // 2c. Ara (app.baseClient → CF+DDoS bypass üzerinden)
                        val searchResults = CsStreamRunner.safeSearch(api, query)
                        searchCount = searchResults.size
                        Log.d(TAG, "  🔍 [${plugin.internalName}] Arama: $searchCount sonuç ('$query')")

                        if (searchResults.isEmpty()) {
                            val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
                            results.add(
                                DeepTestResult(
                                    pluginId = plugin.internalName,
                                    repoSlug = plugin.repoSlug,
                                    displayName = plugin.displayName,
                                    downloaded = true,
                                    loaded = true,
                                    searchQuery = query,
                                    searchCount = 0,
                                    loadOk = false,
                                    streamCount = 0,
                                    streamUrls = emptyList(),
                                    error = lastErr ?: "No search results (CF/WAF?)"
                                )
                            )
                            return@withPermit
                        }

                        // 2d. Detay yükle
                        val firstResult = searchResults.first()
                        val loadResponse = CsStreamRunner.safeLoad(api, firstResult.url)

                        if (loadResponse == null) {
                            val lastErr = CsPluginStatusTracker.getErrorMessage(api.name)
                            results.add(
                                DeepTestResult(
                                    pluginId = plugin.internalName,
                                    repoSlug = plugin.repoSlug,
                                    displayName = plugin.displayName,
                                    downloaded = true,
                                    loaded = true,
                                    searchQuery = query,
                                    searchCount = searchCount,
                                    loadOk = false,
                                    streamCount = 0,
                                    streamUrls = emptyList(),
                                    error = lastErr ?: "load() returned null"
                                )
                            )
                            return@withPermit
                        }
                        loadOk = true
                        Log.d(TAG, "  📋 [${plugin.internalName}] Detay yüklendi: ${loadResponse.name}")

                        // 2e. Bölüm datasını çek
                        // Fallback: bazı providerlar (powerDizi gibi) loadLinks'e URL'yi data olarak
                        // geçiyor — findEpisodeData null döndürürse loadResponse.url'yi kullan.
                        val episodeData = CsEpisodeMatcher.findEpisodeData(loadResponse, 1, 1)
                            ?: run {
                                val fallbackUrl = loadResponse.url.takeIf { it.isNotBlank() }
                                if (fallbackUrl != null) {
                                    Log.w(TAG, "  ⚠️ [${plugin.internalName}] findEpisodeData null — URL fallback kullanılıyor: $fallbackUrl")
                                }
                                fallbackUrl
                            }

                        if (episodeData == null) {
                            results.add(
                                DeepTestResult(
                                    pluginId = plugin.internalName,
                                    repoSlug = plugin.repoSlug,
                                    displayName = plugin.displayName,
                                    downloaded = true,
                                    loaded = true,
                                    searchQuery = query,
                                    searchCount = searchCount,
                                    loadOk = true,
                                    streamCount = 0,
                                    streamUrls = emptyList(),
                                    error = "No episode data (URL da boş)"
                                )
                            )
                            return@withPermit
                        }

                        // 2f. Gerçek stream linklerini çek (app'in safe extraction logic'ini yani resolveEmbedUrl'li akışını kullanıyoruz!)
                        val streams = try {
                            CsStreamRunner.getStreamsForUrl(api, firstResult.url, 1, 1)
                        } catch (t: Throwable) {
                            errorMsg = "${t.javaClass.simpleName}: ${t.message}"
                            emptyList()
                        }

                        streamCount = streams.size
                        streamUrls.addAll(streams.map { "${it.name}: ${it.url}" })
                        Log.i(TAG, "  ✅ [${plugin.internalName}] Bitti → search=$searchCount loadOk=$loadOk streams=$streamCount")

                    } catch (t: Throwable) {
                        Log.e(TAG, "  💥 [${plugin.internalName}] Genel hata: ${t.message}", t)
                        errorMsg = "${t.javaClass.simpleName}: ${t.message}"
                    }

                    results.add(
                        DeepTestResult(
                            pluginId = plugin.internalName,
                            repoSlug = plugin.repoSlug,
                            displayName = plugin.displayName,
                            downloaded = downloaded,
                            loaded = loaded,
                            searchQuery = query,
                            searchCount = searchCount,
                            loadOk = loadOk,
                            streamCount = streamCount,
                            streamUrls = streamUrls,
                            error = errorMsg
                        )
                    )
                }
            }
        }

        // Tüm paralel testlerin bitmesini bekle
        jobs.awaitAll()
        Log.i(TAG, "🏁 Tüm eklenti testleri tamamlandı. ${results.size} sonuç.")

        // 3. Rapor oluştur
        val sortedResults = results.sortedWith(compareBy({ it.repoSlug }, { it.pluginId }))
        val reportFile = File(ctx.getExternalFilesDir(null), "cs_deep_stream_report.md")
        val sb = StringBuilder()
        sb.appendLine("# Kitsugi Turkish CS Plugins On-Device E2E Diagnostic Report")
        sb.appendLine()
        sb.appendLine("**Date:** ${java.time.LocalDateTime.now()}")
        sb.appendLine("**Device:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("**Mode:** Paralel (max $MAX_CONCURRENT eş zamanlı) | CloudflareKiller ✅ | DdosGuardKiller ✅")
        sb.appendLine("**Total Plugins:** ${uniquePlugins.size}")
        sb.appendLine()

        val workingCount = sortedResults.count { it.streamCount > 0 }
        val searchOkNoStream = sortedResults.count { it.loaded && it.searchCount > 0 && it.streamCount == 0 }
        val searchEmpty = sortedResults.count { it.loaded && it.searchCount == 0 }
        val failLoad = sortedResults.count { !it.loaded }

        sb.appendLine("## Özet")
        sb.appendLine("| Durum | Sayı |")
        sb.appendLine("|---|---|")
        sb.appendLine("| ✅ Tam çalışıyor (Stream bulundu) | $workingCount |")
        sb.appendLine("| ⚠️ Arama var ama stream yok | $searchOkNoStream |")
        sb.appendLine("| 🔍 Arama boş (CF/WAF/AJAX) | $searchEmpty |")
        sb.appendLine("| ❌ Yüklenemedi / Bozuk | $failLoad |")
        sb.appendLine()

        sb.appendLine("## Detaylı Sonuçlar")
        sb.appendLine()
        sb.appendLine("| Plugin | Repo | İndir | Yükle | Sorgu | Sonuç | Load OK | Stream | Durum |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|")

        for (r in sortedResults) {
            val dlIcon = if (r.downloaded) "✅" else "❌"
            val loadIcon = if (r.loaded) "✅" else "❌"
            val loadOkStr = if (r.loadOk) "✅" else "❌"
            val statusStr = when {
                !r.downloaded -> "❌ Download Failed"
                !r.loaded -> "❌ Load Failed"
                r.searchCount == 0 -> "🔍 Search Empty (CF/WAF)"
                !r.loadOk -> "⚠️ ${r.error ?: "Load URL Failed"}"
                r.streamCount == 0 -> "⚠️ ${r.error ?: "No Streams"}"
                else -> "✅ ${r.streamCount} streams"
            }
            sb.appendLine("| **${r.pluginId}** | ${r.repoSlug} | $dlIcon | $loadIcon | `${r.searchQuery}` | ${r.searchCount} | $loadOkStr | ${r.streamCount} | $statusStr |")
        }

        sb.appendLine()
        sb.appendLine("## Bulunan Stream URL'leri")
        sb.appendLine()
        for (r in sortedResults.filter { it.streamCount > 0 }) {
            sb.appendLine("### ${r.pluginId} (${r.repoSlug})")
            for (url in r.streamUrls) {
                sb.appendLine("- $url")
            }
            sb.appendLine()
        }

        sb.appendLine()
        sb.appendLine("## KNOWN_BROKEN_PLUGINS Adayları")
        sb.appendLine("```kotlin")
        sb.appendLine("// Aşağıdaki eklentiler dead veya CF bloklu — KNOWN_BROKEN_PLUGINS'e ekle:")
        val broken = sortedResults.filter { !it.downloaded || (!it.loaded && it.downloaded) }
        for (r in broken) {
            sb.appendLine("\"${r.pluginId}\", // ${r.error}")
        }
        sb.appendLine("```")

        try {
            reportFile.writeText(sb.toString())
            Log.i(TAG, "📄 Rapor kaydedildi: ${reportFile.absolutePath}")
            Log.i(TAG, "\n=== E2E ÖZET ===\n✅ Çalışan: $workingCount | ⚠️ Kısmi: $searchOkNoStream | 🔍 CF Bloklu: $searchEmpty | ❌ Dead: $failLoad")
        } catch (e: Exception) {
            Log.e(TAG, "Rapor yazılamadı: ${e.message}")
        }
    }
}
}
