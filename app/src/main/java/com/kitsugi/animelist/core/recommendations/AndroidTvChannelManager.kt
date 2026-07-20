package com.kitsugi.animelist.core.recommendations

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.kitsugi.animelist.MainActivity
import com.kitsugi.animelist.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TvChannelSync"

/**
 * B1.5 - AndroidTvChannelManager: Android TV launcher "Continue Watching" preview
 * kanalini yonetir.
 *
 * Capability-gated: FEATURE_LEANBACK olmayan cihazlarda isSupported() false
 * doner ve tum islemler no-op'tur.
 *
 * ensureChannel(): Kanal varsa ID'yi doner, yoksa olusturur.
 *   - TvChannelPreferences'te saklanan ID once kontrol edilir.
 *   - Silinmis kanal yeniden olusturulur.
 *   - Sahipsiz kanal (packageName'e ait) yeniden kullanilir.
 *
 * reconcile(items): Verilen WatchProgress listesine gore kanal programlarini
 *   gunceller: yeni olanlar INSERT, mevcutlar UPDATE, listede olmayanlar DELETE edilir.
 *   Fire OS uyumlulugu: selection clause kullanilmaz, channelId bellekte filtrelenir.
 */
@Singleton
class AndroidTvChannelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: TvChannelPreferences
) {
    fun isSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    /**
     * Kanal ID'sini doner; gerekirse olusturur veya yeniden kullanir.
     * Non-leanback veya hata durumunda null doner.
     */
    suspend fun ensureChannel(): Long? = withContext(Dispatchers.IO) {
        if (!isSupported()) return@withContext null
        runCatching {
            // 1. Kayitli kanal ID'si var mi?
            val stored = prefs.getChannelId()
            if (stored != null) {
                val cursor = context.contentResolver.query(
                    TvContractCompat.buildChannelUri(stored),
                    arrayOf(TvContractCompat.Channels._ID),
                    null, null, null
                )
                cursor?.use { if (it.moveToFirst()) return@runCatching stored }
                Log.d(TAG, "Stored channel $stored gone; recreating")
                prefs.clearChannelId()
            }

            // 2. Sahipsiz kanal var mi? (yeniden kullan)
            val orphan = context.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                arrayOf(
                    TvContractCompat.Channels._ID,
                    TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID
                ),
                null, null, null
            )?.use { c ->
                val idIdx = c.getColumnIndex(TvContractCompat.Channels._ID)
                val providerIdx = c.getColumnIndex(TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID)
                if (idIdx < 0 || providerIdx < 0) return@use null
                while (c.moveToNext()) {
                    val providerId = c.getString(providerIdx)
                    if (providerId != null && providerId.startsWith(context.packageName)) {
                        return@use c.getLong(idIdx)
                    }
                }
                null
            }
            if (orphan != null) {
                Log.d(TAG, "Reusing orphaned channel $orphan")
                prefs.setChannelId(orphan)
                writeChannelLogo(orphan)
                return@runCatching orphan
            }

            // 3. Yeni kanal olustur
            val appLinkUri = Uri.parse(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .toUri(Intent.URI_INTENT_SCHEME)
            )
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(context.getString(R.string.continue_watching))
                .setAppLinkIntentUri(appLinkUri)
                .build()

            val inserted = context.contentResolver.insert(
                TvContractCompat.Channels.CONTENT_URI,
                channel.toContentValues()
            ) ?: return@runCatching null

            val id = ContentUris.parseId(inserted)
            prefs.setChannelId(id)
            writeChannelLogo(id)
            TvContractCompat.requestChannelBrowsable(context, id)
            Log.d(TAG, "Created channel id=$id")
            id
        }.onFailure { Log.w(TAG, "ensureChannel failed", it) }.getOrNull()
    }

    /**
     * items listesine gore kanal programlarini reconcile eder:
     * - Listede olmayan programlar silinir (tamamlanan/dismiss edilen).
     * - Mevcut programlar UPDATE ile guncellenir (row ID stabil kalir, launcher dogru repaint yapar).
     * - Yeni programlar INSERT edilir.
     *
     * Fire OS uyumu: selection clause kullanilmaz; channelId bellekte filtrelenir.
     */
    suspend fun reconcile(items: List<WatchProgress>) = withContext(Dispatchers.IO) {
        if (!isSupported()) return@withContext
        runCatching {
            val channelId = ensureChannel() ?: return@runCatching
            val existing = queryExistingPrograms(channelId)
            val desiredKeys = items.map { progressKey(it) }.toSet()

            // Listede olmayan satirlari sil
            for ((key, rowId) in existing) {
                if (key !in desiredKeys) {
                    context.contentResolver.delete(
                        TvContractCompat.buildPreviewProgramUri(rowId), null, null
                    )
                    Log.d(TAG, "Removed program key=$key")
                }
            }

            // Mevcut satirlari guncelle veya yeni satir ekle
            items.forEachIndexed { index, progress ->
                val key = progressKey(progress)
                val values = buildProgramValues(progress, channelId, index, key)
                val existingRow = existing[key]
                if (existingRow != null) {
                    context.contentResolver.update(
                        TvContractCompat.buildPreviewProgramUri(existingRow), values, null, null
                    )
                } else {
                    context.contentResolver.insert(
                        TvContractCompat.PreviewPrograms.CONTENT_URI, values
                    )
                }
                Log.d(TAG, "${if (existingRow != null) "Updated" else "Inserted"} program key=$key")
            }
        }.onFailure { Log.w(TAG, "reconcile failed", it) }
    }

    /** Kanalin tum programlarini temizler — cikis veya gecmis temizleme. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        if (!isSupported()) return@withContext
        runCatching {
            val channelId = prefs.getChannelId() ?: return@runCatching
            val rows = queryExistingPrograms(channelId)
            rows.values.forEach { rowId ->
                context.contentResolver.delete(
                    TvContractCompat.buildPreviewProgramUri(rowId), null, null
                )
            }
            Log.d(TAG, "Cleared ${rows.size} programs for channel $channelId")
        }.onFailure { Log.w(TAG, "clearAll failed", it) }
    }

    // --- Private ---

    private fun queryExistingPrograms(channelId: Long): Map<String, Long> {
        val projection = arrayOf(
            TvContractCompat.PreviewPrograms._ID,
            TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID,
            TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID
        )
        val result = mutableMapOf<String, Long>()
        context.contentResolver.query(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            projection,
            null, null, null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms._ID)
            val channelIdx = c.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID)
            val keyIdx = c.getColumnIndexOrThrow(TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID)
            while (c.moveToNext()) {
                if (c.getLong(channelIdx) != channelId) continue
                val key = c.getString(keyIdx) ?: continue
                result[key] = c.getLong(idIdx)
            }
        }
        return result
    }

    private fun buildProgramValues(
        progress: WatchProgress,
        channelId: Long,
        sortOrder: Int,
        key: String
    ): ContentValues {
        // B1.1 versioned deep link kullan (REF'in Intent.toUri() yerine)
        val intentUri = Uri.Builder()
            .scheme("Kitsugianimelist")
            .authority("v1")
            .appendPath("play")
            .appendPath(progress.contentId)
            .apply {
                progress.season?.let { appendQueryParameter("season", it.toString()) }
                progress.episode?.let { appendQueryParameter("episode", it.toString()) }
                if (progress.position > 0) appendQueryParameter("position", progress.position.toString())
            }
            .build()

        val type = if (progress.contentType.equals("movie", ignoreCase = true))
            TvContractCompat.PreviewPrograms.TYPE_MOVIE
        else
            TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE

        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setType(type)
            .setTitle(progress.name)
            .setIntentUri(intentUri)
            .setInternalProviderId(key)
            .setWeight(Int.MAX_VALUE - sortOrder)

        // Backdrop/poster tile doldurmasi; logo kucuk rozet olarak render edilir
        val (imageUri, aspectRatio) = when {
            !progress.backdrop.isNullOrBlank() ->
                progress.backdrop to TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9
            !progress.poster.isNullOrBlank() ->
                progress.poster to TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3
            else -> null to null
        }
        imageUri?.let { builder.setPosterArtUri(Uri.parse(it)).setPosterArtAspectRatio(aspectRatio!!) }
        progress.logo?.let { builder.setLogoUri(Uri.parse(it)) }

        if (progress.duration > 0) {
            builder.setDurationMillis(progress.duration.toInt())
            val positionMs = if (progress.position > 0) {
                progress.position.toInt()
            } else {
                (progress.progressPercent?.let { it / 100f * progress.duration }?.toLong() ?: 0L).toInt()
            }
            builder.setLastPlaybackPositionMillis(positionMs)
        }

        if (type == TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE) {
            progress.season?.let { builder.setSeasonNumber(it) }
            progress.episode?.let { builder.setEpisodeNumber(it) }
            progress.episodeTitle?.let { builder.setEpisodeTitle(it) }
        }

        return builder.build().toContentValues().also { cv ->
            // Launcher siralamasini drives eden engagement time — Builder API post-1.0.0
            cv.put("last_engagement_time_utc_millis", progress.lastWatched)
            // UPDATE dongulerinde stale artwork kalmasin
            if (imageUri == null) {
                cv.putNull(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI)
            }
            if (progress.logo.isNullOrBlank()) {
                cv.putNull(TvContractCompat.PreviewPrograms.COLUMN_LOGO_URI)
            }
            if (progress.duration <= 0) {
                cv.putNull("duration_millis")
                cv.putNull("last_playback_position_millis")
            }
        }
    }

    private fun writeChannelLogo(channelId: Long) {
        runCatching {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                ?: return
            context.contentResolver.openOutputStream(
                TvContractCompat.buildChannelLogoUri(channelId)
            )?.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { Log.w(TAG, "writeChannelLogo failed", it) }
    }

    /** WatchProgressPreferences.createKey() ile ayni politika. */
    private fun progressKey(progress: WatchProgress): String =
        if (progress.season != null && progress.episode != null)
            "${progress.contentId}_s${progress.season}e${progress.episode}"
        else
            progress.contentId
}
