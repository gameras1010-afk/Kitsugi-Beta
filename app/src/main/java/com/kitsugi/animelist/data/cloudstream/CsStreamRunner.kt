package com.kitsugi.animelist.data.cloudstream

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.extractors.helper.AesHelper
import com.lagradost.cloudstream3.Prerelease
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.remote.KitsugiIdResolver
import com.kitsugi.animelist.core.player.SubtitleInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
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

    interface EmbedResolveListener {
        fun onEmbedAttempt(
            providerName: String,
            rawUrl: String,
            resolved: Boolean,
            resolvedUrl: String?,
            error: String?
        )
    }

    @Volatile
    var embedResolveListener: EmbedResolveListener? = null

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
        "asyaanimeleri.pw"    to "asyaanimeleri.top",
        "turkanime.co"        to "www.turkanime.tv",
        "animeler.me"         to "animeler.pw",
        "uqload.co"           to "uqload.is",
        "uqload.com"          to "uqload.is",
        "dood.la"             to "doodstream.com",
        "dood.wf"             to "doodstream.com",
        "dood.pm"             to "doodstream.com",
        "vidplay.online"      to "vidoy.com",
        "dropload.io"         to "dropload.tv",
        "vtube.to"            to "vtube.network",
        "vidmoly.to"          to "ww547.vidmoly.to",
        // RecTV domain geçişleri — 2026-08 tanı raporu
        // NOT: feroxx sürümü a.prectv70.lol kullanıyor (101 sonuç), b. sürümü 0 sonuç veriyor
        "prectv38.sbs"        to "a.prectv70.lol",
        "prectv43.sbs"        to "a.prectv70.lol",
        "prectv50.sbs"        to "a.prectv70.lol",
        "b.prectv70.lol"      to "a.prectv70.lol",  // maarrem/nikyokki → feroxx aktif endpoint
        "m.prectv50.sbs"      to "a.prectv70.lol",
        // DBX (DiziBox) embed player
        "dbx.molystream.org"  to "www.molystream.org",
        // DiziPal — sık domain değiştiriyor; bilinen eski sürümler
        "dizipal1571.com"     to "dizipal.bid",
        "dizipal1210.com"     to "dizipal.bid",
        "dizipal1219.com"     to "dizipal.bid",
        "dizipal1565.com"     to "dizipal.bid",
        "dizipal1570.com"     to "dizipal.bid",
        "dizipal1573.com"     to "dizipal.bid",
        "dizipal1574.com"     to "dizipal.bid",
        // DiziPalOriginal için özel aktif domain
        "dizipal2106.com"     to "dizipal1572.com",
        // DiziKorea — ölü domainler; aktif olan dizikorea3.com (feroxx sürümünde çalışıyor)
        "dizikorea2.com"      to "dizikorea3.com",
        "dizikorea.pw"        to "dizikorea3.com",
        // FilmMakinesi — güncel domain
        "filmmakinesi.sh"     to "filmmakinesi.to",
        "filmmakinesi.tv"     to "filmmakinesi.to",
        // Dizilla — eski domain yönlendirmeleri
        "dizilla40.com"       to "dizillahd.com",
        // HDFilmCehennemi — aktif mirror
        "hdfilmcehennemi.la"  to "hdfilmcehennemi.nl",
        "hdfilmcehennemi.com" to "hdfilmcehennemi.nl",
        // DiziMom — aktif domain
        "dizimom.love"        to "www.dizimom.rest",
        "dizimom.ws"          to "www.dizimom.rest",
        // DiziYou — aktif domain
        "diziyou.mx"          to "www.diziyou.one",
        "diziyou.to"          to "www.diziyou.one",
        // SezonlukDizi — eski domainler; sezonlukdizi.cc ise CF/WAF 0-stream yüzünden KNOWN_BROKEN listesine alındı
        "sezonlukdizi8.com"   to "sezonlukdizi.cc",
        "sezonlukdizi10.com"  to "sezonlukdizi.cc",
        "sezonlukdizi12.com"  to "sezonlukdizi.cc",
        // FilmModu — aktif domain
        "filmmodu.vip"        to "www.filmmodu.one",
        // FullHDFilm — hdfilm.us HTTP 200 AKTIF (2026-08 tanı düzeltmesi); cx eski ölü domain
        // "hdfilm.us" — artık domain fix gerekmez, doğrudan çalışıyor
        "fullhdfilm.cx"       to "hdfilm.us",
        "fullhdfilm.pro"      to "hdfilm.us",
        // FullHDFilmizlesene — maarrem sürümü .tv kullanıyor, feroxx/nikyokki .mx/.so (aktif)
        "fullhdfilmizlesene.tv"  to "www.fullhdfilmizlesene.mx",
        "fullhdfilmizlesene.so"  to "www.fullhdfilmizlesene.mx",
        // SinemaCX — aktif domain: sinema.gg
        "sinema.lat"          to "sinema.gg",
        "sinema.cv"           to "sinema.gg",
        // SetFilmIzle — .my boş döndürüyor, .uk de arama sıfır
        "setfilmizle.my"      to "www.setfilmizle.uk",
        // KultFilmler — nikyokki sürümü kultfilmler.pro kullanıyor (ölü), .net aktif
        "kultfilmler.pro"     to "kultfilmler.net",
        // MirrorVerse — net52.cc ölü, net77.cc aktif (Netmirror/Dismirror ile aynı)
        "net52.cc"            to "net77.cc",
        // ─── 2026-08 İkinci tanı raporu domain düzeltmeleri ─────────────────────
        // DiziLife — dizi73.life aktif ama stream yok; eski domain
        "dizi43.life"         to "dizi73.life",
        "dizi55.life"         to "dizi73.life",
        // YeniKaynak — yenikaynak.com aktif
        "yenikaynak.net"      to "www.yenikaynak.com",
        // DiziYo — www.diziyo.so aktif
        "diziyo.nl"           to "www.diziyo.so",
        "diziyo.com"          to "www.diziyo.so",
        // DiziBox — dizibox.live aktif (eski .biz .com)
        "dizibox.com"         to "www.dizibox.live",
        "dizibox.biz"         to "www.dizibox.live",
        // Dizipod — dizipod.com (aktif domain)
        "dizipod.net"         to "dizipod.com",
        // FilmEkseni — filmekseni.vip aktif
        "filmekseni.net"      to "filmekseni.vip",
        // AsyaWatch — asyawatch.com aktif
        "asyawatch.net"       to "asyawatch.com",
        "asyawatch.pw"        to "asyawatch.com",
        // AsyaFanatiklerim — asyafanatiklerim.com aktif
        "asyafanatiklerim.net" to "asyafanatiklerim.com",
        // Filmzal — filmzal.me aktif
        "filmzal.com"         to "filmzal.me",
        // JetFilmizle — jetfilmizle.now aktif (tüm eski domainler)
        "jetfilmizle.to"       to "jetfilmizle.now",
        "jetfilmizle.pro"      to "jetfilmizle.now",
        "jetfilmizle.website"  to "jetfilmizle.now",
        "jetfilmizle.ltd"      to "jetfilmizle.now",
        // ─── 2026-08 Tanı raporu sonrası ek domain düzeltmeleri ─────────────────
        // TRasyalog — asyalog.co feroxx repoda arama yapıyor ama stream çıkmıyor;
        // maarrem sürümü asyalog.com kullanıyor — ikisini de aktif domain'e yönlendir
        "asyalog.com"          to "asyalog.co",
        // SelcukFlix — nikyokki selcukflix.net kullanıyor (0 sonuç); .co aktif endpoint
        "selcukflix.net"       to "selcukflix.co",
        // WebteIzle — webteizle.info (maarrem/nikyokki) 0 stream; feroxx webteizle3.xyz çalışıyor
        "webteizle.info"       to "webteizle3.xyz",
        "webteizle2.xyz"       to "webteizle3.xyz",
        // DiziKorea — tüm eski domain varyantları aktif dizikorea3.com'a yönlendirildi (2026-08)
        // (zaten yukarıda mevcut, ek varyantlar)
        "dizikorea4.com"       to "dizikorea3.com",
        "dizikorea5.com"       to "dizikorea3.com",
        // FilmMakinesi — filmmakinesi.net eski domain
        "filmmakinesi.net"     to "filmmakinesi.to",
        // DiziPalOriginal — 1572 aktif; diğer eski numaralı domainler
        "dizipal2107.com"      to "dizipal1572.com",
        "dizipal2108.com"      to "dizipal1572.com",
        // ─── 2026-08 Cs-Karma-master tanı raporu domain düzeltmeleri ────────────
        // CinemaCity — cinemacity.cc CF/403 yeni aktif domain: cinemacity.rip
        "cinemacity.cc"        to "cinemacity.rip",
        // DiziPal — warmup ve eklenti domain yönlendirmesi
        "dizipal.bid"          to "dizipal1565.com",
        // CizgiMax — aktif domain: cizgimax.online
        "cizgiduo.online"      to "cizgimax.online",
        // ─── 2026-08 Stream problemi olan eklentiler için embed düzeltmeleri ──
        // JetFilmizle — s2.videolar.biz DNS hatası; d2rs.com aktif embed (HTTP 200)
        "s2.videolar.biz"      to "d2rs.com",
        "videolar.biz"         to "d2rs.com",
        // SetFilmIzle — setplay.shop HTTP 403; vctplay.site aktif (HTTP 200)
        "setplay.shop"         to "vctplay.site"
    )

    private val KNOWN_BROKEN_PLUGINS = emptySet<String>()


    /**
     * Yetişkin (+18) içerik sağlayıcı eklentileri.
     *
     * Bu liste dinamik olarak `showAdultContent` ayarıyla kontrol edilir:
     * - `showAdultContent = false` (varsayılan): Bu eklentiler stream çözümlemesinde engellenir.
     * - `showAdultContent = true`: Kullanıcı onayıyla eklentiler stream pipeline'ından geçer.
     *
     * Uygulama genelindeki `blurAdultMedia` ayarı ise UI katmanında bu eklentilerin
     * döndürdüğü thumbnail/artwork'leri blurlamak için kullanılır.
     *
     * @see showAdultContent
     * @see setShowAdultContent
     */
    private val ADULT_PLUGINS = setOf(
        // ─── Genel Yetişkin / Porno Siteleri ────────────────────────────────────
        "FullPorner", "Hqporner", "HQPorner", "PornoAnne", "XNXX", "Xhamster", "xHamster", "Kalite18",
        "EU", "XXXChina", "XChina", "AdultTvChannels", "Aki", "Allpornstream", "AZNude",
        "BadTv", "Beeg", "CamCaps", "CamWh", "Chatrubate", "Cloudbate",
        "CollectionOfBestPorn", "CosXPlay", "Cumlouder", "Doeda", "DoedaOrijinal",
        "Domlepen", "EFukt", "EPorner", "Erome", "EroThots", "Evooli",
        "FamilyPorn", "Fapix", "FitNakedGirls", "FreePornVideos", "FreeUsePorn",
        "Fyptt", "Girlsswallowed", "Heavy", "HentaiCity",
        "Hentaila", "HentaiWorld", "Hentaizm", "Hanime", "HanimeTV", "HentaizmManga",
        "HotLeak", "HQCollect",
        "İnfluencerChicks", "InfluencerChicks", "JavGuru", "Javseen",
        "Javtiful", "Kopeda", "Koreanpornmovie", "Koreaye", "LiveCamRips",
        "Maheir", "Mangoporn", "MilfNut", "MissAV", "Motherless", "NetFapX",
        "New", "NoodleMagazine", "Notmik", "OnScreens", "OppaiStream",
        "PerfectGirls", "PerverZija", "Pimpbunny", "Pinkueiga", "PMVHaven",
        "Porn36", "PornHat", "PornHub", "Pornocarioca", "Pornslash", "Porntrex",
        "PornWatch", "Redgifs", "RoshyTv", "RouVideo", "Rule34Video", "Rusporn",
        "Sexfilm", "Sextb", "Sxyprn", "Temel", "ThaiPorn", "ThotDeep",
        "Tubepornclassic", "TurkHub", "Turkifsahub", "TurkIfsalar", "TurkPorno",
        "Vsex", "WatchHentai", "Wumaobi", "XMoviesForYou",
        "XNalgas", "Xpaja", "XRares", "xVideos", "XXXParodyHD", "YouJizz",
        "Youperv", "YTBoob", "AnimeAV", "Stripchat", "MomLover", "PornoLandia",
        "PornoLava", "SuperErotikGeldi", "IncestFlix", "XnxxProvider", "XhamsterProvider",
        "AllClassicPorn", "Coomer", "DirtyShip", "Porn00", "SimpCity", "UnusualPornX",
        "VideoCelebs", "WatchPorn", "EPawg"
    )

    /** [CsPluginDiagnosticRunner] gibi dış sınıfların KNOWN_BROKEN_PLUGINS listesine erişimi için. */
    val KNOWN_BROKEN_PLUGINS_SET: Set<String> get() = KNOWN_BROKEN_PLUGINS

    /** Dış sınıfların ADULT_PLUGINS listesine erişimi için. */
    val ADULT_PLUGINS_SET: Set<String> get() = ADULT_PLUGINS

    /**
     * Kullanıcının "+18 içerik" tercihini yansıtır.
     *
     * - `false` (varsayılan): [ADULT_PLUGINS] listesindeki eklentiler [getStreams]'de engellenir.
     * - `true`: Adult eklentiler stream pipeline'ından geçer (kullanıcı onayladı).
     *
     * Bu değer [setShowAdultContent] ile güncellenir; [AppRootSettingsExtras.onAdultContentChanged]
     * ve [KitsugiApplication] startup'ında [SettingsDataStore]'dan okunarak senkronize edilir.
     */
    @Volatile
    var showAdultContent: Boolean = false
        private set

    /**
     * `showAdultContent` değerini günceller.
     * UI ayar değişikliğinde veya uygulama başlangıcında çağrılmalıdır.
     */
    fun setShowAdultContent(show: Boolean) {
        if (showAdultContent != show) {
            showAdultContent = show
            android.util.Log.i(TAG, "+18 içerik filtresi güncellendi: showAdultContent=$show")
        }
    }

    /**
     * Kalıcı olarak kapalı veya ölü olduğu bilinen alan adları.
     * Eklentinin mainUrl değeri bu domainlerden birini içeriyorsa eklenti direkt atlanır.
     */
    private val KNOWN_BROKEN_DOMAINS = setOf(
        // ─── DNS çözümlenemeyen / tamamen kapalı domainler ────────────────────
        "ifsalog4.club",
        "superfilmgeldi12.art",
        "superfilmgeldi13.art",
        "ugurfilm1.xyz",
        "ugurfilm3.xyz",
        "prectv38.sbs",         // RecTV eski domain
        "prectv43.sbs",         // RecTV eski domain
        "koreanturk.net",       // Origin Connection Timeout (522)
        "roketdizi.live",       // UnknownHostException — tanı 2026-08
        "wfilmizle.art",        // UnknownHostException — tanı 2026-08
        "filmizlesene.plus",    // UnknownHostException — tanı 2026-08
        "fullhdfilm.cx",        // UnknownHostException — tanı 2026-08
        "kultfilmler.pro",      // UnknownHostException — tanı 2026-08
        "hdabla.net",           // CF/WAF — hiçbir aramada sonuç yok
        // ─── 2026-08 Tanı raporu ek ölü domainler ────────────────────────────
        "net52.cc",             // MirrorVerse eski domain (domain fix ile net77'ye yönlendiriliyor)
        "tafdi.info",           // Tafdi — 0 sonuç, DNS yok
        "dizimag.mom",          // DiziMag — 0 sonuç
        "4kfilmizlesene.nl",    // 4KFilmIzlesene — 0 sonuç
        "hdfilmcehennemi2.site",// HDFilmCehennemi2 — ölü mirror
        "hdfilmizle.to",        // HDFilmIzle — 0 sonuç
        "hdfilmsite.net",       // HDFilmSitesi — 0 sonuç
        "filmkovasi.pw",        // FilmKovasi — 0 sonuç
        "filmizleilk.vip",      // FilmIzleIlk — 0 sonuç
        "fullhdfilmizlede.org", // FullHDFilmİzlede — 0 sonuç
        "666filmizle.site"      // AltiYuzAltmisAltiFilmIzle — 0 sonuç
    )

    /**
     * Kalıcı olarak erişilemeyen CDN sunucuları.
     * Bu CDN'lerden gelen embed URL'leri loadExtractor'a hiç gönderilmez ve
     * ÖLÜ KANAL fallback olarak da kullanıcıya sunulmaz — tamamen sessizce atlanır.
     */
    private val KNOWN_DEAD_CDN_HOSTS = setOf(
        "pichive.online",        // FourPichive / AsyaWatch / Dizilla — her zaman ÖLÜ KANAL
        "pichive.cc",            // Pichive CDN alternatif alan
        "sssrr.org",             // Abyss CDN (AsyaAnimeleri) — her zaman ÖLÜ KANAL
        "abyss.to",              // Abyss servis alanı (yedek)
        "vmnow.online"           // VidMoly CDN yeni alt domain — 0 stream; SezonlukDizi embed CDN'i
    )

    /**
     * Cloudflare / WAF challange nedeniyle yavaş çalışan eklentiler.
     * Bu eklentiler için:
     *  1. ID doğrulaması (syncData kontrolü) atlanır — her load() çağrısı CF timeout riski taşıdığından
     *  2. Timeout PROVIDER_TIMEOUT_MS yerine WEBVIEW_PROVIDER_TIMEOUT_MS kullanılır
     * TrAnimeci: tranimaci.com Custom WAF Security Verification — CloudflareKiller bile takılıyor
     */
    internal val CF_PROTECTED_PLUGINS = setOf(
        "TrAnimeci",
        "TrAnimeIzle",
        // DiziBox — molystream.org embed CDN CF korumalı; loadLinks 25s timeout'ta kilitlendi (2026-08 tanı)
        "DiziBox",
        // TurkAnime — turkanime.tv CF + AES şifreli player; 25s yeterli değil
        "TurkAnime"
    )

    /** CF korumalı eklentiler için uzatılmış timeout (90 saniye) */
    private const val CF_PROVIDER_TIMEOUT_MS = 90_000L

    private val dynamicDomains = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val dynamicBlockedPlugins = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val isDomainListFetched = java.util.concurrent.atomic.AtomicBoolean(false)
    private val runnerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    /**
     * Uzak domain listesini arka planda çeker (henüz çekilmediyse).
     * Uygulama açılışında ve her arama başında otomatik tetiklenir.
     */
    fun triggerRemoteDomainsFetch() {
        if (!isDomainListFetched.get()) {
            runnerScope.launch { fetchRemoteDomains() }
        }
    }

    /**
     * Uzak domain listesi önbelleğini sıfırlar ve yeniden çeker.
     * Beni ara: "Domain yenile" dediğinde bunu çağırıyoruz.
     */
    fun forceRefreshDomains() {
        isDomainListFetched.set(false)
        dynamicDomains.clear()
        dynamicBlockedPlugins.clear()
        runnerScope.launch { fetchRemoteDomains() }
        Log.i(TAG, "Remote domain cache temizlendi, yeniden çekiliyor...")
    }

    /**
     * Domain listesini GitHub'daki domain_fixes.json dosyasından çeker.
     * URL: https://raw.githubusercontent.com/gameras1010-afk/Kitsugi-Beta/main/domain_fixes.json
     *
     * Dosyayı güncellemek için:
     *   1. Kitsugi-Beta/domain_fixes.json dosyasını düzenle
     *   2. git push yap (veya GitHub_Upload.bat çalıştır)
     *   3. Kullanıcılar uygulamayı kapatıp açtığında yeni domainleri otomatik çeker
     */
    private suspend fun fetchRemoteDomains() = withContext(Dispatchers.IO) {
        if (isDomainListFetched.getAndSet(true)) return@withContext
        try {
            Log.d(TAG, "Fetching remote domain list from GitHub (gameras1010-afk/Kitsugi-Beta)...")
            val request = okhttp3.Request.Builder()
                .url("https://raw.githubusercontent.com/gameras1010-afk/Kitsugi-Beta/main/domain_fixes.json")
                .header("Cache-Control", "no-cache")
                .build()
            val json = com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
                response.body?.string() ?: ""
            }

            // Parse JSON: { "domains": { "eklentiadi": "https://..." } }
            val jsonObj = org.json.JSONObject(json)
            val domainsObj = jsonObj.optJSONObject("domains") ?: run {
                Log.w(TAG, "domain_fixes.json içinde 'domains' anahtarı bulunamadı.")
                isDomainListFetched.set(false)
                return@withContext
            }
            var loaded = 0
            for (key in domainsObj.keys()) {
                val url = domainsObj.optString(key, "")
                if (key.isNotEmpty() && url.startsWith("http")) {
                    dynamicDomains[key.lowercase(Locale.ROOT)] = url
                    loaded++
                }
            }

            // Parse dynamic blocked plugins: { "blocked": ["eklenti1", "eklenti2"] }
            val blockedArr = jsonObj.optJSONArray("blocked")
            val blockedSet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
            if (blockedArr != null) {
                for (i in 0 until blockedArr.length()) {
                    val pName = blockedArr.optString(i, "")
                    if (pName.isNotEmpty()) {
                        blockedSet.add(pName.lowercase(Locale.ROOT))
                    }
                }
            }
            dynamicBlockedPlugins.clear()
            dynamicBlockedPlugins.addAll(blockedSet)

            Log.i(TAG, "✅ Uzak domain listesi başarıyla yüklendi: $loaded eklenti domaini güncellendi, ${dynamicBlockedPlugins.size} eklenti engellendi.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Uzak domain listesi çekilemedi: ${e.message}")
            // Bayrak sıfırlanır — bir sonraki açılışta tekrar denenecek
            isDomainListFetched.set(false)
        }
    }

    /**
     * Plugin'in mainUrl'sini bilinen eski→yeni domain eşlemeleriyle günceller.
     * Eğer plugin zaten doğru domain'i kullanıyorsa hiçbir şey değişmez.
     */
    internal fun applyDomainFix(api: MainAPI) {
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
        if (currentUrl.isBlank()) return
        
        try {
            val uri = java.net.URI(currentUrl)
            val host = uri.host ?: ""
            if (host.isNotBlank()) {
                for ((oldDomain, newDomain) in KNOWN_DOMAIN_FIXES) {
                    val cleanOld = oldDomain.replace("www.", "")
                    val cleanHost = host.replace("www.", "")
                    if (cleanHost.equals(cleanOld, ignoreCase = true) || cleanHost.contains(cleanOld, ignoreCase = true)) {
                        val cleanNew = newDomain.replace("www.", "")
                        val preferredHost = if (host.startsWith("www.") || newDomain.startsWith("www.")) "www.$cleanNew" else cleanNew
                        
                        val scheme = uri.scheme ?: "https"
                        val path = uri.rawPath ?: ""
                        val query = uri.rawQuery?.let { "?$it" } ?: ""
                        val fragment = uri.rawFragment?.let { "#$it" } ?: ""
                        
                        val fixedUrl = "$scheme://$preferredHost$path$query$fragment"
                        Log.w(TAG, "[${api.name}] Domain lokal kuralla (Uri) düzeltildi: $currentUrl -> $fixedUrl")
                        api.mainUrl = fixedUrl
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse URI for domain fix: ${e.message}")
        }

        // Fallback to simple replace if URI parsing failed
        for ((oldDomain, newDomain) in KNOWN_DOMAIN_FIXES) {
            if (currentUrl.contains(oldDomain)) {
                val fixed = currentUrl.replace(oldDomain, newDomain)
                val deduplicated = fixed.replace("www.www.", "www.")
                Log.w(TAG, "[${api.name}] Domain lokal kuralla (Fallback) düzeltildi: $currentUrl -> $deduplicated")
                api.mainUrl = deduplicated
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

        // Dinamik olarak engellenmiş (ölü/bozuk) eklentileri atla
        val nameKey = api.name.lowercase(Locale.ROOT)
        if (nameKey in dynamicBlockedPlugins) {
            Log.w(TAG, "[${api.name}] Dinamik engelli listesinde (domain_fixes.json) — atlanıyor.")
            return@withContext emptyList()
        }

        // Kalıcı bozuk olduğu bilinen plugin'leri direkt atla — ağ kaynağı harcama
        if (api.name in KNOWN_BROKEN_PLUGINS) {
            Log.w(TAG, "[${api.name}] KNOWN_BROKEN_PLUGINS listesinde — atlanıyor.")
            return@withContext emptyList()
        }

        // ── +18 İçerik Filtresi ──────────────────────────────────────────────────
        // showAdultContent = false (varsayılan) → ADULT_PLUGINS listesindeki eklentiler engellenir.
        // showAdultContent = true → kullanıcı +18 içeriklere izin verdi; eklenti çalışır.
        if (!showAdultContent && api.name in ADULT_PLUGINS) {
            Log.w(TAG, "[${api.name}] ADULT_PLUGINS listesinde ve showAdultContent=false — atlanıyor. (+18 filtre aktif)")
            return@withContext emptyList()
        }
        if (showAdultContent && api.name in ADULT_PLUGINS) {
            Log.d(TAG, "[${api.name}] ADULT_PLUGINS listesinde ama showAdultContent=true — devam ediliyor.")
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

        // ── Native ID Resolution Shortcut ────────────────────────────────────
        // If the plugin declares supportedSyncNames, try getLoadUrl() first.
        // This completely bypasses fuzzy title-matching and is far more reliable.
        val nativeResult = tryNativeIdResolution(
            api       = api,
            malId     = malId,
            aniListId = resolvedIds?.aniListId ?: aniListId,
            tmdbId    = resolvedIds?.tmdbId    ?: tmdbId,
            imdbId    = targetImdb,
            kitsuId   = targetKitsu,
            season    = season,
            episode   = episode
        )
        if (nativeResult != null) {
            Log.d(TAG, "[${api.name}] ⚡ Native getLoadUrl çözümü başarılı — arama atlandı (${nativeResult.size} stream)")
            return nativeResult
        }

        // Build title variants: original + normalized + alts
        val titleVariants = buildTitleVariants(title, alternativeTitles, season)
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
                ?: bestLoadResponse.url.takeIf { it.isNotBlank() }?.also {
                    Log.w(TAG, "[${api.name}] findEpisodeData null — URL fallback: $it (powerDizi/XPrime style)")
                }
            if (episodeData == null) {
                Log.w(TAG, "[${api.name}] S${season}E${episode} ve URL fallback da başarısız.")
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

        // findEpisodeData null döndürürse (powerDizi/XPrime gibi providerlar loadLinks'e URL geçiyor),
        // loadResponse.url'yi fallback olarak kullan.
        val episodeData = findEpisodeData(loadResponse, season, episode)
            ?: loadResponse.url.takeIf { it.isNotBlank() }?.also {
                Log.w(TAG, "[${api.name}] findEpisodeData null — URL fallback: $it (powerDizi/XPrime style)")
            }
        if (episodeData == null) {
            Log.w(TAG, "[${api.name}] S${season}E${episode} bulunamadı. LoadResponse tipi: ${loadResponse.javaClass.simpleName}")
            return emptyList()
        }

        Log.d(TAG, "[${api.name}] S${season}E${episode} için episodeData bulundu")
        return extractStreamsFromEpisode(api, loadResponse, episodeData)
    }

    /**
     * Plugin'in native getLoadUrl() metodunu kullanarak direkt URL çözümü dener.
     * Plugin `supportedSyncNames` listesinde MAL/AniList/TMDB/IMDb ID'lerinden birini
     * destekliyorsa, getLoadUrl() metodunu çağırarak search+match adımlarını atlar.
     *
     * @return Başarılı olursa stream listesi, getLoadUrl desteklenmiyor veya başarısız ise null
     */
    private suspend fun tryNativeIdResolution(
        api: MainAPI,
        malId: Int?,
        aniListId: Int?,
        tmdbId: Int?,
        imdbId: String?,
        kitsuId: Int?,
        season: Int,
        episode: Int
    ): List<StreamSource>? {
        if (api.supportedSyncNames.isEmpty()) return null

        // Build candidate (syncName → idString) pairs in priority order
        val syncCandidates = buildList {
            if (com.lagradost.cloudstream3.syncproviders.SyncIdName.MyAnimeList in api.supportedSyncNames && malId != null && malId > 0) {
                add(com.lagradost.cloudstream3.syncproviders.SyncIdName.MyAnimeList to malId.toString())
            }
            if (com.lagradost.cloudstream3.syncproviders.SyncIdName.Anilist in api.supportedSyncNames && aniListId != null && aniListId > 0) {
                add(com.lagradost.cloudstream3.syncproviders.SyncIdName.Anilist to aniListId.toString())
            }
            if (com.lagradost.cloudstream3.syncproviders.SyncIdName.Imdb in api.supportedSyncNames && !imdbId.isNullOrBlank()) {
                add(com.lagradost.cloudstream3.syncproviders.SyncIdName.Imdb to imdbId)
            }
            if (com.lagradost.cloudstream3.syncproviders.SyncIdName.Kitsu in api.supportedSyncNames && kitsuId != null && kitsuId > 0) {
                add(com.lagradost.cloudstream3.syncproviders.SyncIdName.Kitsu to kitsuId.toString())
            }
            // TMDB is not in SyncIdName enum but some providers may map it via custom keys
        }

        if (syncCandidates.isEmpty()) return null

        for ((syncName, syncId) in syncCandidates) {
            try {
                Log.d(TAG, "[${api.name}] getLoadUrl($syncName, $syncId) deneniyor...")
                val loadUrl = withTimeoutOrNull(15_000L) {
                    api.getLoadUrl(syncName, syncId)
                }
                if (!loadUrl.isNullOrBlank()) {
                    Log.d(TAG, "[${api.name}] ✓ getLoadUrl başarılı: $loadUrl")
                    val fakeSearchResponse = api.newAnimeSearchResponse(
                        name = api.name,
                        url  = loadUrl,
                        type = com.lagradost.cloudstream3.TvType.Anime,
                        fix  = false
                    )
                    return loadAndExtractStreams(api, fakeSearchResponse, season, episode)
                } else {
                    Log.d(TAG, "[${api.name}] getLoadUrl($syncName, $syncId) → null (desteklenmiyor veya bulunamadı)")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Throwable) {
                Log.w(TAG, "[${api.name}] getLoadUrl($syncName, $syncId) HATA: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        return null // Hiçbir sync ID çalışmadı — normal arama akışına dön
    }
    /**
     * URL'nin bir embed/iframe video oynatıcı sayfası olup olmadığını kontrol eder.
     * Doğrudan .mp4/.m3u8/.mpd dosyaları veya bilinen video akışları false döndürür.
     */
    internal fun isEmbedUrl(url: String): Boolean {
        val clean = resolveHrefLi(url).lowercase(Locale.ROOT)
        val path = try {
            java.net.URI(clean).path?.lowercase(Locale.ROOT) ?: ""
        } catch (_: Exception) {
            clean.substringBefore('?').lowercase(Locale.ROOT)
        }.trimEnd('/')
        val isDirectMedia = path.endsWith(".m3u8") ||
                            path.endsWith(".mp4") ||
                            path.endsWith(".mpd") ||
                            path.endsWith(".mkv") ||
                            path.endsWith(".avi") ||
                            path.endsWith(".webm") ||
                            path.endsWith("master.txt") ||
                            path.endsWith("playlist.txt") ||
                            path.contains(".m3u8/") ||
                            path.contains(".mp4/")
        if (isDirectMedia && !clean.contains("href.li/?")) {
            return false
        }
        return clean.contains("vk.com/video") ||
               clean.contains("vkvideo.ru") ||
               clean.contains("sibnet") ||
               // VidMoly — ana alan + tüm CDN subdomainleri (ww1.vidmoly.to, ww547.vidmoly.to, vmnow.online vb.)
               clean.contains("vidmoly") ||
               clean.contains("vmnow.online") ||      // VidMoly yeni CDN alt alanı (SezonlukDizi)
               clean.contains("filemoon") ||
               clean.contains("fmoonembed") ||        // Filemoon embed CDN
               clean.contains("ok.ru") ||
               clean.contains("odnoklassniki") ||
               clean.contains("streamtape") ||
               clean.contains("streamwish") ||
               clean.contains("swdyu") ||
               clean.contains("sfastwish") ||
               clean.contains("wishfast") ||
               clean.contains("dood") ||
               clean.contains("playmogo") ||          // Doodstream redirect
               clean.contains("ds2play") ||
               clean.contains("mixdrop") ||
               clean.contains("voe.sx") ||
               clean.contains("vidplay") ||
               clean.contains("vidoy") ||             // Vidplay redirect
               clean.contains("uqload") ||            // Uqload redirect
               clean.contains("dropload") ||          // Dropload redirect
               clean.contains("vtube") ||             // Vtube redirect
               clean.contains("rapidrame") ||         // Rapidrame CDN (HDFilmCehennemi)
               clean.contains("hdplayersystem") ||    // DiziMom CDN player
               clean.contains("pichive.online") ||    // FourPichive (AsyaWatch, Dizilla)
               clean.contains("pichive.cc") ||        // Pichive CDN alternatif
               clean.contains("sssrr.org") ||         // Abyss CDN (AsyaAnimeleri)
               clean.contains("molystream.org") ||    // Molystream (DiziBox)
               clean.contains("streambox.xyz") ||     // Streambox CDN (DDizi)
               clean.contains("turkanime.tv/embed") || // TurkAnime AES embed player
               clean.contains("turkanime.co/embed") || // TurkAnime AES embed player (eski domain)
               clean.contains("closeload.top") ||     // CloseLoad (Türkiye CDN)
               clean.contains("gstore.one") ||        // GStore (Türkiye CDN)
               clean.contains("trstx.org") ||         // TRsTX (Türkiye CDN)
               clean.contains("chillx.top") ||        // Chillx embed
               clean.contains("boosterx.stream") ||   // Boosterx embed
               clean.contains("kwik.si") ||           // Kwik (GogoAnime)
               clean.contains("kwik.cx") ||           // Kwik (GogoAnime alt)
               clean.contains("megacloud.tv") ||      // MegaCloud (HiAnime)
               clean.contains("rapidcloud.cc") ||     // Rapidcloud
               clean.contains("vudeo.net") ||         // Vudeo (Asyalog)
               clean.contains("vudeo.org") ||         // Vudeo mirror
               clean.contains("wishembed.pro") ||     // StreamWish alt (Asyalog "Wish" player)
               clean.contains("awish.pro") ||         // StreamWish alt
               clean.contains("alions.pro") ||        // AlionsPlayer (yeni Türkiye CDN)
               clean.contains("lionscdn.pro") ||      // LionsCDN (AlionsPlayer yedek)
               clean.contains("embed") ||
               clean.contains("shell.php") ||
               clean.contains("video_ext.php") ||
               clean.contains("player.php") ||
               url.contains("href.li/?")
    }


    internal fun resolveHrefLi(url: String): String {
        var target = if (url.contains("href.li/?", ignoreCase = true)) {
            val lowerUrl = url.lowercase(Locale.ROOT)
            val idx = lowerUrl.indexOf("href.li/?")
            val resolved = url.substring(idx + "href.li/?".length)
            Log.d(TAG, "[HrefLi] href.li resolved: $url → $resolved")
            resolved
        } else {
            url
        }
        
        var fixed = target
        for ((oldDomain, newDomain) in KNOWN_DOMAIN_FIXES) {
            if (fixed.contains(oldDomain)) {
                fixed = fixed.replace(oldDomain, newDomain)
            }
        }
        fixed = fixed.replace("www.www.", "www.")
        
        if (fixed != target) {
            Log.d(TAG, "[DomainFix] URL domain fixed: $target → $fixed")
        }
        return fixed
    }

    /**
     * LoadResponse nesnesinden posterUrl alanını reflection ile okur.
     * Cloudstream API sürümleri arasında alan adı farklılık gösterebileceğinden
     * birden fazla olası isim denenir.
     */
    private fun extractPosterUrl(response: LoadResponse): String? {
        val candidates = listOf("posterUrl", "poster", "posterimage", "coverImage", "coverUrl", "backgroundPosterUrl")
        for (fieldName in candidates) {
            try {
                var clazz: Class<*>? = response.javaClass
                while (clazz != null) {
                    try {
                        val field = clazz.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val value = field.get(response)
                        if (value is String && value.isNotBlank()) return value
                    } catch (_: NoSuchFieldException) {}
                    clazz = clazz.superclass
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Embed URL'yi (VK, Sibnet, Vidmoly, Filemoon, Okru, StreamWish vb.) CS3 loadExtractor sistemiyle çözer.
     * CS3 ExtractorApi'leri gerçek .mp4/.m3u8 URL'lerini ve gerekli HTTP başlıklarını döndürür.
     *
     * @OptIn Prerelease: AesHelper.cryptoAESHandler TurkAnime AES-128 şifreli embed URL'leri için kullanılıyor.
     */
    @OptIn(Prerelease::class)
    internal suspend fun resolveEmbedUrl(
        providerName: String,
        rawUrl: String,
        rawLinkName: String,
        rawHeaders: Map<String, String>,
        referer: String,
        subtitleCallback: (SubtitleInput) -> Unit,
        thumbnailUrl: String? = null
    ): List<StreamSource> {
        val resolvedUrl = resolveHrefLi(rawUrl)
        Log.d(TAG, "[$providerName] Embed URL çözümleniyor: $resolvedUrl")

        // ── TurkAnime AES-128 intercept ──────────────────────────────────────
        // turkanime.tv/embed/#/url/<BASE64(AES_encrypted_CDN_url)> formatını
        // orijinal plugin'in kendi iframe2AesLink() metoduyla aynı mantıkla çözer.
        val isTurkAnimeEmbed = resolvedUrl.contains("turkanime.tv/embed", ignoreCase = true) ||
                               resolvedUrl.contains("turkanime.co/embed", ignoreCase = true)
        if (isTurkAnimeEmbed) {
            Log.d(TAG, "[$providerName] TurkAnime AES embed tespit edildi — çözülüyor…")
            try {
                val aesData = resolvedUrl
                    .substringAfter("embed/#/url/")
                    .substringBefore("?status")
                    .substringBefore("#")
                val base64Decoded = String(Base64.decode(aesData, Base64.DEFAULT))
                val aesKey = "710^8A@3@>T2}#zN5xK?kR7KNKb@-A!LzYL5~M1qU0UfdWsZoBm4UUat%}ueUv6E--*hDPPbH7K2bp9^3o41hw,khL:}Kx8080@M"
                val decrypted = AesHelper.cryptoAESHandler(base64Decoded, aesKey.toByteArray(), false)
                    ?.replace("\\", "")
                    ?.replace("\"", "")
                    ?.trim()
                if (!decrypted.isNullOrBlank() && (decrypted.startsWith("http://") || decrypted.startsWith("https://"))) {
                    Log.i(TAG, "[$providerName] TurkAnime AES çözüldü → $decrypted")
                    // Çözülen URL'yi tekrar normal embed pipeline'ından geçir
                    return resolveEmbedUrl(
                        providerName    = providerName,
                        rawUrl          = decrypted,
                        rawLinkName     = rawLinkName,
                        rawHeaders      = rawHeaders,
                        referer         = "https://www.turkanime.tv/",
                        subtitleCallback = subtitleCallback,
                        thumbnailUrl    = thumbnailUrl
                    )
                } else {
                    Log.w(TAG, "[$providerName] TurkAnime AES çözüldü ama URL geçersiz: $decrypted")
                }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "[$providerName] TurkAnime AES decrypt hatası: ${e.message}")
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        val resolvedStreams = mutableListOf<StreamSource>()
        var errorMsg: String? = null
        try {
            val timedOut = withTimeoutOrNull(20_000L) {
                try {
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
                                    subtitles      = emptyList(),
                                    thumbnailUrl   = thumbnailUrl,
                                    isAdultContent = ADULT_PLUGINS.contains(providerName)
                                )
                            )
                        }
                    )
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    errorMsg = "${e.javaClass.simpleName}: ${e.message}"
                    Log.e(TAG, "[$providerName] loadExtractor HATA: $errorMsg")
                }
            } == null
            if (timedOut && errorMsg == null) {
                errorMsg = "Timeout: 20 seconds exceeded"
                Log.w(TAG, "[$providerName] loadExtractor 20 saniyelik zaman aşımına uğradı.")
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorMsg = "${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "[$providerName] loadExtractor HATA: $errorMsg")
        }

        // ── Custom Wrapper Fallback — CS3 loadExtractor başarısız olduğunda ──
        // CS3 kütüphanesindeki built-in extractor'lar bazı durumlarda boş dönebilir
        // (özellikle kütüphane versiyonu eski olduğunda). Bu durumda Kitsugi'nin
        // kendi regex-tabanlı fallback wrapper'larını dener.
        if (resolvedStreams.isEmpty()) {
            try {
                val needsHtml = com.kitsugi.animelist.data.cloudstream.extractors.McloudWrapper.isMcloudUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.MixdropWrapper.isMixdropUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.Mp4UploadWrapper.isMp4UploadUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.StreamTapeWrapper.isStreamTapeUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.UpstreamWrapper.isUpstreamUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.VoeWrapper.isVoeUrl(resolvedUrl) ||
                        com.kitsugi.animelist.data.cloudstream.extractors.XStreamCdnWrapper.isXStreamCdnUrl(resolvedUrl)

                val pageContent = if (needsHtml) {
                    try {
                        Log.d(TAG, "[$providerName] Fallback için HTML çekiliyor: $resolvedUrl")
                        com.lagradost.cloudstream3.app.get(
                            url = resolvedUrl,
                            referer = referer
                        ).text
                    } catch (e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e(TAG, "[$providerName] Fallback HTML çekme hatası: ${e.message}")
                        ""
                    }
                } else ""

                val wrapperVideos: List<com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo> = when {
                    com.kitsugi.animelist.data.cloudstream.extractors.McloudWrapper.isMcloudUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Mcloud wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.McloudWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.MixdropWrapper.isMixdropUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Mixdrop wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.MixdropWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.Mp4UploadWrapper.isMp4UploadUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Mp4Upload wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.Mp4UploadWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.StreamTapeWrapper.isStreamTapeUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] StreamTape wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.StreamTapeWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.UpstreamWrapper.isUpstreamUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Upstream wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.UpstreamWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.VoeWrapper.isVoeUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Voe wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.VoeWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.XStreamCdnWrapper.isXStreamCdnUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] XStreamCDN wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.XStreamCdnWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    com.kitsugi.animelist.data.cloudstream.extractors.GdriveplayerWrapper.isGdriveplayerUrl(resolvedUrl) -> {
                        Log.d(TAG, "[$providerName] Gdriveplayer wrapper fallback deneniyor...")
                        com.kitsugi.animelist.data.cloudstream.extractors.GdriveplayerWrapper
                            .extractFromHtml(pageContent, resolvedUrl)
                            .map { com.kitsugi.animelist.data.cloudstream.extractors.JWPlayerWrapper.ExtractedVideo(it.url, it.label, it.isM3u8, it.headers) }
                    }
                    else -> emptyList()
                }
                wrapperVideos.forEach { vid ->
                    if (vid.url.isNotBlank() && vid.url.startsWith("http")) {
                        val wHeaders = vid.headers.toMutableMap()
                        if (!wHeaders.keys.any { it.equals("user-agent", ignoreCase = true) }) {
                            wHeaders["User-Agent"] = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
                        }
                        resolvedStreams.add(
                            StreamSource(
                                addonName      = providerName,
                                name           = "$providerName • ${vid.label}",
                                title          = vid.label,
                                url            = vid.url,
                                infoHash       = null,
                                fileIndex      = null,
                                requestHeaders = wHeaders,
                                isCS           = true,
                                quality        = getQualityString(if (vid.label.contains("1080")) 1080 else if (vid.label.contains("720")) 720 else if (vid.label.contains("480")) 480 else 0),
                                qualityValue   = if (vid.label.contains("1080")) 1080 else if (vid.label.contains("720")) 720 else if (vid.label.contains("480")) 480 else 0,
                                subtitles      = emptyList(),
                                thumbnailUrl   = thumbnailUrl,
                                isAdultContent = ADULT_PLUGINS.contains(providerName)
                            )
                        )
                        Log.i(TAG, "[$providerName] ✅ Custom wrapper fallback başarılı: ${vid.url.take(80)}")
                    }
                }
            } catch (wrapErr: Throwable) {
                if (wrapErr is kotlinx.coroutines.CancellationException) throw wrapErr
                Log.w(TAG, "[$providerName] Custom wrapper fallback hatası: ${wrapErr.message}")
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        val resolved = resolvedStreams.isNotEmpty()
        val finalError = if (resolved) null else (errorMsg ?: "No links extracted / Host unsupported or offline")
        embedResolveListener?.onEmbedAttempt(
            providerName = providerName,
            rawUrl = rawUrl,
            resolved = resolved,
            resolvedUrl = if (resolved) resolvedStreams.firstOrNull()?.url else null,
            error = finalError
        )

        if (resolvedStreams.isEmpty()) {
            // Ölü CDN'ler için ÖLÜ KANAL fallback bile gösterme
            val isDeadCdn = KNOWN_DEAD_CDN_HOSTS.any { resolvedUrl.contains(it, ignoreCase = true) }
            if (isDeadCdn) {
                Log.w(TAG, "[$providerName] Ölü CDN — ÖLÜ KANAL fallback da atlanıyor: $rawUrl")
                return resolvedStreams // empty
            }
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
                    addonName    = providerName,
                    name         = "$providerName • $rawLinkName [ÖLÜ KANAL / İLETİŞİM HATASI]",
                    title        = "$rawLinkName [ÖLÜ KANAL / İLETİŞİM HATASI]",
                    url          = rawUrl,
                    infoHash     = null,
                    fileIndex    = null,
                    requestHeaders = headers,
                    isCS         = true,
                    quality      = "720p",
                    qualityValue = 720,
                    subtitles    = emptyList(),
                    thumbnailUrl = thumbnailUrl,
                    isAdultContent = ADULT_PLUGINS.contains(providerName)
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
        // Kaynağa özgü kapak görseli — LoadResponse.posterUrl varsa StreamSource'a aktarılır
        val posterUrl = extractPosterUrl(loadResponse)
        Log.d(TAG, "[${api.name}] posterUrl=${posterUrl ?: "(yok)"}")

        val streams = mutableListOf<StreamSource>()
        val subtitleList = mutableListOf<SubtitleInput>()
        val pendingEmbedUrls = mutableListOf<Triple<String, String, Map<String, String>>>() // (rawUrl, linkName, headers)

        try {
            // Uses LOAD semaphore — completely separate from searchSemaphore, no deadlock risk
            loadSemaphore.withPermit {
                // Throttling: kısa gecikme Cloudflare tetiklenmesini önler
                kotlinx.coroutines.delay(500)
                Log.d(TAG, "[${api.name}] loadLinks çağrılıyor...")
                
                // Wrap the loadLinks in a timeout — CF korumalı siteler için 90s, diğerleri 25s
                val loadLinksTimeoutMs = if (api.name in CF_PROTECTED_PLUGINS) CF_PROVIDER_TIMEOUT_MS else 25_000L
                withTimeoutOrNull(loadLinksTimeoutMs) {
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

                            // Görüntü dosyalarını (logo, poster) video stream olarak ekleme — SAM lambda
                            // içinde return yasak, if-guard ile filtrele
                            val lowerPath = try {
                                java.net.URI(cleanUrl.lowercase(Locale.ROOT)).path ?: cleanUrl.lowercase(Locale.ROOT)
                            } catch (_: Exception) { cleanUrl.lowercase(Locale.ROOT) }
                            val isImageUrl = lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                                lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
                                lowerPath.endsWith(".webp") || lowerPath.endsWith(".svg") ||
                                lowerPath.contains("/vod/img/")

                            if (isImageUrl) {
                                Log.w(TAG, "[${api.name}] Görüntü URL'si atlanıyor (video değil): $cleanUrl")
                            } else if (isEmbedUrl(link.url)) {
                                // Kalıcı ölü CDN'leri kuyruğa bile alma — FIX: else branch ile gerçekten atlıyoruz
                                val isDeadCdn = KNOWN_DEAD_CDN_HOSTS.any { cleanUrl.contains(it, ignoreCase = true) }
                                if (isDeadCdn) {
                                    Log.w(TAG, "[${api.name}] Ölü CDN URL'si sessizce atlanıyor: $cleanUrl")
                                } else {
                                    Log.d(TAG, "[${api.name}] Embed URL tespit edildi — extractor kuyruğuna alınıyor: $cleanUrl")
                                    pendingEmbedUrls.add(Triple(link.url, link.name, link.headers))
                                }
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
                                        addonName    = api.name,
                                        name         = "${api.name} • ${link.name}",
                                        title        = link.name,
                                        url          = cleanUrl,
                                        infoHash     = null,
                                        fileIndex    = null,
                                        requestHeaders = headers,
                                        isCS         = true,
                                        quality      = getQualityString(link.quality),
                                        qualityValue = link.quality,
                                        subtitles    = emptyList(),
                                        thumbnailUrl = posterUrl,
                                        isAdultContent = ADULT_PLUGINS.contains(api.name)
                                    )
                                )
                            }
                        }
                    )
                } ?: Log.w(TAG, "[${api.name}] loadLinks ${loadLinksTimeoutMs / 1000}s zaman aşımına uğradı, elde edilen ${streams.size} link döndürülüyor.")
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
                    providerName     = api.name,
                    rawUrl           = rawUrl,
                    rawLinkName      = linkName,
                    rawHeaders       = rawHeaders,
                    referer          = referer,
                    subtitleCallback = { sub -> subtitleList.add(sub) },
                    thumbnailUrl     = posterUrl
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
                                subtitles      = emptyList(),
                                thumbnailUrl   = posterUrl,
                                isAdultContent = ADULT_PLUGINS.contains(api.name)
                            )
                        )
                    } else {
                        Log.w(TAG, "[${api.name}] Extractor çözemedi ve HTML iframe URL'si — oynatılamayacağı için atlandı: $clean")
                    }
                }
            }
        }

        Log.d(TAG, "[${api.name}] Ham sonuç: ${streams.size} stream, ${subtitleList.size} altyazı")

        // ── Stream URL Canlılık Doğrulaması (HTTP HEAD) ───────────────────────────────────────
        // Extractor URL bulsun diye ÖLÜ KANAL değil, gerçekten oynatılabilir mi kontrol et.
        // Sadece doğrudan medya URL'leri kontrol edilir (.m3u8, .mp4, .mpd).
        // Embed/iframe URL'leri (zaten çözümlenmiş olmak zorunda) atlanır.
        // Bu adım kullanıcıya "video bulunamadı" yerine gerçekten çalışan kaynakları sunar.
        val verifiedStreams = mutableListOf<StreamSource>()
        val deadStreams = mutableListOf<StreamSource>()

        for (stream in streams) {
            // ÖLÜ KANAL etiketli kaynakları doğrulama yapmadan düşür
            if (stream.name.contains("ÖLÜ KANAL", ignoreCase = true) ||
                stream.title.contains("ÖLÜ KANAL", ignoreCase = true)) {
                Log.w(TAG, "[${api.name}] ÖLÜ KANAL stream düşürülüyor: ${stream.url.orEmpty().take(80)}")
                deadStreams.add(stream)
                continue
            }

            val url = stream.url.orEmpty()
            if (url.isBlank()) {
                // URL yoksa (infoHash tabanlı?) dokunma
                verifiedStreams.add(stream)
                continue
            }

            val isDirectMedia = url.contains(".m3u8", ignoreCase = true) ||
                                url.contains(".mp4", ignoreCase = true)  ||
                                url.contains(".mpd", ignoreCase = true)  ||
                                url.contains(".mkv", ignoreCase = true)

            if (!isDirectMedia) {
                // Embed URL'leri doğrulamadan direkt ekle (extractor zaten işledi)
                verifiedStreams.add(stream)
                continue
            }

            // HTTP HEAD ile doğrula — 6 saniye timeout (HLS CDN'ler genelde hızlı yanıt verir)
            val isAlive = withTimeoutOrNull(6_000L) {
                try {
                    val referer = stream.requestHeaders?.get("Referer")
                    val requestBuilder = okhttp3.Request.Builder()
                        .url(url)
                        .method("HEAD", null)
                        .addHeader("User-Agent", com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT)
                    if (!referer.isNullOrBlank()) {
                        requestBuilder.addHeader("Referer", referer)
                    }
                    val headRequest = requestBuilder.build()
                    val response = com.kitsugi.animelist.core.network.KitsugiHttpClient.client
                        .newCall(headRequest).execute()
                    val code = response.code
                    response.close()
                    val alive = code in 200..299 || code == 301 || code == 302 || code == 403
                    Log.d(TAG, "[${api.name}] HEAD ${if (alive) "✅" else "❌"} HTTP $code: ${url.take(80)}")
                    alive
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    throw cancel
                } catch (e: Exception) {
                    Log.w(TAG, "[${api.name}] HEAD kontrol hatası (${e.javaClass.simpleName}): ${url.take(60)}")
                    // Ağ hatası = URL'in dead olduğu anlamına gelmez (geçici), güvenli tarafta kal
                    true
                }
            } ?: run {
                Log.w(TAG, "[${api.name}] HEAD timeout (6s): ${url.take(60)} — muhtemelen yavaş CDN, ekleniyor")
                true  // Timeout = belirsiz, oynatmayı dene
            }

            if (isAlive) {
                verifiedStreams.add(stream)
            } else {
                Log.w(TAG, "[${api.name}] ❌ Dead stream tespit edildi, düşürülüyor: ${url.take(80)}")
                deadStreams.add(stream)
            }
        }

        if (deadStreams.isNotEmpty()) {
            Log.w(TAG, "[${api.name}] ${deadStreams.size} dead stream düşürüldü, ${verifiedStreams.size} aktif stream kaldı.")
        }

        // Eğer tüm stream'ler dead çıktıysa (false positive önlemi), orijinalleri döndür
        val finalStreams = if (verifiedStreams.isEmpty() && streams.isNotEmpty()) {
            Log.w(TAG, "[${api.name}] Tüm streamler dead çıktı — false positive olabilir, orijinaller döndürülüyor.")
            streams
        } else {
            // Kalite değerine göre sırala (yüksek kalite önce), null qualityValue 0 kabul et
            verifiedStreams.sortedByDescending { it.qualityValue ?: 0 }
        }

        Log.d(TAG, "[${api.name}] ✅ Sonuç: ${finalStreams.size} doğrulanmış stream, ${subtitleList.size} altyazı")
        return finalStreams.map { stream ->
            stream.copy(subtitles = subtitleList.toList())
        }
    }

    // Delegated to CsLanguageDetector — see CsLanguageDetector.kt
    private fun detectLanguageCode(lang: String): String =
        CsLanguageDetector.detectLanguageCode(lang)
    private fun buildTitleVariants(main: String, alts: List<String>, season: Int): List<String> =
        CsTitleMatcher.buildTitleVariants(main, alts, season)

    internal suspend fun safeSearch(api: MainAPI, query: String): List<SearchResponse> {
        // Session bloklist kontrolü
        if (CsPluginStatusTracker.isBlocked(api.name)) {
            Log.w(TAG, "[${api.name}] safeSearch: Engellendi — atlanıyor.")
            return emptyList()
        }
        // NOT: KNOWN_BROKEN_PLUGINS kontrolü kasıtlı olarak burada YOK.
        // Bu eklentiler plugin arama sayfasında çalışabilmeli.
        // Engel sadece stream çekme aşamasında (getStreamsForUrl) uygulanır.
        val normalizeUrl = { u: String -> u.replace("https://", "").replace("http://", "").replace("www.", "").trimEnd('/') }
        val currentDomain = normalizeUrl(api.mainUrl)
        if (KNOWN_BROKEN_DOMAINS.any { currentDomain.contains(it) }) {
            Log.w(TAG, "[${api.name}] safeSearch: Domain (${api.mainUrl}) ölü domain listesinde — atlanıyor.")
            return emptyList()
        }
        applyDomainFix(api)
        return searchSemaphore.withPermit {
            withTimeoutOrNull(15_000L) {
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
                                          providerName.contains("Elysium", ignoreCase = true) ||
                                          providerName.contains("AnimPow", ignoreCase = true) ||
                                          providerName.contains("AsyaAnimeleri", ignoreCase = true) ||
                                          providerName.contains("AsyaWatch", ignoreCase = true) ||
                                          providerName.contains("CizgiMax", ignoreCase = true)

                suspend fun doSearchAttempt(searchQuery: String): List<SearchResponse>? {
                    var results: List<SearchResponse>? = null

                    // 1. Try paginated search if overridden and not marked unsupported
                    val supportsPaginated = isMethodOverridden(api, "search", String::class.java, Int::class.javaPrimitiveType ?: Int::class.java, kotlin.coroutines.Continuation::class.java)
                    if (supportsPaginated && !unsupportedMethods.contains("$providerName:search_paginated")) {
                        try {
                            Log.d(TAG, "[$providerName] api.search('$searchQuery', 1) çağrılıyor...")
                            results = api.search(searchQuery, 1)?.items
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
                                throw t
                            }
                        }
                    }

                    // 2. Try standard search if overridden and not marked unsupported
                    val supportsStandard = isMethodOverridden(api, "search", String::class.java, kotlin.coroutines.Continuation::class.java)
                    if (results == null && supportsStandard && !unsupportedMethods.contains("$providerName:search")) {
                        try {
                            Log.d(TAG, "[$providerName] api.search('$searchQuery') çağrılıyor...")
                            results = api.search(searchQuery)
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
                                throw t
                            }
                        }
                    }

                    // 3. Try quickSearch if overridden and not marked unsupported
                    val supportsQuick = isMethodOverridden(api, "quickSearch", String::class.java, kotlin.coroutines.Continuation::class.java)
                    if (results == null && supportsQuick && !unsupportedMethods.contains("$providerName:quickSearch")) {
                        try {
                            Log.d(TAG, "[$providerName] api.quickSearch('$searchQuery') çağrılıyor...")
                            results = api.quickSearch(searchQuery)
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
                                throw t
                            }
                        }
                    }

                    // 4. Fallback if reflection returned false for all methods but we haven't succeeded
                    if (results == null && !supportsPaginated && !supportsStandard && !supportsQuick) {
                        Log.d(TAG, "[$providerName] Metot tespiti yapılamadı, tüm varyantlar sırayla deneniyor...")
                        try {
                            results = api.search(searchQuery, 1)?.items
                        } catch (cancel: kotlinx.coroutines.CancellationException) {
                            throw cancel
                        } catch (t: Throwable) {
                            val isNotImpl = t is kotlin.NotImplementedError || t.message?.contains("NotImplementedError", ignoreCase = true) == true
                            if (!isNotImpl) CsPluginStatusTracker.recordFailure(providerName, t)
                            try {
                                results = api.search(searchQuery)
                            } catch (cancel2: kotlinx.coroutines.CancellationException) {
                                throw cancel2
                            } catch (t2: Throwable) {
                                val isNotImpl2 = t2 is kotlin.NotImplementedError || t2.message?.contains("NotImplementedError", ignoreCase = true) == true
                                if (!isNotImpl2) CsPluginStatusTracker.recordFailure(providerName, t2)
                                try {
                                    results = api.quickSearch(searchQuery)
                                } catch (cancel3: kotlinx.coroutines.CancellationException) {
                                    throw cancel3
                                } catch (t3: Throwable) {
                                    val isNotImpl3 = t3 is kotlin.NotImplementedError || t3.message?.contains("NotImplementedError", ignoreCase = true) == true
                                    if (!isNotImpl3) CsPluginStatusTracker.recordFailure(providerName, t3)
                                    throw t3
                                }
                            }
                        }
                    }
                    return results
                }

                var results: List<SearchResponse>? = null
                if (isTurkishPathSearch) {
                    val encoded = encodePathQuery(query)
                    try {
                        results = doSearchAttempt(encoded)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        Log.w(TAG, "[$providerName] Path-encoded query '$encoded' failed. Falling back to standard query '$query'. Error: ${t.message}")
                    }

                    // If results are null/empty, retry with raw query
                    if (results.isNullOrEmpty()) {
                        try {
                            results = doSearchAttempt(query)
                        } catch (cancel: kotlinx.coroutines.CancellationException) {
                            throw cancel
                        } catch (t: Throwable) {
                            Log.w(TAG, "[$providerName] Standard query search also failed: ${t.message}")
                        }
                    }
                } else {
                    try {
                        results = doSearchAttempt(query)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (t: Throwable) {
                        Log.w(TAG, "[$providerName] Search failed: ${t.message}")
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

    internal suspend fun safeLoad(api: MainAPI, url: String): LoadResponse? {
        // Session bloklist kontrolü
        if (CsPluginStatusTracker.isBlocked(api.name)) {
            Log.w(TAG, "[${api.name}] safeLoad: Engellendi — atlanıyor.")
            return null
        }
        // NOT: KNOWN_BROKEN_PLUGINS kontrolü kasıtlı olarak burada YOK.
        // Detay sayfası (İçerik bilgisi, bölüm listesi) erişilebilmeli.
        // Engel sadece stream çekme aşamasında (getStreamsForUrl) uygulanır.
        val normalizeUrl = { u: String -> u.replace("https://", "").replace("http://", "").replace("www.", "").trimEnd('/') }
        val currentDomain = normalizeUrl(api.mainUrl)
        if (KNOWN_BROKEN_DOMAINS.any { currentDomain.contains(it) }) {
            Log.w(TAG, "[${api.name}] safeLoad: Domain (${api.mainUrl}) ölü domain listesinde — atlanıyor.")
            return null
        }
        applyDomainFix(api)
        return loadSemaphore.withPermit {
            withTimeoutOrNull(15_000L) {
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

    suspend fun searchAllAddons(context: android.content.Context, query: String): List<Pair<MainAPI, SearchResponse>> = withContext(Dispatchers.IO) {
        if (!isDomainListFetched.get()) {
            runnerScope.launch {
                fetchRemoteDomains()
            }
        }
        val db = com.kitsugi.animelist.data.local.KitsugiDatabase.getDatabase(context.applicationContext)
        val enabledPlugins = db.csPluginDao().getEnabledPlugins()
        for (plugin in enabledPlugins) {
            try {
                com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, plugin.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load extension ${plugin.name} during global search: ${e.message}")
            }
        }
        val enabledIds = enabledPlugins.map { it.id }.toSet()
        val activeApis = com.lagradost.cloudstream3.APIHolder.allProviders.filter { api ->
            val pluginId = java.io.File(api.sourcePlugin).nameWithoutExtension
            enabledIds.contains(pluginId)
        }
        if (activeApis.isEmpty()) {
            return@withContext emptyList()
        }
        val results = mutableListOf<Pair<MainAPI, SearchResponse>>()
        kotlinx.coroutines.supervisorScope {
            val jobs = activeApis.map { api ->
                async {
                    try {
                        val searchRes = safeSearch(api, query)
                        synchronized(results) {
                            searchRes.forEach { results.add(api to it) }
                        }
                    } catch (e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e(TAG, "Search failed for provider ${api.name}: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            }
            jobs.forEach {
                try {
                    it.await()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e(TAG, "Await failed: ${e.message}")
                }
            }
        }
        results
    }

    suspend fun getStreamsForUrl(
        api: MainAPI,
        url: String,
        season: Int,
        episode: Int
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        applyDomainFix(api)
        if (api.name in KNOWN_BROKEN_PLUGINS) return@withContext emptyList()
        val searchResponse = api.newAnimeSearchResponse(
            name = api.name,
            url = url,
            type = com.lagradost.cloudstream3.TvType.Anime,
            fix = false
        )
        loadAndExtractStreams(api, searchResponse, season, episode)
    }
}

