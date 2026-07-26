package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment
import kotlin.math.abs

@Composable
fun ChaptersSheet(
    chapters: List<IndexedSegment>,
    currentChapter: IndexedSegment,
    onClick: (IndexedSegment) -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks = chapters,
        header = {
            TrackSheetTitle(
                title = "Bölümler",
                modifier = modifier.padding(top = 4.dp),
            )
        },
        track = { chapter ->
            ChapterTrack(
                chapter = chapter,
                index = chapters.indexOf(chapter),
                selected = currentChapter == chapter,
                onClick = { onClick(chapter) },
            )
        },
        onDismissRequest = onDismissRequest,
        dismissEvent = dismissSheet,
        modifier = modifier,
    )
}

@Composable
fun ChapterTrack(
    chapter: IndexedSegment,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${index + 1}. ${chapter.name}",
            fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            prettyTime(chapter.start.toInt()),
            fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
        )
    }
}

private fun prettyTime(seconds: Int): String {
    val a = abs(seconds)
    val h = a / 3600
    val m = (a % 3600) / 60
    val s = a % 60
    return if (h > 0) "$h:%02d:%02d".format(m, s) else "%d:%02d".format(m, s)
}
