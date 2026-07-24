package com.kitsugi.animelist

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTmdbUpcomingMedia() = kotlinx.coroutines.runBlocking {
        val apiKey = "8265bd1679663a7ea12ac168da84d2e8"
        val language = "tr-TR"
        val client = okhttp3.OkHttpClient()
        val executeGet: suspend (String) -> String? = { url ->
            val req = okhttp3.Request.Builder().url(url).build()
            kotlin.runCatching {
                client.newCall(req).execute().use { resp ->
                    resp.body?.string()
                }
            }.getOrNull()
        }

        println("Fetching page 1...")
        val page1 = com.kitsugi.animelist.data.remote.TmdbDiscoverClient.getUpcomingMedia(
            page = 1, apiKey = apiKey, language = language, executeGet = executeGet
        )
        println("Page 1 count: ${page1.size}")
        page1.forEach { item ->
            println(" - [${item.subtitle}] ${item.title} (Adult: ${item.isAdult})")
        }

        println("Fetching page 2...")
        val page2 = com.kitsugi.animelist.data.remote.TmdbDiscoverClient.getUpcomingMedia(
            page = 2, apiKey = apiKey, language = language, executeGet = executeGet
        )
        println("Page 2 count: ${page2.size}")
        page2.forEach { item ->
            println(" - [${item.subtitle}] ${item.title} (Adult: ${item.isAdult})")
        }
    }

    @Test
    fun testNewlyAddedAnime() = kotlinx.coroutines.runBlocking {
        val client = com.kitsugi.animelist.data.remote.JikanSearchClient()
        try {
            val results = client.newlyAddedAnime(page = 1, showAdultContent = false)
            println("NewlyAddedAnime count: ${results.size}")
            results.forEach { item ->
                println(" - [${item.source}] ID: ${item.malId}, Title: ${item.title}")
            }
        } catch (e: Exception) {
            println("Exception: ${e.message}")
            e.printStackTrace()
        }
    }
}