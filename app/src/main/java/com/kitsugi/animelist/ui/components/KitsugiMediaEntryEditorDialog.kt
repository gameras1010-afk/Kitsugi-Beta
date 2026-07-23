package com.kitsugi.animelist.ui.components

import androidx.compose.runtime.Composable
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus

@Composable
fun KitsugiMediaEntryEditorDialog(
    initialEntry: MediaEntry? = null,
    source: String = initialEntry?.source ?: "manual",
    scoreFormat: String = "POINT_10",
    onDismiss: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onConfirm: (
        title: String,
        subtitle: String,
        type: MediaType,
        status: WatchStatus,
        isAdult: Boolean,
        progress: Int,
        total: Int?,
        score: Int?,
        isFavorite: Boolean,
        startDate: String?,
        endDate: String?,
        notes: String?,
        tags: String?,
        priority: Int?,
        isRepeating: Boolean,
        repeatCount: Int,
        repeatValue: Int,
        volumeProgress: Int,
        isPrivate: Boolean,
        isHiddenFromStatusLists: Boolean,
        advancedScores: List<Double>?
    ) -> Unit
) {
    KitsugiEditMediaSheet(
        initialEntry = initialEntry,
        source = source,
        scoreFormat = scoreFormat,
        onDismiss = onDismiss,
        onDeleteClick = onDeleteClick,
        onConfirm = onConfirm
    )
}