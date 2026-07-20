package com.kitsugi.animelist.core.recommendations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.kitsugi.animelist.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * B1.3 - TvProgramBuilder: WatchNextProgram nesnelerini WatchProgress'ten olusturur.
 *
 * Kitsugi'ya ozel versioned deep link URI'lari (Kitsugianimelist://v1/play/...)
 * kullanir - REF ProgramBuilder'in Intent.toUri() yaklasiminin yerine gecen
 * daha temiz, test edilebilir URI bicimi.
 *
 * Thread-safe: tum metotlar IO-guvenli, ContentResolver cagrilari caller'in
 * Dispatchers.IO coroutine'inda yapilmalidir.
 */
@Singleton
class TvProgramBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Dahili ID politikasi (B1.4): "wn_mal_<id>" veya "wn_mal_<id>_s<S>e<E>"
    fun watchNextId(progress: WatchProgress): String =
        if (progress.season != null && progress.episode != null)
            "wn_${progress.contentId}_s${progress.season}e${progress.episode}"
        else
            "wn_${progress.contentId}"

    fun buildWatchNextProgram(progress: WatchProgress): WatchNextProgram {
        val isMovie = progress.contentType.equals("movie", ignoreCase = true)
        val programType = if (isMovie)
            TvContractCompat.WatchNextPrograms.TYPE_MOVIE
        else
            TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE

        val builder = WatchNextProgram.Builder()
            .setType(programType)
            .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            .setTitle(progress.name)
            .setLastEngagementTimeUtcMillis(progress.lastWatched)
            .setInternalProviderId(watchNextId(progress))
            .setIntentUri(buildPlayUri(progress))

        builder.setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)

        // Backdrop tercih edilir (16:9 fit), yoksa poster kullan
        val horizontalArt = progress.backdrop ?: progress.poster
        horizontalArt?.let {
            val uriWithCacheBuster = Uri.parse(it).buildUpon()
                .appendQueryParameter("v", "horizontal_fix")
                .build()
            builder.setPosterArtUri(uriWithCacheBuster)
        }

        if (progress.duration > 0) {
            builder.setLastPlaybackPositionMillis(progress.position.toInt())
            builder.setDurationMillis(progress.duration.toInt())
        }

        if (!isMovie) {
            progress.season?.let { builder.setSeasonNumber(it) }
            progress.episode?.let { builder.setEpisodeNumber(it) }
            progress.episodeTitle?.let { builder.setEpisodeTitle(it) }
        }

        return builder.build()
    }

    /**
     * Mevcut WatchNext satiri varsa guncelle, yoksa ekle.
     * ContentResolver cagrilari; caller Dispatchers.IO'da olmalidir.
     */
    fun upsertWatchNextProgram(program: WatchNextProgram, internalId: String) {
        try {
            val existingId = findWatchNextByInternalId(internalId)
            if (existingId != null) {
                val uri = TvContractCompat.buildWatchNextProgramUri(existingId)
                context.contentResolver.update(uri, program.toContentValues(), null, null)
            } else {
                context.contentResolver.insert(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    program.toContentValues()
                )
            }
        } catch (_: Exception) {
        }
    }

    fun removeWatchNextProgram(internalId: String) {
        try {
            val existingId = findWatchNextByInternalId(internalId) ?: return
            val uri = TvContractCompat.buildWatchNextProgramUri(existingId)
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
    }

    fun removeWatchNextByContentId(contentId: String) {
        try {
            val cursor = context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                arrayOf(
                    TvContractCompat.WatchNextPrograms._ID,
                    TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                ),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val idIdx = it.getColumnIndex(
                        TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                    )
                    if (idIdx >= 0) {
                        val providerId = it.getString(idIdx)
                        if (watchNextIdMatchesContentId(providerId, contentId)) {
                            val pkIdx = it.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                            if (pkIdx >= 0) {
                                val uri = TvContractCompat.buildWatchNextProgramUri(it.getLong(pkIdx))
                                context.contentResolver.delete(uri, null, null)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun clearAllWatchNextPrograms() {
        var cursor: android.database.Cursor? = null
        try {
            cursor = context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                arrayOf(
                    TvContractCompat.WatchNextPrograms._ID,
                    TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                ),
                null, null, null
            )
            cursor?.let {
                while (it.moveToNext()) {
                    val idIdx = it.getColumnIndex(
                        TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                    )
                    if (idIdx >= 0) {
                        val providerId = it.getString(idIdx)
                        // Sadece Kitsugi'nun "wn_" prefix'li kayitlarini temizle
                        if (providerId?.startsWith("wn_") == true) {
                            val pkIdx = it.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                            if (pkIdx >= 0) {
                                val uri = TvContractCompat.buildWatchNextProgramUri(it.getLong(pkIdx))
                                context.contentResolver.delete(uri, null, null)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
    }

    // --- Private ---

    private fun findWatchNextByInternalId(internalId: String): Long? {
        return try {
            val cursor = context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                arrayOf(
                    TvContractCompat.WatchNextPrograms._ID,
                    TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                ),
                null, null, null
            )
            var foundId: Long? = null
            cursor?.use {
                while (it.moveToNext()) {
                    val providerIdIdx = it.getColumnIndex(
                        TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID
                    )
                    if (providerIdIdx >= 0) {
                        val currentProviderId = it.getString(providerIdIdx)
                        if (currentProviderId == internalId) {
                            val idIdx = it.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                            if (idIdx >= 0) {
                                foundId = it.getLong(idIdx)
                                break
                            }
                        }
                    }
                }
            }
            foundId
        } catch (_: Exception) {
            null
        }
    }

    /**
     * B1.1 versioned deep link URI'si olusturur.
     * Format: Kitsugianimelist://v1/play/{contentId}?season={s}&episode={e}&position={ms}
     *
     * MainActivity'deki DeepLinkHandler bunu DeepLinkParser ile parse eder ve
     * TvNavigationState.pendingDeepLinkDetail'e park eder.
     */
    private fun buildPlayUri(progress: WatchProgress): Uri {
        val builder = Uri.Builder()
            .scheme("Kitsugianimelist")
            .authority("v1")
            .appendPath("play")
            .appendPath(progress.contentId)

        progress.season?.let { builder.appendQueryParameter("season", it.toString()) }
        progress.episode?.let { builder.appendQueryParameter("episode", it.toString()) }
        if (progress.position > 0) {
            builder.appendQueryParameter("position", progress.position.toString())
        }
        return builder.build()
    }
}

/**
 * B1.4 - Stable program/channel ID politikasi:
 * "wn_<contentId>" veya "wn_<contentId>_s<S>e<E>" formatindaki dahili ID'nin
 * verilen contentId ile eslesip eslesemedigini kontrol eder.
 */
internal fun watchNextIdMatchesContentId(providerId: String?, contentId: String): Boolean {
    val baseId = "wn_$contentId"
    if (providerId == baseId) return true
    if (providerId?.startsWith("${baseId}_s") != true) return false

    val episodeSeparator = providerId.indexOf('e', startIndex = baseId.length + 2)
    return episodeSeparator > baseId.length + 2 &&
        episodeSeparator < providerId.lastIndex &&
        (baseId.length + 2 until episodeSeparator).all { providerId[it].isDigit() } &&
        (episodeSeparator + 1..providerId.lastIndex).all { providerId[it].isDigit() }
}
