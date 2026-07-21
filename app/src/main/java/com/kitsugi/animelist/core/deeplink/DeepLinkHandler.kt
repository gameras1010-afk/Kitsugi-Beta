package com.kitsugi.animelist.core.deeplink

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * B1.1 - Deep-Link Handler
 *
 * Parsed TvDeepLink sonucunu pending state'e park eder ve deepLinkFlow uzerinden yayinlar.
 * TvRootScreen, AppRoot veya ViewModel bu state'i okuyup drain eder.
 *
 * Tasarim karari: Handler, UI katmanina dogrudan bagimli degil.
 * Bu sayede Activity seviyesinde kullanilabilir; circular dep riski yoktur.
 *
 * Auth callbackler (malapp://, Kitsugianimelist://tv-login) ayri yonetilir;
 * handle() true donunce ExternalAuthManager devreye girer.
 */
object DeepLinkHandler {

    // Thread-safe pending link - Activity ve Compose thread'leri arasinda guvenli.
    @Volatile
    private var pendingDeepLink: TvDeepLink? = null

    private val _deepLinkFlow = MutableSharedFlow<TvDeepLink>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deepLinkFlow: SharedFlow<TvDeepLink> = _deepLinkFlow.asSharedFlow()

    // -- Public API -----------------------------------------------------------

    /**
     * Intent'ten URI cikarir, parse eder ve pending'e park eder.
     *
     * @param intent Activity'den gelen Intent
     * @return true = Auth callback; caller ExternalAuthManager'a yonlendirir.
     *         false = Non-auth link park edildi (veya None/gecersiz).
     */
    fun handle(intent: Intent?): Boolean {
        val uri  = intent?.data ?: return false
        val link = DeepLinkParser.parse(uri)
        return when (link) {
            TvDeepLink.None -> false
            TvDeepLink.Auth -> true  // Auth yonetimi caller'a
            else            -> {
                pendingDeepLink = link
                _deepLinkFlow.tryEmit(link)
                false
            }
        }
    }

    /**
     * Dogrudan URI string'i parse edip park eder (test ve channel click icin).
     */
    fun handleUri(uriString: String) {
        val uri  = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        val link = DeepLinkParser.parse(uri)
        if (link == TvDeepLink.None || link == TvDeepLink.Auth) return
        pendingDeepLink = link
        _deepLinkFlow.tryEmit(link)
    }

    /**
     * Pending deep link'i tuketir ve dondurur.
     * Drain sonrasi pending temizlenir - tek seferlik consume garantisi.
     */
    fun drainPending(): TvDeepLink? {
        val link = pendingDeepLink
        pendingDeepLink = null
        return link
    }

    /** Pending link'i okur; drain etmez. Unit test ve diagnostics icin. */
    fun peekPending(): TvDeepLink? = pendingDeepLink

    /** Pending'i acikca temizler (logout / profile switch). */
    fun clearPending() { pendingDeepLink = null }
}

