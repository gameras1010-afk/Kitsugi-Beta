package com.kitsugi.animelist.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiApiSearchDialog
import com.kitsugi.animelist.ui.components.KitsugiConfirmDialog
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryEditorDialog
import com.kitsugi.animelist.ui.components.KitsugiMediaGridDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uygulama genelinde gösterilen overlay dialog'ları barındıran host composable.
 * AppRoot.kt'nin dialog yığınını temizlemek için çıkarılmıştır.
 */
@Composable
fun AppDialogHost(
    // Global search
    showGlobalSearch: Boolean,
    onDismissGlobalSearch: () -> Unit,
    onGlobalSearchResultSelected: (ApiSearchSelection) -> Unit,

    // Entry editing
    editingEntry: MediaEntry?,
    scoreFormat: String,
    onDismissEditing: () -> Unit,
    onDeleteEditingEntry: (MediaEntry) -> Unit,
    onConfirmEdit: (
        title: String,
        subtitle: String,
        type: com.kitsugi.animelist.model.MediaType,
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
    ) -> Unit,

    // Entry deletion confirm
    deletingEntry: MediaEntry?,
    onConfirmDelete: (MediaEntry) -> Unit,
    onDismissDelete: () -> Unit,

    // Exit confirm
    showExitConfirmDialog: Boolean,
    onConfirmExit: () -> Unit,
    onDismissExit: () -> Unit,

    // Media grid dialog
    showMediaGridDialog: Boolean,
    mediaGridDialogTitle: String,
    mediaGridDialogResults: List<JikanSearchResult>,
    isAlreadyInList: (JikanSearchResult) -> Boolean,
    onMediaGridItemClick: (JikanSearchResult) -> Unit,
    onDismissMediaGrid: () -> Unit,
    getMediaEntry: (JikanSearchResult) -> MediaEntry? = { null }
) {
    val context = LocalContext.current


    if (showGlobalSearch) {
        KitsugiApiSearchDialog(
            onDismiss = onDismissGlobalSearch,
            onResultSelected = { selection ->
                onGlobalSearchResultSelected(selection)
                onDismissGlobalSearch()
            }
        )
    }

    editingEntry?.let { entry ->
        val isAniList = androidx.compose.runtime.remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(context) != null }
        val isMal = androidx.compose.runtime.remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getMalToken(context) != null }
        val isSimkl = androidx.compose.runtime.remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getSimklToken(context) != null }

        val resolvedSource = when (entry.source.lowercase()) {
            "anilist" -> if (!isAniList && isMal) "mal" else "anilist"
            "mal" -> if (!isMal && isAniList) "anilist" else "mal"
            "simkl" -> "simkl"
            "jikan" -> if (isAniList && !isMal) "anilist" else "mal"
            else -> {
                when {
                    entry.aniListEntryId != null && isAniList -> "anilist"
                    entry.malId != null && isMal -> "mal"
                    entry.simklId != null && isSimkl -> "simkl"
                    entry.aniListEntryId != null -> "anilist"
                    entry.simklId != null -> "simkl"
                    entry.malId != null -> "mal"
                    else -> entry.source
                }
            }
        }

        KitsugiMediaEntryEditorDialog(
            initialEntry = entry,
            source = resolvedSource,
            scoreFormat = scoreFormat,
            onDismiss = onDismissEditing,
            onDeleteClick = {
                onDismissEditing()
                onDeleteEditingEntry(entry)
            },
            onConfirm = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists, advancedScores ->
                onConfirmEdit(
                    title, subtitle, type, status, isAdult, progress, total, score,
                    isFavorite, startDate, endDate, notes, tags, priority,
                    isRepeating, repeatCount, repeatValue, volumeProgress,
                    isPrivate, isHiddenFromStatusLists, advancedScores
                )
            }
        )
    }

    deletingEntry?.let { entry ->
        KitsugiConfirmDialog(
            title = "Kaydı sil?",
            message = "\"${entry.title}\" listenizden kalıcı olarak kaldırılacak.",
            confirmText = "Sil",
            isDestructive = true,
            onConfirm = { onConfirmDelete(entry) },
            onDismiss = onDismissDelete
        )
    }

    if (showExitConfirmDialog) {
        KitsugiConfirmDialog(
            title = "Uygulamadan çık?",
            message = "Kitsugi'dan çıkmak istediğinizden emin misiniz?",
            confirmText = "Çık",
            dismissText = "İptal",
            isDestructive = true,
            onConfirm = {
                onConfirmExit()
                (context as? android.app.Activity)?.finish()
            },
            onDismiss = onDismissExit
        )
    }

    if (showMediaGridDialog) {
        KitsugiMediaGridDialog(
            title = mediaGridDialogTitle,
            results = mediaGridDialogResults,
            alreadyInList = isAlreadyInList,
            onItemClick = { result ->
                onMediaGridItemClick(result)
                onDismissMediaGrid()
            },
            onDismiss = onDismissMediaGrid,
            getMediaEntry = getMediaEntry
        )
    }
}
