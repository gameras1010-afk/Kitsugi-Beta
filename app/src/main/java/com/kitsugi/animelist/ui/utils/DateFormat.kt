package com.kitsugi.animelist.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FP-17 – Helper for standardizing date formatting across the application.
 */
object DateFormat {
    private val standardFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    fun formatDate(timestampMs: Long): String {
        return try {
            standardFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatTime(timestampMs: Long): String {
        return try {
            timeFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDateTime(timestampMs: Long): String {
        return try {
            fullDateTimeFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            ""
        }
    }
}
