package com.kitsugi.animelist.core.deeplink

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class DeepLinkTest {

    private fun mockUri(
        scheme: String?,
        host: String?,
        pathSegments: List<String> = emptyList(),
        queryParams: Map<String, String> = emptyMap()
    ): Uri {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(scheme)
        `when`(uri.host).thenReturn(host)
        `when`(uri.pathSegments).thenReturn(pathSegments)
        for ((key, value) in queryParams) {
            `when`(uri.getQueryParameter(key)).thenReturn(value)
        }
        return uri
    }

    @Test
    fun testParseInvalidOrNull() {
        assertEquals(TvDeepLink.None, DeepLinkParser.parse(null))
        
        val uriWrongScheme = mockUri("http", "v1", listOf("detail", "anilist", "123"))
        assertEquals(TvDeepLink.None, DeepLinkParser.parse(uriWrongScheme))

        val uriNoSegments = mockUri("Kitsugianimelist", "v1", emptyList())
        assertEquals(TvDeepLink.None, DeepLinkParser.parse(uriNoSegments))
    }

    @Test
    fun testParseAuthLinks() {
        val malAuth = mockUri("malapp", "auth")
        assertEquals(TvDeepLink.Auth, DeepLinkParser.parse(malAuth))

        val tvLogin = mockUri("Kitsugianimelist", "tv-login")
        assertEquals(TvDeepLink.Auth, DeepLinkParser.parse(tvLogin))
    }

    @Test
    fun testParseDetail() {
        val detailUri = mockUri("Kitsugianimelist", null, listOf("v1", "detail", "anilist", "456"))
        val result = DeepLinkParser.parse(detailUri)
        assertTrue(result is TvDeepLink.Detail)
        val detail = result as TvDeepLink.Detail
        assertEquals("anilist", detail.source)
        assertEquals("456", detail.mediaId)
    }

    @Test
    fun testParsePlay() {
        val playUri = mockUri(
            "Kitsugianimelist",
            null,
            listOf("v1", "play", "mal", "789"),
            mapOf("season" to "2", "episode" to "12")
        )
        val result = DeepLinkParser.parse(playUri)
        assertTrue(result is TvDeepLink.Play)
        val play = result as TvDeepLink.Play
        assertEquals("mal", play.source)
        assertEquals("789", play.mediaId)
        assertEquals(2, play.season)
        assertEquals(12, play.episode)
    }

    @Test
    fun testParseManga() {
        val mangaUri = mockUri(
            "Kitsugianimelist",
            null,
            listOf("v1", "manga", "mangadex", "abc-123"),
            mapOf("chapter" to "45")
        )
        val result = DeepLinkParser.parse(mangaUri)
        assertTrue(result is TvDeepLink.Manga)
        val manga = result as TvDeepLink.Manga
        assertEquals("mangadex", manga.sourceKey)
        assertEquals("abc-123", manga.mangaId)
        assertEquals("45", manga.chapterId)
    }

    @Test
    fun testDeepLinkHandlerParkingAndDraining() {
        // Clear state first
        DeepLinkHandler.clearPending()
        
        // 1. None should not park
        val invalidUri = mockUri("http", "invalid")
        val intentNone = mock(android.content.Intent::class.java)
        `when`(intentNone.data).thenReturn(invalidUri)
        val isAuthNone = DeepLinkHandler.handle(intentNone)
        assertEquals(false, isAuthNone)
        assertEquals(null, DeepLinkHandler.peekPending())

        // 2. Auth link should return true and not park
        val authUri = mockUri("Kitsugianimelist", "tv-login")
        val intentAuth = mock(android.content.Intent::class.java)
        `when`(intentAuth.data).thenReturn(authUri)
        val isAuthResult = DeepLinkHandler.handle(intentAuth)
        assertEquals(true, isAuthResult)
        assertEquals(null, DeepLinkHandler.peekPending())

        // 3. Valid v1 deep link should park
        val playUri = mockUri("Kitsugianimelist", null, listOf("v1", "play", "mal", "789"))
        val intentPlay = mock(android.content.Intent::class.java)
        `when`(intentPlay.data).thenReturn(playUri)
        val isAuthPlay = DeepLinkHandler.handle(intentPlay)
        assertEquals(false, isAuthPlay)
        
        val peeked = DeepLinkHandler.peekPending()
        assertTrue(peeked is TvDeepLink.Play)
        
        // 4. Draining should consume and clear it
        val drained = DeepLinkHandler.drainPending()
        assertEquals(peeked, drained)
        assertEquals(null, DeepLinkHandler.peekPending())
    }
}
