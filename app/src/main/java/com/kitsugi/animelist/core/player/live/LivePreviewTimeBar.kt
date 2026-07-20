package com.kitsugi.animelist.core.player.live

/**
 * T1.15 – LivePreviewTimeBar (Feature flag: DEFAULT OFF)
 *
 * Canlı yayın için seekbar modeli. ExoPlayer/Media3'ün standart
 * DefaultTimeBar'ının üzerine şu bilgileri ekler:
 *
 * - **Live badge:** "CANLI" etiketi (kırmızı pill)
 * - **DVR kaseti:** DVR penceresi boyunca seyredilebilir bölge
 * - **Go-To-Live marker:** Live edge'i gösteren kırmızı çizgi
 * - **Behind-live label:** "30s geride" gibi metin
 *
 * Bu Composable, PlayerControls.kt içinde `isLive == true` iken
 * normal seekbar'ın yerine kullanılır. `isLive == false` iken
 * bu bileşen render edilmez.
 *
 * **Not:** Bu dosya Compose UI katmanı için bir veri modeli / yardımcı
 * sınıf içerir; gerçek Composable, KitsugiFullscreenPlayerScreen.kt'ye
 * entegre edilecektir.
 */

/**
 * Seekbar üzerinde gösterilecek live yayın meta verisini taşır.
 *
 * @param positionMs       Anlık oynatma pozisyonu (ms)
 * @param durationMs       DVR penceresi toplam süresi (ms). 0 → DVR yok.
 * @param behindLiveMs     Live edge'den geri kalma miktarı (ms)
 * @param showGoToLive     "Canlıya Geri Dön" butonu gösterilsin mi?
 * @param isAtLiveEdge     Kullanıcı live edge'de mi?
 */
data class LiveTimeBarState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val behindLiveMs: Long = 0L,
    val showGoToLive: Boolean = false,
    val isAtLiveEdge: Boolean = false
) {
    /** Live badge metnini döner: "CANLI ● " veya "30s GERIDE" */
    fun badgeText(): String = when {
        isAtLiveEdge -> "CANLI ●"
        behindLiveMs > 0L -> "${behindLiveMs / 1000L}s GERİDE"
        else -> "CANLI"
    }

    /** Seekbar progress 0..1 */
    fun progress(): Float = if (durationMs > 0L) positionMs.toFloat() / durationMs else 0f

    /** DVR penceresi var mı? */
    val hasDvr: Boolean get() = durationMs > 0L
}

/**
 * [LiveManager]'dan [LiveTimeBarState] oluşturmak için yardımcı factory.
 */
fun LiveManager.toTimeBarState(): LiveTimeBarState {
    val pos = positionMs.value
    val dur = durationMs.value
    val behind = behindLiveMs.value
    val goToLive = showGoToLive.value
    val atEdge = !goToLive && dur > 0L
    return LiveTimeBarState(
        positionMs = pos,
        durationMs = dur,
        behindLiveMs = behind,
        showGoToLive = goToLive,
        isAtLiveEdge = atEdge
    )
}
