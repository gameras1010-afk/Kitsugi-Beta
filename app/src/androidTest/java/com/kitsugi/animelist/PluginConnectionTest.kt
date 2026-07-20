package com.kitsugi.animelist

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.cloudstream.CsRuntimeInit
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class PluginConnectionTest {

    companion object {
        private const val TAG = "PluginConnectionTest"
    }

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        CsRuntimeInit.init(appContext)
    }

    @Test
    fun testAllTurkishPlugins() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val plugins = listOf(
            "AnimeciX",
            "Animeler",
            "Animely",
            "AnimPow",
            "Anizium",
            "AsyaAnimeleri",
            "TrAnimeIzle",
            "TurkAnime"
        )

        Log.d(TAG, "Starting Turkish Plugins test...")
        for (pluginId in plugins) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Testing plugin: $pluginId")
            
            val apis = try {
                CsPluginLoader.loadExtension(appContext, pluginId)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load extension: $pluginId", e)
                emptyList()
            }

            if (apis.isEmpty()) {
                Log.e(TAG, "Extension $pluginId loaded 0 APIs.")
                continue
            }

            for (api in apis) {
                Log.d(TAG, "  API Provider: ${api.name} (url=${api.mainUrl})")
                
                try {
                    // Try to search Solo Leveling (very popular, should exist on all Turkish anime sites)
                    Log.d(TAG, "  Searching for 'Solo Leveling'...")
                    val searchResult = try {
                        api.search("Solo Leveling", 1)?.items
                    } catch (t: Throwable) {
                        Log.d(TAG, "  api.search('Solo Leveling', 1) failed: ${t.message}. Trying api.search('Solo Leveling')...")
                        api.search("Solo Leveling")
                    }
                    Log.d(TAG, "  Search result count: ${searchResult?.size ?: 0}")
                    if (searchResult != null) {
                        for (res in searchResult.take(3)) {
                            Log.d(TAG, "    - Found: ${res.name} -> ${res.url}")
                        }
                    }

                    // Test the stream runner pipeline
                    Log.d(TAG, "  Running getStreams pipeline...")
                    val streams = CsStreamRunner.getStreams(
                        api = api,
                        title = "Solo Leveling",
                        alternativeTitles = listOf("Ore dake Level Up na Ken"),
                        year = 2024,
                        season = 1,
                        episode = 1
                    )
                    Log.d(TAG, "  Streams found: ${streams.size}")
                    for (stream in streams) {
                        Log.d(TAG, "    - Stream: ${stream.name} -> ${stream.url}")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "  Error running API ${api.name}:", e)
                }
            }
        }
        Log.d(TAG, "========================================")
        Log.d(TAG, "Turkish Plugins test finished.")
    }
}
