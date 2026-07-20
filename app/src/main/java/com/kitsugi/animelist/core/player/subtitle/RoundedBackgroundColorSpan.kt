package com.kitsugi.animelist.core.player.subtitle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

/**
 * V2-E03 – RoundedBackgroundColorSpan
 *
 * A CharacterStyle span that draws a rounded-rectangle background behind subtitle text.
 * Provides "box style" subtitles similar to Netflix/YouTube captions.
 *
 * Based on CloudStream `RoundedBackgroundColorSpan.kt`.
 */
class RoundedBackgroundColorSpan(
    private val backgroundColor: Int = Color.argb(180, 0, 0, 0),
    private val cornerRadius: Float = 4f,
    private val horizontalPadding: Float = 8f,
    private val verticalPadding: Float = 2f
) : ReplacementSpan() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = backgroundColor
    }

    private val rectF = RectF()

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return (paint.measureText(text, start, end) + horizontalPadding * 2).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val textWidth = paint.measureText(text, start, end)

        // Draw rounded background
        rectF.set(
            x - horizontalPadding,
            top - verticalPadding,
            x + textWidth + horizontalPadding,
            bottom + verticalPadding
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)

        // Draw text
        canvas.drawText(text ?: "", start, end, x, y.toFloat(), paint)
    }

    companion object {
        /** Netflix-style semi-transparent black box */
        fun netflixStyle() = RoundedBackgroundColorSpan(
            backgroundColor = Color.argb(200, 0, 0, 0),
            cornerRadius = 3f,
            horizontalPadding = 6f,
            verticalPadding = 2f
        )

        /** YouTube-style opaque dark box */
        fun youtubeStyle() = RoundedBackgroundColorSpan(
            backgroundColor = Color.argb(230, 8, 8, 8),
            cornerRadius = 2f,
            horizontalPadding = 4f,
            verticalPadding = 1f
        )

        /** Subtle tinted background */
        fun tinted(color: Int, alpha: Int = 160) = RoundedBackgroundColorSpan(
            backgroundColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
            cornerRadius = 6f
        )
    }
}
