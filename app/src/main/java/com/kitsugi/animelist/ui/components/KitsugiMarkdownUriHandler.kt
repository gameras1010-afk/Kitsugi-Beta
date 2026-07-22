package com.kitsugi.animelist.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler

/**
 * Custom URI handler that intercepts clicks from the markdown renderer and routes them:
 *
 *  - `kitsugi-spoiler://` → decoded spoiler text passed to [onSpoilerClicked]
 *  - `kitsugi-image://`   → decoded image URL list (pipe-separated) passed to [onImageClicked]
 *  - anything else        → falls back to [onExternalLinkClicked] (default: open browser)
 */
@Stable
class KitsugiMarkdownUriHandler(
    private val context: Context,
    private val onSpoilerClicked: (text: String) -> Unit = {},
    private val onImageClicked: (urls: List<String>, index: Int) -> Unit = { _, _ -> },
    private val onExternalLinkClicked: (url: String) -> Unit = { url ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    },
) : UriHandler {

    override fun openUri(uri: String) {
        when {
            uri.startsWith(SPOILER_SCHEME) -> {
                val encoded = uri.removePrefix(SPOILER_SCHEME)
                val decoded = runCatching {
                    java.net.URLDecoder.decode(encoded, "UTF-8")
                }.getOrElse { encoded }
                onSpoilerClicked(decoded)
            }

            uri.startsWith(IMAGE_SCHEME) -> {
                val encoded = uri.removePrefix(IMAGE_SCHEME)
                val decoded = runCatching {
                    java.net.URLDecoder.decode(encoded, "UTF-8")
                }.getOrElse { encoded }
                // Pipe-separated list: first segment is index, rest are URLs
                // Format: kitsugi-image://INDEX|URL1|URL2|...
                val parts = decoded.split("|")
                val index = parts.firstOrNull()?.toIntOrNull() ?: 0
                val urls = if (parts.size > 1) parts.drop(1) else listOf(decoded)
                onImageClicked(urls, index)
            }

            else -> onExternalLinkClicked(uri)
        }
    }

    companion object {
        const val SPOILER_SCHEME = "kitsugi-spoiler://"
        const val IMAGE_SCHEME   = "kitsugi-image://"
    }
}
