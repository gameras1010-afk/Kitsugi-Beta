package com.kitsugi.animelist.data.manga

import android.content.Context
import android.net.Uri
import android.util.Log
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.IDN
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class SourceMirrorResolver(
    private val context: Context,
    private val configStore: SourceConfigStore = SourceConfigStore(context),
) {

    private val client: OkHttpClient = uy.kohesive.injekt.Injekt.get(NetworkHelper::class.java).client
    private val tag = "SourceMirrorResolver"

    suspend fun tryResolveAndActivateMirror(source: MangaSource, cause: Throwable? = null): Boolean = kotlinx.coroutines.supervisorScope {
        if (!shouldAttemptRecovery(cause)) return@supervisorScope false

        val currentBaseUrl = configStore.getPreferredBaseUrl(source, source.baseUrl) ?: source.baseUrl
        if (currentBaseUrl.isBlank()) return@supervisorScope false

        val currentUri = runCatching { Uri.parse(currentBaseUrl) }.getOrNull() ?: return@supervisorScope false
        val currentHost = currentUri.host.orEmpty().trim()
        if (currentHost.isBlank()) return@supervisorScope false

        val candidates = linkedSetOf<String>()
        resolveRedirectHost(currentBaseUrl)?.let { redirected ->
            if (!redirected.equals(currentHost, ignoreCase = true)) candidates += redirected
        }
        deriveHostCandidates(currentHost).forEach { candidate ->
            if (!candidate.equals(currentHost, ignoreCase = true)) candidates += candidate
        }

        // Run all probes in parallel using async
        val probeJobs = candidates.map { host ->
            this@supervisorScope.async(kotlinx.coroutines.Dispatchers.IO) {
                if (isReachable(currentUri, host)) host else null
            }
        }

        val firstReachable = probeJobs.awaitAll().firstOrNull { it != null }
        if (firstReachable != null) {
            configStore.setActiveDomain(source, firstReachable)
            Log.i(tag, "Mirror activated for ${source.name}: $firstReachable")
            return@supervisorScope true
        }
        false
    }

    fun applyStoredDomainPreference(sourceId: Long, source: MangaSource) {
        val domain = configStore.getActiveDomain(source) ?: return
        val prefs = sourcePreferences(sourceId) ?: return
        val current = prefs.getString(KEY_DOMAIN, null)
        if (!current.equals(domain, ignoreCase = true)) {
            prefs.edit().putString(KEY_DOMAIN, domain).apply()
        }
    }

    private fun sourcePreferences(sourceId: Long) = runCatching {
        // Mihon/Aniyomi kaynak preference adı bu şablonla tutuluyor.
        // Kitsugi MangaExtensionLoader da default preference'ları bu isimle yazıyor.
        context.getSharedPreferences("source_$sourceId", Context.MODE_PRIVATE)
    }.getOrNull()

    private fun resolveRedirectHost(baseUrl: String): String? {
        probe(baseUrl)?.use {
            return if (it.isSuccessful || it.isRedirect) it.request.url.host else null
        }
        return null
    }

    private fun isReachable(currentUri: Uri, host: String): Boolean {
        val scheme = currentUri.scheme?.takeIf { it.isNotBlank() } ?: "https"
        val url = "$scheme://$host"
        return probe(url)?.use { response ->
            response.isSuccessful || response.isRedirect || response.code == 403 || response.code == 503
        } ?: false
    }

    private fun probe(url: String) = runCatching {
        client.newCall(
            Request.Builder()
                .url(url)
                .head()
                .build(),
        ).execute()
    }.getOrNull() ?: runCatching {
        client.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0")
                .build(),
        ).execute()
    }.getOrNull()

    private fun deriveHostCandidates(host: String): List<String> {
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        val out = linkedSetOf<String>()

        // 1. strip www. and find baseName and current TLD
        val cleanHost = asciiHost.removePrefix("www.")
        val parts = cleanHost.split(".")
        if (parts.size >= 2) {
            val baseName = parts.dropLast(1).joinToString(".")
            if (baseName.isNotBlank()) {
                val commonTlds = listOf(
                    "com", "net", "org", "xyz", "co", "online", "club", "today", "me", "fun", 
                    "space", "life", "website", "pro", "live", "art", "homes", "link", "info", "tv"
                )
                for (tld in commonTlds) {
                    out += "$baseName.$tld"
                    out += "www.$baseName.$tld"
                }
            }
        }

        if (asciiHost.startsWith("www.")) {
            out += asciiHost.removePrefix("www.")
        } else {
            out += "www.$asciiHost"
        }

        if (asciiHost.contains("manga-tr")) {
            out += asciiHost.replace("manga-tr", "mangatr")
            out += asciiHost.replace("manga-tr", "manga-tr-com")
        }
        if (asciiHost.contains("mangatr")) {
            out += asciiHost.replace("mangatr", "manga-tr")
        }

        return out.toList()
    }

    private fun shouldAttemptRecovery(cause: Throwable?): Boolean {
        if (cause == null) return true
        val message = buildString {
            append(cause.message.orEmpty())
            append(' ')
            append(cause.cause?.message.orEmpty())
        }.lowercase()

        return message.contains("404") ||
            message.contains("not found") ||
            message.contains("unable to resolve host") ||
            message.contains("failed to connect") ||
            message.contains("connection reset") ||
            message.contains("ssl") ||
            message.contains("timeout")
    }

    private companion object {
        const val KEY_DOMAIN = "domain"
    }
}
