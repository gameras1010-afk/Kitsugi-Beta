package com.kitsugi.animelist.core.player.engine

import android.content.Context
import com.kitsugi.animelist.data.settings.AppSettings

/**
 * TASK_031 — PlayerFactory Interface + Implementasyonlar
 *
 * Oynatıcı motor instance'larını oluşturmak için factory pattern.
 * PlayerActivity veya KitsugiFullscreenPlayerScreen bu factory'yi
 * kullanarak hangi motoru kullanacağına DI aracılığıyla karar verir.
 *
 * Kaynak referans: NuvioTV PlayerFactory.kt
 * Kitsugi uyarlaması: Media3PlayerEngine ve MpvPlayerEngine saran wrapper.
 */
interface PlayerFactory {
    /**
     * Verilen [context] ve [settings] ile yeni bir [PlayerEngine] instance'ı oluşturur.
     */
    fun create(context: Context, settings: AppSettings): PlayerEngine
}

// ── Concrete Factories ─────────────────────────────────────────────────────────

/**
 * ExoPlayer (Media3) factory implementasyonu.
 *
 * Kaynak: NuvioTV ExoPlayerFactoryImpl
 */
class Media3PlayerEngineFactory : PlayerFactory {
    override fun create(context: Context, settings: AppSettings): PlayerEngine =
        Media3PlayerEngine(context, settings)
}

/**
 * MPV factory implementasyonu.
 *
 * Kaynak: NuvioTV MpvPlayerFactoryImpl
 */
class MpvPlayerEngineFactory : PlayerFactory {
    override fun create(context: Context, settings: AppSettings): PlayerEngine =
        MpvPlayerEngine(context, settings)
}

// ── Factory Provider ───────────────────────────────────────────────────────────

/**
 * TASK_031 — PlayerFactoryProvider
 *
 * Motor tipine göre doğru factory'yi döndüren singleton sağlayıcı.
 * KitsugiFullscreenPlayerScreen ve PlayerManagerHelper bu nesneyi kullanır.
 *
 * Kaynak: NuvioTV PlayerFactoryProvider
 */
object PlayerFactoryProvider {

    private val registry: Map<PlayerEngineType, PlayerFactory> = mapOf(
        PlayerEngineType.MEDIA3   to Media3PlayerEngineFactory(),
        PlayerEngineType.MPV      to MpvPlayerEngineFactory()
    )

    /**
     * [type]'a karşılık gelen [PlayerFactory]'yi döndürür.
     *
     * @throws IllegalArgumentException Kayıtlı factory bulunamazsa
     */
    fun getFactory(type: PlayerEngineType): PlayerFactory =
        registry[type]
            ?: throw IllegalArgumentException("PlayerFactory bulunamadı: $type")

    /**
     * Kolaylaştırıcı: [settings]'ten engine tipini belirleyip doğrudan engine oluşturur.
     */
    fun createEngine(
        context: Context,
        settings: AppSettings,
        videoUrl: String = "",
        isCS: Boolean = false
    ): PlayerEngine {
        val type = PlayerEngineSelector.selectEngine(settings, videoUrl, isCS)
        return getFactory(type).create(context, settings)
    }
}
