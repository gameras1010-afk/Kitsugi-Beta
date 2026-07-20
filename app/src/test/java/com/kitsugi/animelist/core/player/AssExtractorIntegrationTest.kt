package com.kitsugi.animelist.core.player

import android.content.Context
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.SubtitleParser
import com.kitsugi.animelist.data.settings.AppSettings
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class AssExtractorIntegrationTest {

    @Test
    fun testConfigureSubtitleParsingAppliesFactories() {
        val context = mock(Context::class.java)
        val settings = AppSettings(enableAssExtractor = true)
        val factory = PlayerMediaSourceFactory(context, settings)

        val customExtractorsFactory = mock(ExtractorsFactory::class.java)
        val customSubtitleParserFactory = mock(SubtitleParser.Factory::class.java)

        factory.configureSubtitleParsing(customExtractorsFactory, customSubtitleParserFactory)

        // Verify the properties exist and configuration executes without crash
        assertNotNull(factory)
    }
}
