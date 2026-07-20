package com.kitsugi.animelist.data.manga

import com.kitsugi.animelist.data.manga.model.SourceErrorCategory
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus

object SourceFailureClassifier {

    fun classifyCategory(error: Throwable?): SourceErrorCategory {
        val message = buildString {
            append(error?.message.orEmpty())
            append(' ')
            append(error?.cause?.message.orEmpty())
        }.lowercase()

        return when {
            message.contains("timeout") || message.contains("timed out") -> SourceErrorCategory.Timeout
            message.contains("429") || message.contains("too many requests") -> SourceErrorCategory.RateLimited
            message.contains("cloudflare") || message.contains("captcha") -> SourceErrorCategory.Captcha
            message.contains("404") || message.contains("not found") -> SourceErrorCategory.NotFound
            message.contains("401") || message.contains("403") || message.contains("unauthorized") || message.contains("forbidden") -> SourceErrorCategory.Auth
            message.contains("unable to resolve host") ||
                message.contains("failed to connect") ||
                message.contains("connection reset") ||
                message.contains("network") ||
                message.contains("ssl") -> SourceErrorCategory.Network
            message.isBlank() -> SourceErrorCategory.Unknown
            else -> SourceErrorCategory.Unknown
        }
    }

    fun classifyStatus(error: Throwable?): SourceHealthStatus {
        return when (classifyCategory(error)) {
            SourceErrorCategory.RateLimited -> SourceHealthStatus.RateLimited
            SourceErrorCategory.Captcha -> SourceHealthStatus.CaptchaRequired
            SourceErrorCategory.NotFound -> SourceHealthStatus.Broken
            SourceErrorCategory.Timeout,
            SourceErrorCategory.Network,
            SourceErrorCategory.Auth,
            SourceErrorCategory.Unknown -> SourceHealthStatus.Degraded
            SourceErrorCategory.None -> SourceHealthStatus.Unknown
        }
    }
}
