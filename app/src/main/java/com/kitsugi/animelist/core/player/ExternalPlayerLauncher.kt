package com.kitsugi.animelist.core.player

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract

// ─────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────

/** Subtitle file to pass to an external player. */
data class SubtitleInput(
    val url: String,
    val name: String,
    val lang: String
) : java.io.Serializable

/** Input data for launching an external video player via Intent or ActivityResultContract. */
data class ExternalPlayerInput(
    val url: String,
    val title: String? = null,
    /** HTTP request headers (e.g. Authorization, Referer) from behaviorHints.proxyHeaders.request. */
    val headers: Map<String, String>? = null,
    val resumePositionMs: Long = 0L,
    val subtitles: List<SubtitleInput>? = null,
    /** Pre-resolved intro/outro skip segments JSON — read by mpvNova, ignored by others. */
    val skipSegmentsJson: String? = null,
    val preferredPackage: String? = null
)

/**
 * Result returned by an external video player after playback ends.
 * Not all players return this — MX Player, VLC, Just Player, mpv-android are known to support it.
 */
data class ExternalPlayerResult(
    val positionMs: Long,
    val durationMs: Long?,
    val endedByUser: Boolean = true
)

// ─────────────────────────────────────────────────────────────
// Launcher
// ─────────────────────────────────────────────────────────────

/**
 * Builds and fires an external-player intent.
 *
 * Ported from KitsugiTV-dev's ExternalPlayerLauncher to support:
 *  - HTTP request headers (MX Player, mpv-android, Nova)
 *  - Resume position (MX Player, VLC, Just Player, Vimu, mpv-android)
 *  - Subtitles via ClipData + per-player extras (MX Player, VLC, Just Player, Vimu)
 *  - Skip-segments JSON (mpvNova)
 *
 * Use this for fire-and-forget launches (no result tracking).
 * Use [ExternalPlayerResultContract] when you need position/duration back.
 */
object ExternalPlayerLauncher {

    fun launch(
        context: Context,
        url: String,
        title: String? = null,
        headers: Map<String, String>? = null,
        resumePositionMs: Long = 0L,
        subtitles: List<SubtitleInput>? = null,
        skipSegmentsJson: String? = null,
        preferredPackage: String? = null
    ): Boolean {
        return try {
            val intent = buildIntent(
                context          = context,
                url              = url,
                title            = title,
                headers          = headers,
                resumePositionMs = resumePositionMs,
                subtitles        = subtitles,
                skipSegmentsJson = skipSegmentsJson,
                forResult        = false,
                preferredPackage = preferredPackage
            )
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Harici oynatıcı başlatılamadı veya bulunamadı.", Toast.LENGTH_LONG).show()
            false
        }
    }

    /** Build an [ExternalPlayerInput] for use with [ExternalPlayerResultContract]. */
    fun createInput(
        url: String,
        title: String? = null,
        headers: Map<String, String>? = null,
        resumePositionMs: Long = 0L,
        subtitles: List<SubtitleInput>? = null,
        skipSegmentsJson: String? = null,
        preferredPackage: String? = null
    ): ExternalPlayerInput = ExternalPlayerInput(
        url              = url,
        title            = title,
        headers          = headers,
        resumePositionMs = resumePositionMs,
        subtitles        = subtitles,
        skipSegmentsJson = skipSegmentsJson,
        preferredPackage = preferredPackage
    )

    /** Package-internal helper used by both [launch] and [ExternalPlayerResultContract]. */
    internal fun buildIntent(
        context: Context,
        url: String,
        title: String?,
        headers: Map<String, String>?,
        resumePositionMs: Long,
        subtitles: List<SubtitleInput>?,
        skipSegmentsJson: String?,
        forResult: Boolean,
        preferredPackage: String?
    ): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), "video/*")

        var targetPackage = preferredPackage
        if (targetPackage.isNullOrBlank() && ZidooPlayerMonitor.isZidooDevice()) {
            targetPackage = ZidooPlayerMonitor.ZIDOO_PACKAGE
        }
        if (!targetPackage.isNullOrBlank()) {
            setPackage(targetPackage)
        }

        // Skip segments — read by mpvNova, harmless for other players
        skipSegmentsJson?.let { putExtra("skip_segments", it) }

        // Title extras (MX Player, Just Player, Vimu)
        title?.let {
            putExtra("title", it)
            putExtra(Intent.EXTRA_TITLE, it)
            putExtra("forcename", it)
        }

        // HTTP request headers — passed as "Header-Name: value" string array
        headers?.let { hdrs ->
            if (hdrs.isNotEmpty()) {
                val headerArray = hdrs.entries.map { "${it.key}: ${it.value}" }.toTypedArray()
                putExtra("headers", headerArray)
            }
        }

        // Resume position
        if (resumePositionMs > 0L) {
            putExtra("position", resumePositionMs.toInt())      // MX Player / Just Player / mpv (Int ms)
            putExtra("extra_position", resumePositionMs)         // VLC (Long ms)
            putExtra("startfrom", resumePositionMs.toInt())      // Vimu (Int ms)
            putExtra("forceresume", true)                        // Vimu: enable resume for network streams
            putExtra("from_start", false)                        // VLC: don't force start from beginning
        }

        // Ask player to return position/duration — required by MX Player, harmless for others
        putExtra("return_result", true)

        // Subtitles
        if (!subtitles.isNullOrEmpty()) {
            val subtitleUris = subtitles.map { sub ->
                if (sub.url.startsWith("http://") || sub.url.startsWith("https://") || sub.url.startsWith("content://")) {
                    Uri.parse(sub.url)
                } else {
                    try {
                        val file = java.io.File(sub.url)
                        if (file.exists()) {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "com.kitsugi.animelist.fileprovider",
                                file
                            )
                        } else {
                            Uri.parse(sub.url)
                        }
                    } catch (e: Exception) {
                        Uri.parse(sub.url)
                    }
                }
            }.toTypedArray()

            val subtitleNames     = subtitles.map { it.name }.toTypedArray()
            val subtitleFilenames = subtitles.map { "${it.lang}_${it.name}.srt" }.toTypedArray()

            // Grant read permission for content:// URIs via ClipData
            // (FLAG_GRANT_READ_URI_PERMISSION only covers intent.data, not extras)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val clipData = ClipData(
                "subtitles",
                arrayOf("application/x-subrip", "text/vtt"),
                ClipData.Item(subtitleUris.first())
            )
            subtitleUris.drop(1).forEach { uri -> clipData.addItem(ClipData.Item(uri)) }
            setClipData(clipData)

            // MX Player / mpv-android / Nova
            putExtra("subs", subtitleUris)
            putExtra("subs.name", subtitleNames)
            putExtra("subs.filename", subtitleFilenames)
            putExtra("subs.enable", arrayOf(subtitleUris.first()))

            // Just Player
            putExtra("subtitle_uri", subtitleUris)
            putExtra("subtitle_name", subtitleNames)

            // VLC (single subtitle — use first)
            putExtra("subtitles_location", subtitleUris.first())

            // Vimu Player
            putExtra("forcedsrt", subtitleUris.first().toString())
        }

        // Do NOT add FLAG_ACTIVITY_NEW_TASK when forResult=true — it prevents receiving the result
        if (!forResult) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

// ─────────────────────────────────────────────────────────────
// ActivityResultContract
// ─────────────────────────────────────────────────────────────

/**
 * ActivityResultContract that launches an external video player and parses the playback result.
 *
 * Observed player result contracts (verified on-device):
 * - MX Player / mpvNova : position + duration (Int ms) + end_by
 * - Just Player          : on completion returns ONLY end_by="playback_completion"
 * - mpv-android (is.xyz.mpv): position/duration (Int) on back-press; at EOF — RESULT_OK, NO extras
 * - VLC                  : extra_position / extra_duration (Long ms), no end_by
 * - Vimu                 : position only
 * - NovaPlayer           : RESULT_CANCELED + null intent — not supported
 */
class ExternalPlayerResultContract : ActivityResultContract<ExternalPlayerInput, ExternalPlayerResult?>() {

    override fun createIntent(context: Context, input: ExternalPlayerInput): Intent =
        ExternalPlayerLauncher.buildIntent(
            context          = context,
            url              = input.url,
            title            = input.title,
            headers          = input.headers,
            resumePositionMs = input.resumePositionMs,
            subtitles        = input.subtitles,
            skipSegmentsJson = input.skipSegmentsJson,
            forResult        = true,   // no FLAG_ACTIVITY_NEW_TASK — needed to receive result
            preferredPackage = input.preferredPackage
        )

    override fun parseResult(resultCode: Int, intent: Intent?): ExternalPlayerResult? {
        android.util.Log.d(
            "ExtPlayerContract",
            "parseResult: resultCode=$resultCode extras=${intent?.extras?.keySet()?.toList()}"
        )
        val data = intent ?: return null

        val position = parsePosition(data)
        val duration = parseDuration(data)
        val endBy    = data.getStringExtra("end_by")
        val completedByEndReason = endBy == "playback_completion"

        // Vanilla mpv-android at EOF: RESULT_OK + no extras at all
        val mpvFinishedWithNoData =
            resultCode == android.app.Activity.RESULT_OK &&
            data.action == "is.xyz.mpv.MPVActivity.result" &&
            position == null && duration == null && endBy == null

        // Drop result if there is genuinely nothing to act on
        if (position == null && !completedByEndReason && !mpvFinishedWithNoData) {
            android.util.Log.d("ExtPlayerContract", "parseResult: no usable data — dropping")
            return null
        }

        // On bare completion signal with no position, use duration so the 90%-check passes
        val effectivePosition = position ?: duration ?: 0L
        val endedByUser = !mpvFinishedWithNoData && endBy != "playback_completion"

        android.util.Log.d(
            "ExtPlayerContract",
            "Parsed → position=${effectivePosition}ms duration=${duration}ms endBy=$endBy endedByUser=$endedByUser"
        )

        return ExternalPlayerResult(
            positionMs   = effectivePosition,
            durationMs   = duration,
            endedByUser  = endedByUser
        )
    }

    // Robust against key+type variants across players
    private fun parsePosition(data: Intent): Long? =
        firstPositiveExtra(data, "extra_position", "position")

    private fun parseDuration(data: Intent): Long? =
        firstPositiveExtra(data, "extra_duration", "duration")

    private fun firstPositiveExtra(data: Intent, vararg keys: String): Long? {
        for (key in keys) {
            val asLong = data.getLongExtra(key, -1L)
            if (asLong > 0) return asLong
            val asInt = data.getIntExtra(key, -1)
            if (asInt > 0) return asInt.toLong()
        }
        return null
    }
}
