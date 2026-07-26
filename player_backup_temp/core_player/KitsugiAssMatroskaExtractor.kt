package com.kitsugi.animelist.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mkv.EbmlProcessor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.text.SubtitleParser
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.type.AssRenderType
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * T1.10 – KitsugiAssMatroskaExtractor
 *
 * [MatroskaExtractor]'ı extends ederek MKV container'ındaki ASS/SSA
 * track'lerini libass ([AssHandler]) üzerinden render edebilecek şekilde
 * işler. Özellikler:
 *
 * - MKV Attachments bloğunu parse ederek gömülü font dosyalarını
 *   [AssHandler.addFont] ile libass'a iletir
 * - Video boyutunu [AssHandler.setVideoSize] ile bildirir (render aspect ratio)
 * - SSA/ASS cue'larını [sampleMetadata] override'ı ile [AssHandler.readTrackDialogue]'a iletir
 * - zlib-compressed MKV ASS payload'larını otomatik decompress eder
 *
 * **Feature flag:** [com.kitsugi.animelist.data.settings.AppSettings.enableAssExtractor]
 * `false` iken bu extractor hiç oluşturulmaz; standart [MatroskaExtractor] kullanılır.
 *
 * Port: `KitsugiTV-dev/PlayerLibassCompat.kt` + `KitsugiAssMatroskaExtractor.kt`
 * Orijinal paket: `com.kitsugi.tv.ui.screens.player`
 */
@OptIn(UnstableApi::class)
internal class KitsugiAssMatroskaExtractor(
    subtitleParserFactory: SubtitleParser.Factory,
    private val assHandler: AssHandler,
    flags: Int = 0
) : MatroskaExtractor(subtitleParserFactory, flags) {

    private var currentAttachmentName: String? = null
    private var currentAttachmentMime: String? = null

    // Reflection-based access to MatroskaExtractor private fields.
    // Necessary because subtitleSample is needed for raw SSA cue parsing.
    internal val subtitleSample: ParsableByteArray =
        subtitleSampleField.get(this) as ParsableByteArray

    override fun getElementType(id: Int): Int {
        return when (id) {
            ID_ATTACHMENTS   -> EbmlProcessor.ELEMENT_TYPE_MASTER
            ID_ATTACHED_FILE -> EbmlProcessor.ELEMENT_TYPE_MASTER
            ID_FILE_NAME     -> EbmlProcessor.ELEMENT_TYPE_STRING
            ID_FILE_MIME_TYPE -> EbmlProcessor.ELEMENT_TYPE_STRING
            ID_FILE_DATA     -> EbmlProcessor.ELEMENT_TYPE_BINARY
            else             -> super.getElementType(id)
        }
    }

    override fun isLevel1Element(id: Int): Boolean =
        super.isLevel1Element(id) || id == ID_ATTACHMENTS

    override fun startMasterElement(id: Int, contentPosition: Long, contentSize: Long) {
        when (id) {
            ID_EBML -> {
                // Wrap ExtractorOutput so text tracks are intercepted for libass rendering.
                if (assHandler.renderType != AssRenderType.CUES) {
                    val current = extractorOutputField.get(this) as ExtractorOutput
                    if (current !is KitsugiAssSubtitleExtractorOutput) {
                        extractorOutputField.set(
                            this,
                            KitsugiAssSubtitleExtractorOutput(current, assHandler, this)
                        )
                    }
                }
                super.startMasterElement(id, contentPosition, contentSize)
            }
            ID_ATTACHED_FILE -> clearAttachment()
            else -> super.startMasterElement(id, contentPosition, contentSize)
        }
    }

    override fun endMasterElement(id: Int) {
        when (id) {
            ID_VIDEO -> {
                // Inform libass of the source video dimensions so it can scale ASS correctly.
                val track = currentTrackGetter.invoke(this, id)
                assHandler.setVideoSize(
                    @Suppress("UNCHECKED_CAST")
                    (track as? Any)?.let {
                        runCatching { it.javaClass.getField("width").getInt(it) }.getOrDefault(0)
                    } ?: 0,
                    @Suppress("UNCHECKED_CAST")
                    (track as? Any)?.let {
                        runCatching { it.javaClass.getField("height").getInt(it) }.getOrDefault(0)
                    } ?: 0
                )
                super.endMasterElement(id)
            }
            ID_ATTACHED_FILE -> clearAttachment()
            else -> super.endMasterElement(id)
        }
    }

    override fun stringElement(id: Int, value: String) {
        when (id) {
            ID_FILE_NAME      -> currentAttachmentName = value
            ID_FILE_MIME_TYPE -> currentAttachmentMime = value
            else              -> super.stringElement(id, value)
        }
    }

    override fun binaryElement(id: Int, contentSize: Int, input: ExtractorInput) {
        when (id) {
            ID_FILE_DATA -> {
                val name = requireNotNull(currentAttachmentName)
                val mime = requireNotNull(currentAttachmentMime)
                if (mime in FONT_MIME_TYPES) {
                    val data = ByteArray(contentSize)
                    input.readFully(data, 0, contentSize)
                    assHandler.addFont(name, data)
                } else {
                    input.skipFully(contentSize)
                }
            }
            else -> super.binaryElement(id, contentSize, input)
        }
    }

    private fun clearAttachment() {
        currentAttachmentName = null
        currentAttachmentMime = null
    }

    private companion object {
        // EBML element IDs for MKV attachment support
        const val ID_EBML = 0x1A45DFA3
        const val ID_VIDEO = 0xE0
        const val ID_ATTACHMENTS = 0x1941A469
        const val ID_ATTACHED_FILE = 0x61A7
        const val ID_FILE_NAME = 0x466E
        const val ID_FILE_MIME_TYPE = 0x4660
        const val ID_FILE_DATA = 0x465C

        val FONT_MIME_TYPES = setOf(
            "font/ttf",
            "font/otf",
            "font/sfnt",
            "font/woff",
            "font/woff2",
            "application/font-sfnt",
            "application/font-woff",
            "application/x-truetype-font",
            "application/vnd.ms-opentype",
            "application/x-font-ttf"
        )

        // Reflection fields — accessed once per class, cached in companion.
        val extractorOutputField = MatroskaExtractor::class.java
            .getDeclaredField("extractorOutput")
            .apply { isAccessible = true }

        val subtitleSampleField = MatroskaExtractor::class.java
            .getDeclaredField("subtitleSample")
            .apply { isAccessible = true }

        // currentTrackGetter — try to read the current track object for video size.
        // Gracefully degrades if the field name changes between Media3 versions.
        val currentTrackGetter: (MatroskaExtractor, Int) -> Any? = run {
            val field = runCatching {
                MatroskaExtractor::class.java
                    .getDeclaredField("currentTrack")
                    .apply { isAccessible = true }
            }.getOrNull()
            if (field != null) {
                { extractor, _ -> field.get(extractor) }
            } else {
                { _, _ -> null }
            }
        }
    }
}

// ── Internal ExtractorOutput wrapper ─────────────────────────────────────────

@OptIn(UnstableApi::class)
private class KitsugiAssSubtitleExtractorOutput(
    private val delegate: ExtractorOutput,
    private val assHandler: AssHandler,
    private val extractor: KitsugiAssMatroskaExtractor
) : ExtractorOutput by delegate {

    override fun track(id: Int, type: Int): TrackOutput {
        return if (type == C.TRACK_TYPE_TEXT) {
            KitsugiAssTrackOutput(delegate.track(id, type), assHandler, extractor)
        } else {
            delegate.track(id, type)
        }
    }
}

// ── Internal TrackOutput interceptor ─────────────────────────────────────────

@OptIn(UnstableApi::class)
private class KitsugiAssTrackOutput(
    private val delegate: TrackOutput,
    private val assHandler: AssHandler,
    private val extractor: KitsugiAssMatroskaExtractor
) : TrackOutput by delegate {

    private var isAss = false
    private var trackId: String? = null

    override fun format(format: Format) {
        // Detect ASS track by MIME or codec string
        if (format.sampleMimeType == MimeTypes.TEXT_SSA || format.codecs == MimeTypes.TEXT_SSA) {
            isAss = true
            trackId = format.id
        }
        delegate.format(format)
    }

    override fun sampleMetadata(
        timeUs: Long,
        flags: Int,
        size: Int,
        offset: Int,
        cryptoData: TrackOutput.CryptoData?
    ) {
        if (isAss && timeUs != C.TIME_UNSET) {
            val sample = extractor.subtitleSample
            // SSA cue format: "ReadOrder, Layer, Style, ..., End, Text"
            // Token 1 = End timestamp, Token 2 = start of dialogue text
            val endIndex  = findTokenIndex(sample.data, 1)
            val lineIndex = findTokenIndex(sample.data, 2)
            if (endIndex > 0 && lineIndex > endIndex) {
                val rawDuration = sample.data.decodeToString(endIndex, lineIndex - 1)
                val durationUs  = parseTimecodeUs(rawDuration)
                if (durationUs != C.TIME_UNSET) {
                    val dialogue = sample.data.dialoguePayload(
                        offset = lineIndex,
                        limit  = sample.limit()
                    )
                    assHandler.readTrackDialogue(
                        trackId  = trackId,
                        start    = timeUs / 1000,
                        duration = durationUs / 1000,
                        data     = dialogue,
                        offset   = 0,
                        length   = dialogue.size
                    )
                }
            }
        }
        delegate.sampleMetadata(timeUs, flags, size, offset, cryptoData)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseTimecodeUs(timeString: String): Long {
        val matcher = SSA_TIMECODE_PATTERN.matcher(timeString.trim())
        if (!matcher.matches()) return C.TIME_UNSET
        var us = Util.castNonNull(matcher.group(1)).toLong() * 60 * 60 * C.MICROS_PER_SECOND
        us += Util.castNonNull(matcher.group(2)).toLong() * 60 * C.MICROS_PER_SECOND
        us += Util.castNonNull(matcher.group(3)).toLong() * C.MICROS_PER_SECOND
        us += Util.castNonNull(matcher.group(4)).toLong() * 10_000
        return us
    }

    private fun findTokenIndex(array: ByteArray, tokenNumber: Int): Int {
        if (tokenNumber == 0) return 0
        var tokensFound = 0
        array.forEachIndexed { index, byte ->
            if (byte == COMMA && ++tokensFound == tokenNumber) return index + 1
        }
        return 0
    }

    private fun ByteArray.dialoguePayload(offset: Int, limit: Int): ByteArray {
        if (offset >= size) return EMPTY_BYTES
        val rawEnd = if (looksLikeZlib(offset, size)) size else limit.coerceIn(offset, size)
        return maybeInflate(copyOfRange(offset, rawEnd))
    }

    private fun ByteArray.looksLikeZlib(offset: Int, limit: Int): Boolean {
        if (limit - offset < 2) return false
        val cmf = this[offset].toInt() and 0xFF
        val flg = this[offset + 1].toInt() and 0xFF
        return cmf and 0x0F == 8 && ((cmf shl 8) + flg) % 31 == 0
    }

    private fun maybeInflate(data: ByteArray): ByteArray {
        if (!data.looksLikeZlib(0, data.size)) return data
        val inflater = Inflater()
        return try {
            inflater.setInput(data)
            val out = ByteArrayOutputStream(data.size * 4)
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n > 0) out.write(buf, 0, n)
                else if (inflater.needsInput() || inflater.needsDictionary()) break
            }
            val inflated = out.toByteArray()
            if (inflater.finished() && inflated.isNotEmpty()) inflated else data
        } catch (_: DataFormatException) {
            data
        } finally {
            inflater.end()
        }
    }

    private companion object {
        val SSA_TIMECODE_PATTERN: Pattern =
            Pattern.compile("""(?:(\d+):)?(\d+):(\d+)[:.]((\d+))""")
        const val COMMA: Byte = 44
        val EMPTY_BYTES = ByteArray(0)
    }
}
