package com.kitsugi.animelist.core.deeplink

/**
 * B1.1 - ViewModel'in tuketmesi icin parse edilmis detail deep link verisi.
 * TvNavigationState'te var pendingDeepLinkDetail olarak tutulur.
 * ViewModel bu alani okuduktan sonra null'a ceker (single-consume pattern).
 */
data class PendingDetailLink(
    val source: String,
    val mediaId: String,
    val autoPlay: Boolean = false,
    val season: Int? = null,
    val episode: Int? = null
)

/**
 * B1.1 - ViewModel'in tuketmesi icin parse edilmis manga deep link verisi.
 * TvNavigationState'te var pendingDeepLinkManga olarak tutulur.
 * ViewModel bu alani okuduktan sonra null'a ceker (single-consume pattern).
 */
data class PendingMangaLink(
    val sourceKey: String,
    val mangaId: String,
    val chapterId: String?
)
