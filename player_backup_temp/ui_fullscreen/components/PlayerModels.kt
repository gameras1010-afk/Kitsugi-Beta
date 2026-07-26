package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.runtime.Immutable

@Immutable
data class SubtitleStyleSettings(
    val size: Int = 16,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val bold: Boolean = false,
    val outlineEnabled: Boolean = true,
    val outlineColor: Int = 0xFF000000.toInt(),
    val verticalOffset: Int = 0,
    val preferredLanguage: String? = null,
    val secondaryPreferredLanguage: String? = null,
    val showOnlyPreferredLanguages: Boolean = false
)

enum class PlayerExitReason {
    StillWatchingPrompt
}

data class NextEpisodeInfo(
    val videoId: String,
    val season: Int,
    val episode: Int,
    val title: String,
    val thumbnail: String?,
    val overview: String?,
    val released: String?,
    val hasAired: Boolean,
    val unairedMessage: String?,
    val isOtherType: Boolean = false
)

sealed interface PostPlayMode {
    val nextEpisode: NextEpisodeInfo

    data class AutoPlay(
        override val nextEpisode: NextEpisodeInfo,
        val searching: Boolean = false,
        val sourceName: String? = null,
        val countdownSec: Int? = null,
    ) : PostPlayMode

    data class StillWatching(
        override val nextEpisode: NextEpisodeInfo,
        val countdownSec: Int? = null,
    ) : PostPlayMode

    fun copyWithNextEpisode(nextEpisode: NextEpisodeInfo): PostPlayMode {
        if (nextEpisode == this.nextEpisode) return this
        return when (this) {
            is AutoPlay -> copy(nextEpisode = nextEpisode)
            is StillWatching -> copy(nextEpisode = nextEpisode)
        }
    }

    fun blocksNaturalCompletion(): Boolean = when (this) {
        is StillWatching -> true
        is AutoPlay -> searching || countdownSec != null
    }
}

data class StreamInfoData(
    val addonName: String? = null,
    val addonLogo: String? = null,
    val streamName: String? = null,
    val streamDescription: String? = null,
    val filename: String? = null,
    val fileSize: Long? = null,
    val videoCodec: String? = null,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val videoFrameRate: Float? = null,
    val videoBitrate: Int? = null,
    val audioCodec: String? = null,
    val audioChannels: String? = null,
    val audioSampleRate: Int? = null,
    val audioLanguage: String? = null,
    val subtitleName: String? = null,
    val subtitleCodec: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleSource: String? = null,
    val playerEngine: String? = null
)

@Immutable
data class MetaCastMember(
    val name: String,
    val character: String? = null,
    val photo: String? = null,
    val tmdbId: Int? = null
) : java.io.Serializable

enum class PlayerPanel {
    NONE, SUBTITLES, AUDIO, SOURCES, EPISODES, STREAM_INFO, SKIP_SETTINGS, SPEED, QUALITY
}

data class TrackOption(
    val group: androidx.media3.common.Tracks.Group,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

