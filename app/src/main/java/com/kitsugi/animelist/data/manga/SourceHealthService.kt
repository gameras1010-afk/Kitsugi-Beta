package com.kitsugi.animelist.data.manga

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SourceHealthService(
    context: Context,
    private val stateStore: MangaSourceStateStore = MangaSourceStateStore(context),
    private val mirrorResolver: SourceMirrorResolver = SourceMirrorResolver(context),
) {

    private val tag = "SourceHealthService"

    suspend fun quickCheck(
        source: MangaSource,
        sampleQuery: String = "one piece",
    ): SourceHealthStatus = withContext(Dispatchers.IO) {
        val initial = evaluate(source, sampleQuery)
        if (initial == SourceHealthStatus.Broken || initial == SourceHealthStatus.Degraded) {
            val recovered = mirrorResolver.tryResolveAndActivateMirror(source)
            if (recovered) {
                val retried = evaluate(source, sampleQuery)
                stateStore.setHealthStatus(source, retried, reason = "mirror_recheck")
                return@withContext retried
            }
        }
        stateStore.setHealthStatus(source, initial)
        initial
    }

    private suspend fun evaluate(source: MangaSource, sampleQuery: String): SourceHealthStatus {
        return try {
            val search = withTimeoutOrNull(12_000L) {
                source.fetchSearchManga(page = 1, query = sampleQuery).mangas
            } ?: return SourceHealthStatus.Degraded

            if (search.isEmpty()) {
                return SourceHealthStatus.Degraded
            }

            val first = search.first()
            val details = withTimeoutOrNull(12_000L) {
                source.fetchMangaDetails(first.url)
            } ?: return SourceHealthStatus.Degraded

            val chapters = withTimeoutOrNull(15_000L) {
                source.fetchChapterList(details.url)
            } ?: return SourceHealthStatus.Degraded

            if (chapters.isEmpty()) {
                return SourceHealthStatus.Degraded
            }

            val pages = withTimeoutOrNull(15_000L) {
                source.fetchPageList(chapters.first())
            } ?: return SourceHealthStatus.Degraded

            if (pages.isEmpty()) {
                return SourceHealthStatus.Degraded
            }

            val imageUrl = withTimeoutOrNull(10_000L) {
                source.fetchImageUrl(pages.first())
            } ?: return SourceHealthStatus.Degraded

            if (imageUrl.isBlank()) {
                SourceHealthStatus.Degraded
            } else {
                SourceHealthStatus.Healthy
            }
        } catch (e: Exception) {
            Log.w(tag, "quickCheck failed for ${source.name}: ${e.message}")
            classify(e)
        }
    }

    private fun classify(error: Throwable): SourceHealthStatus {
        val message = buildString {
            append(error.message.orEmpty())
            append(' ')
            append(error.cause?.message.orEmpty())
        }.lowercase()

        return when {
            message.contains("cloudflare") || message.contains("captcha") -> SourceHealthStatus.CaptchaRequired
            message.contains("429") || message.contains("too many requests") -> SourceHealthStatus.RateLimited
            message.contains("404") || message.contains("not found") || message.contains("unable to resolve host") -> SourceHealthStatus.Broken
            else -> SourceHealthStatus.Degraded
        }
    }
}
