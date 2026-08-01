package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.repository.StreamSource
import com.google.gson.Gson
import java.util.Locale

/** Applies a semi-transparent graphicsLayer alpha value. */
fun Modifier.graphicsLayerAlpha(alphaValue: Float): Modifier =
    this.then(Modifier.graphicsLayer { alpha = alphaValue })

/**
 * Determines whether an addon supports streaming for the given content type and video ID,
 * based on its declared stream types and ID prefix list.
 */
fun ManagedAddonEntity.supportsStreamResource(type: String, videoId: String): Boolean {
    if (streamTypes == null && subtitleTypes != null) return false
    val types = streamTypes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
    if (!types.isNullOrEmpty()) {
        val matches = types.any {
            it.equals(type, ignoreCase = true) ||
            (type.equals("series", ignoreCase = true) && it.equals("anime", ignoreCase = true)) ||
            (type.equals("movie", ignoreCase = true) && (it.equals("anime", ignoreCase = true) || it.equals("animemovie", ignoreCase = true)))
        }
        if (!matches) return false
    }
    val prefixesJson = idPrefixes
    if (!prefixesJson.isNullOrBlank()) {
        try {
            val prefixList = Gson().fromJson(prefixesJson, Array<String>::class.java).filter { it.isNotBlank() }
            if (prefixList.isNotEmpty() && prefixList.none { videoId.startsWith(it) }) return false
        } catch (_: Exception) { /* fall through */ }
    }
    return true
}

/**
 * Evaluates the Debrid cache state of a [StreamSource] by inspecting name/title tokens.
 */
fun getCacheState(stream: StreamSource): DebridCacheState {
    val nameLower = stream.name.lowercase(Locale.ROOT)
    val titleLower = stream.title.lowercase(Locale.ROOT)
    return when {
        nameLower.contains("[rd+]") || nameLower.contains("rd+") || nameLower.contains("cached") ||
        titleLower.contains("[rd+]") || titleLower.contains("rd+") ||
        nameLower.contains("[tb+]") || nameLower.contains("tb+") ||
        nameLower.contains("[pm+]") || nameLower.contains("pm+") ||
        nameLower.contains("[ad+]") || nameLower.contains("ad+") -> DebridCacheState.CACHED

        nameLower.contains("[rd~]") || nameLower.contains("download") ||
        titleLower.contains("[rd~]") || titleLower.contains("download") -> DebridCacheState.NOT_CACHED

        stream.infoHash != null && stream.url == null -> DebridCacheState.P2P

        else -> DebridCacheState.CACHED
    }
}

enum class StreamLangType(val label: String, val isDub: Boolean, val isSub: Boolean) {
    DUB("🎙️ Dublaj", true, false),
    SUB("💬 Altyazılı", false, true),
    DUAL("🌐 Dual (TR/EN)", true, true),
    UNKNOWN("🎬 Standart", false, false)
}

/**
 * Detects whether a stream source is Subbed, Dubbed, or Dual by analyzing titles, names, and provider hints.
 */
fun detectStreamLang(stream: StreamSource): StreamLangType {
    val text = "${stream.name} ${stream.title} ${stream.addonName}".lowercase(Locale.ROOT)
    val isDual = text.contains("dual") || text.contains("multi") || (text.contains("dub") && text.contains("sub"))
    if (isDual) return StreamLangType.DUAL

    val isDub = text.contains("dublaj") || text.contains("dub") || text.contains("tr-dub") || text.contains("trdub") || text.contains("türkçe ses") || text.contains("turkce ses")
    if (isDub) return StreamLangType.DUB

    val isSub = text.contains("altyazı") || text.contains("altyazi") || text.contains("sub") || text.contains("tr-sub") || text.contains("trsub") || text.contains("çeviri") || text.contains("softsub") || text.contains("hardsub")
    if (isSub) return StreamLangType.SUB

    // Turkish plugins/addons default to Subbed if no explicit tag is present
    val isTrAddon = stream.addonName.contains("Dizi", ignoreCase = true) ||
        stream.addonName.contains("Anime", ignoreCase = true) ||
        stream.isCS
    if (isTrAddon) {
        return StreamLangType.SUB
    }

    return StreamLangType.UNKNOWN
}

/**
 * Parses quality label (4K, 1080p, etc.) and file size from a stream source.
 */
fun parseStreamQuality(stream: StreamSource): Pair<String, String> {
    val rawQuality = stream.quality
    if (!rawQuality.isNullOrBlank() && rawQuality != "Bilinmeyen" && rawQuality != "Auto") {
        val sizeRegex = Regex("""(\d+(?:\.\d+)?\s*(?:gb|mb|gib|mib))""", RegexOption.IGNORE_CASE)
        val size = sizeRegex.find(stream.title)?.value?.uppercase(Locale.ROOT) ?: ""
        return rawQuality to size
    }

    val textLower = "${stream.title} ${stream.name} ${stream.url}".lowercase(Locale.ROOT)
    val quality = when {
        textLower.contains("2160") || textLower.contains("4k") || textLower.contains("uhd") -> "4K UHD"
        textLower.contains("1080") || textLower.contains("fhd") || textLower.contains("fullhd") -> "1080p"
        textLower.contains("720") || textLower.contains("hd") -> "720p"
        textLower.contains("480") || textLower.contains("sd") -> "480p"
        textLower.contains("360") -> "360p"
        else -> "1080p (HD)"
    }
    val sizeRegex = Regex("""(\d+(?:\.\d+)?\s*(?:gb|mb|gib|mib))""", RegexOption.IGNORE_CASE)
    val size = sizeRegex.find(stream.title)?.value?.uppercase(Locale.ROOT) ?: ""
    return quality to size
}

fun parseStreamTitle(title: String): Pair<String, String> {
    val titleLower = title.lowercase(Locale.ROOT)
    val quality = when {
        titleLower.contains("2160") || titleLower.contains("4k") -> "4K"
        titleLower.contains("1080") -> "1080p"
        titleLower.contains("720") -> "720p"
        titleLower.contains("480") -> "480p"
        else -> "1080p (HD)"
    }
    val sizeRegex = Regex("""(\d+(?:\.\d+)?\s*(?:gb|mb|gib|mib))""", RegexOption.IGNORE_CASE)
    val size = sizeRegex.find(title)?.value?.uppercase(Locale.ROOT) ?: ""
    return quality to size
}

/**
 * Extracts a clean and descriptive video name or domain name from the source stream url or title.
 */
fun getCleanVideoSourceLabel(stream: StreamSource): String {
    // 1. Torrent / Magnet
    val isTorrent = !stream.infoHash.isNullOrBlank() || stream.url?.startsWith("magnet:", ignoreCase = true) == true
    if (isTorrent) {
        if (!stream.title.isNullOrBlank()) {
            return stream.title.trim()
        }
        return "Torrent: ${stream.infoHash?.take(8) ?: "Magnet"}"
    }

    // 2. Direct or Embed HTTP Link
    val url = stream.url
    if (!url.isNullOrBlank()) {
        try {
            // Unescape href.li if present
            val cleanUrl = if (url.contains("href.li/?", ignoreCase = true)) {
                val idx = url.indexOf("href.li/?")
                url.substring(idx + "href.li/?".length)
            } else {
                url
            }

            val uri = java.net.URI(cleanUrl)
            val host = uri.host
            val path = uri.path

            val hostLabel = if (!host.isNullOrBlank()) {
                var h = host.lowercase(Locale.ROOT)
                if (h.startsWith("www.")) h = h.substring(4)
                val parts = h.split(".")
                if (parts.size >= 2) parts.takeLast(2).joinToString(".") else h
            } else {
                ""
            }

            val pathLabel = if (!path.isNullOrBlank()) {
                val segments = path.split("/").filter { it.isNotBlank() }
                if (segments.isNotEmpty()) {
                    val lastSegment = segments.last()
                    // If the last segment is just generic like "master.m3u8", "index.m3u8", "playlist.m3u8", "video.mp4", "play.html", etc.
                    val isGeneric = lastSegment.equals("master.m3u8", ignoreCase = true) ||
                            lastSegment.equals("index.m3u8", ignoreCase = true) ||
                            lastSegment.equals("playlist.m3u8", ignoreCase = true) ||
                            lastSegment.equals("video.mp4", ignoreCase = true) ||
                            lastSegment.equals("play", ignoreCase = true) ||
                            lastSegment.equals("play.html", ignoreCase = true) ||
                            lastSegment.matches(Regex("""\d+""")) // only digits
                    
                    if (isGeneric && segments.size >= 2) {
                        segments[segments.size - 2] + "/" + lastSegment
                    } else {
                        lastSegment
                    }
                } else {
                    ""
                }
            } else {
                ""
            }

            return when {
                !hostLabel.isNullOrBlank() && !pathLabel.isNullOrBlank() -> "$hostLabel • $pathLabel"
                !hostLabel.isNullOrBlank() -> hostLabel
                !pathLabel.isNullOrBlank() -> pathLabel
                else -> stream.title.ifBlank { "Doğrudan Bağlantı" }
            }
        } catch (e: Exception) {
            // Fallback
        }
    }

    return stream.title.ifBlank { "Doğrudan Bağlantı" }
}

