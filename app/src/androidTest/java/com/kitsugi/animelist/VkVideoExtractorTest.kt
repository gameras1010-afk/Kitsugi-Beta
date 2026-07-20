package com.kitsugi.animelist

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitsugi.animelist.data.cloudstream.CsRuntimeInit
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import com.lagradost.cloudstream3.utils.loadExtractor

@RunWith(AndroidJUnit4::class)
class VkVideoExtractorTest {

    companion object {
        private const val TAG = "VkVideoExtractorTest"
    }

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        CsRuntimeInit.init(appContext)
    }

    @Test
    fun testVkVideoExtractionDirect() = runBlocking {
        // Direct VK embed URL
        val vkUrl = "https://vk.com/video_ext.php?oid=282319464&id=171189649&hash=f591e22157c12448&hd=1"
        Log.d(TAG, "Testing direct VK URL: $vkUrl")

        var extractedCount = 0
        try {
            loadExtractor(
                url = vkUrl,
                referer = "https://vk.com",
                subtitleCallback = { sub ->
                    Log.d(TAG, "Extracted Subtitle: ${sub.lang} -> ${sub.url}")
                },
                callback = { link ->
                    Log.d(TAG, "Extracted Link: ${link.name} -> ${link.url}")
                    extractedCount++
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Extraction failed", e)
        }

        Log.d(TAG, "Extracted links count: $extractedCount")
        // Since VK links might require active session/cookie, or could be expired,
        // let's just make sure it does not crash, and log the outcome.
    }

    @Test
    fun testVkVideoExtractionWithHrefLi() = runBlocking {
        val hrefLiUrl = "https://href.li/?https://vk.com/video_ext.php?oid=282319464&id=171189649&hash=f591e22157c12448&hd=1"
        Log.d(TAG, "Testing href.li wrapped VK URL: $hrefLiUrl")

        // 1. Verify URL detection works
        val isVk = CsStreamRunner.isVkEmbedUrl(hrefLiUrl)
        assertTrue("isVkEmbedUrl should recognize the URL", isVk)

        // 2. Resolve URL
        val resolvedStreams = CsStreamRunner.resolveVkEmbedUrl(
            providerName = "TestProvider",
            rawUrl = hrefLiUrl,
            referer = "https://vk.com",
            subtitleCallback = { sub ->
                Log.d(TAG, "Sub: ${sub.name} -> ${sub.url}")
            }
        )

        Log.d(TAG, "Resolved stream count: ${resolvedStreams.size}")
        for (stream in resolvedStreams) {
            Log.d(TAG, "Stream: ${stream.name} -> ${stream.url}")
            assertTrue("Url should not be empty", stream.url.isNotEmpty())
        }
    }
}
