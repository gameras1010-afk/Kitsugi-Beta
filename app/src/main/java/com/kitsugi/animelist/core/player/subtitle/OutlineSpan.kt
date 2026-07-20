package com.kitsugi.animelist.core.player.subtitle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * V2-E03 – OutlineSpan
 *
 * A CharacterStyle span that draws text with a stroke/outline effect.
 * Used by KitsugiSubtitleView to improve subtitle readability over bright video frames.
 *
 * Based on CloudStream `OutlineSpan.kt`.
 */
class OutlineSpan(
    private val strokeColor: Int = Color.BLACK,
    private val strokeWidth: Float = 3f
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return paint.measureText(text, start, end).toInt()
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
        val originalColor = paint.color
        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth

        // Draw stroke/outline first
        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth * 2f
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawText(text ?: "", start, end, x, y.toFloat(), paint)

        // Draw fill on top
        paint.style = Paint.Style.FILL
        paint.color = originalColor
        paint.strokeWidth = originalStrokeWidth
        canvas.drawText(text ?: "", start, end, x, y.toFloat(), paint)

        // Restore
        paint.style = originalStyle
        paint.color = originalColor
        paint.strokeWidth = originalStrokeWidth
    }

    companion object {
        /** Solid black outline — maximum contrast */
        fun solidBlack(strokeWidth: Float = 3f) = OutlineSpan(Color.BLACK, strokeWidth)

        /** Semi-transparent dark outline for softer look */
        fun softDark(strokeWidth: Float = 2.5f) = OutlineSpan(Color.argb(180, 0, 0, 0), strokeWidth)

        /** White outline for dark themes */
        fun solidWhite(strokeWidth: Float = 2f) = OutlineSpan(Color.WHITE, strokeWidth)
    }
}
