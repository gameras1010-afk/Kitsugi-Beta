package com.kitsugi.animelist.data.manga

import android.net.Uri
import java.util.Locale

fun MangaSource.stableSourceKey(): String {
    val pkg = pkgName.trim().lowercase(Locale.ROOT)
    if (pkg.isNotEmpty()) return pkg

    val host = runCatching { Uri.parse(baseUrl).host.orEmpty() }
        .getOrDefault("")
        .trim()
        .lowercase(Locale.ROOT)
    if (host.isNotEmpty()) return "${lang.lowercase(Locale.ROOT)}|$host"

    return "${lang.lowercase(Locale.ROOT)}|${name.trim().lowercase(Locale.ROOT)}"
}

fun MangaSource.searchSignature(): String {
    return buildString {
        append(name)
        append(' ')
        append(baseUrl)
        if (pkgName.isNotBlank()) {
            append(' ')
            append(pkgName)
        }
    }.lowercase(Locale.ROOT)
}
