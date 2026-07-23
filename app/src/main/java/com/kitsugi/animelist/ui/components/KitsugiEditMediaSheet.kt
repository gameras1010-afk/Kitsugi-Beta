package com.kitsugi.animelist.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kitsugi.animelist.data.auth.AniListSyncManager
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import androidx.compose.material.icons.rounded.List
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

// ---------------------------------------------------------------------------
//  Renk yardımcısı — puan 0-4 kırmızı, 5-6 sarı, 7-10 yeşil
//  Sabit Color kullanılıyor (KitsugiColors @Composable gerektiriyor)
// ---------------------------------------------------------------------------
private fun scoreColor(score: Int): Color = when {
    score <= 0 -> Color(0xFF8899AA)   // muted grey
    score <= 4 -> Color(0xFFFB7185)   // AccentRed
    score <= 6 -> Color(0xFFFB923C)   // AccentOrange
    else       -> Color(0xFF34D399)   // AccentGreen
}

@Composable
fun KitsugiEditMediaSheet(
    initialEntry: MediaEntry?,
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
    val isEditing    = initialEntry != null
    val accentColor  = LocalKitsugiAccent.current
    val isAniList = when (source.lowercase()) {
        "anilist" -> true
        "mal", "jikan", "myanimelist" -> false
        "simkl", "tmdb" -> false
        else -> initialEntry?.aniListEntryId != null
    }
    val isMal = when (source.lowercase()) {
        "anilist" -> false
        "mal", "jikan", "myanimelist" -> true
        "simkl", "tmdb" -> false
        else -> !isAniList && (initialEntry?.malId != null || initialEntry?.source == "mal")
    }
    val isSimkl = when (source.lowercase()) {
        "anilist" -> false
        "mal", "jikan", "myanimelist" -> false
        "simkl", "tmdb" -> true
        else -> !isAniList && !isMal && (initialEntry?.simklId != null || initialEntry?.source == "simkl")
    }
    val isManual     = !isAniList && !isMal && !isSimkl

    // ── State ──────────────────────────────────────────────────────────────
    var title      by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.title.orEmpty()) }
    var subtitle   by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.subtitle.orEmpty()) }
    var selectedType by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.type ?: MediaType.Anime) }
    var selectedStatus by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.status ?: WatchStatus.Planned) }
    var isAdult    by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.isAdult ?: false) }
    var progressText by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.progress?.toString() ?: "0") }
    var totalText  by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.total?.toString().orEmpty()) }

    var scoreRaw   by rememberSaveable(initialEntry?.id) {
        val db = initialEntry?.score
        val ui = if (db != null) when (scoreFormat) {
            "POINT_100"      -> (db * 10)
            "POINT_5"        -> (db / 2)
            "POINT_3"        -> when { db >= 8 -> 3; db >= 5 -> 2; else -> 1 }
            else             -> db
        } else 0
        mutableIntStateOf(ui)
    }

    var isFavorite     by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.isFavorite ?: false) }
    var startDateText  by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.startDate.orEmpty()) }
    var endDateText    by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.endDate.orEmpty()) }
    var notes          by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.notes.orEmpty()) }
    var tags           by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.tags.orEmpty()) }
    var priority       by rememberSaveable(initialEntry?.id) { mutableIntStateOf(initialEntry?.priority ?: 0) }
    var isRepeating    by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.isRepeating ?: false) }
    var repeatCount    by rememberSaveable(initialEntry?.id) { mutableIntStateOf(initialEntry?.repeatCount ?: 0) }
    var repeatValue    by rememberSaveable(initialEntry?.id) { mutableIntStateOf(initialEntry?.repeatValue ?: 0) }
    var volumeProgress by rememberSaveable(initialEntry?.id) { mutableIntStateOf(initialEntry?.volumeProgress ?: 0) }
    var isPrivate      by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.isPrivate ?: false) }
    var isHiddenFromStatusLists by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.isHiddenFromStatusLists ?: false) }

    // AniList Metadata / Advanced Scoring / Custom Lists
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val aniListToken = androidx.compose.runtime.remember { ExternalAuthManager.getAniListToken(context) }
    var showCustomListsDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    var isLoadingMetadata by androidx.compose.runtime.remember { mutableStateOf(false) }
    var viewerCustomLists by androidx.compose.runtime.remember { mutableStateOf<List<String>>(emptyList()) }
    var customListsMap by androidx.compose.runtime.remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var advancedScoresEnabled by androidx.compose.runtime.remember { mutableStateOf(false) }
    var advancedScoresCategories by androidx.compose.runtime.remember { mutableStateOf<List<String>>(emptyList()) }
    var advancedScoresMap by androidx.compose.runtime.remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(initialEntry?.id, isAniList, selectedType) {
        if (isAniList && aniListToken != null) {
            isLoadingMetadata = true
            withContext(Dispatchers.IO) {
                val userId = runCatching { AniListSyncManager.fetchAniListUserId(aniListToken) }.getOrNull()
                val viewerOptions = AniListSyncManager.getViewerOptions(aniListToken)
                val isManga = selectedType == MediaType.Manga
                val userLists = AniListSyncManager.getViewerCustomLists(aniListToken, isManga).orEmpty()
                
                var entryCustomLists: Map<String, Boolean>? = null
                var entryAdvancedScores: Map<String, Double>? = null
                
                val entryId = initialEntry?.aniListEntryId
                if (entryId != null && entryId > 0 && userId != null) {
                    val metadata = AniListSyncManager.getMediaListMetadata(aniListToken, entryId, userId)
                    if (metadata != null) {
                        entryCustomLists = metadata.customLists
                        entryAdvancedScores = metadata.advancedScores
                    }
                }
                
                withContext(Dispatchers.Main) {
                    viewerCustomLists = userLists
                    if (entryCustomLists != null) {
                        customListsMap = entryCustomLists
                    } else {
                        customListsMap = userLists.associateWith { false }
                    }
                    
                    if (viewerOptions != null) {
                        val enabled = if (isManga) viewerOptions.mangaEnabled else viewerOptions.animeEnabled
                        val categories = if (isManga) viewerOptions.mangaCategories else viewerOptions.animeCategories
                        advancedScoresEnabled = enabled
                        advancedScoresCategories = categories
                        
                        val newScoresMap = categories.associateWith { 0.0 }.toMutableMap()
                        if (entryAdvancedScores != null) {
                            entryAdvancedScores.forEach { (cat, valScore) ->
                                if (newScoresMap.containsKey(cat)) {
                                    newScoresMap[cat] = valScore
                                }
                            }
                        }
                        advancedScoresMap = newScoresMap
                    }
                    isLoadingMetadata = false
                }
            }
        }
    }

    // ── Computed ───────────────────────────────────────────────────────────
    val parsedProgress = progressText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val parsedTotal    = totalText.toIntOrNull()?.takeIf { it > 0 }
    
    val activeAdvancedScores = advancedScoresMap.values.filter { it > 0.0 }
    val advancedAverage = if (activeAdvancedScores.isNotEmpty()) activeAdvancedScores.average() else 0.0
    val effectiveScoreRaw = if (advancedScoresEnabled && advancedScoresCategories.isNotEmpty()) {
        advancedAverage.roundToInt()
    } else {
        scoreRaw
    }

    val parsedScore: Int? = if (effectiveScoreRaw <= 0) null else when (scoreFormat) {
        "POINT_100"      -> (effectiveScoreRaw / 10).coerceIn(0, 10)
        "POINT_10_DECIMAL" -> effectiveScoreRaw.coerceIn(0, 10)
        "POINT_5"        -> (effectiveScoreRaw * 2).coerceIn(0, 10)
        "POINT_3"        -> when (effectiveScoreRaw) { 3 -> 10; 2 -> 5; else -> 1 }
        else             -> effectiveScoreRaw.coerceIn(0, 10)
    }
    val normalizedStart = startDateText.trim().takeIf { it.isValidDateOrBlank() }
    val normalizedEnd   = endDateText.trim().takeIf { it.isValidDateOrBlank() }
    val canSave = (isManual && title.isNotBlank() || !isManual) &&
            startDateText.trim().isValidDateOrBlank() &&
            endDateText.trim().isValidDateOrBlank()

    val scoreMax = when (scoreFormat) { "POINT_100" -> 100; "POINT_5" -> 5; "POINT_3" -> 3; else -> 10 }
    val starCount = when (scoreFormat) { "POINT_100" -> 10; "POINT_5" -> 5; "POINT_3" -> 3; else -> 10 }

    // ── Platform info ──────────────────────────────────────────────────────
    val platformName = when {
        isAniList -> "AniList"
        isMal     -> "MyAnimeList"
        isSimkl   -> "Simkl"
        else      -> "Yerel Kitaplık"
    }
    val platformColor = when {
        isAniList -> Color(0xFF02A9FF)
        isMal     -> Color(0xFF2E51A2)
        isSimkl   -> Color(0xFFE21926)
        else      -> KitsugiColors.TextMuted
    }

    fun selectDate(current: String, onSelected: (String) -> Unit) {
        val cal = java.util.Calendar.getInstance()
        val p   = current.split("-")
        val y   = p.getOrNull(0)?.toIntOrNull() ?: cal.get(java.util.Calendar.YEAR)
        val m   = p.getOrNull(1)?.toIntOrNull()?.minus(1) ?: cal.get(java.util.Calendar.MONTH)
        val d   = p.getOrNull(2)?.toIntOrNull() ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
        DatePickerDialog(context, { _, yr, mn, dy ->
            onSelected("$yr-${String.format("%02d", mn + 1)}-${String.format("%02d", dy)}")
        }, y, m, d).show()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UI
    // ══════════════════════════════════════════════════════════════════════
    KitsugiSheetOrDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("İptal", color = KitsugiColors.TextSecondary, fontWeight = FontWeight.Medium)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isManual) title.ifBlank { "Yeni Kayıt" } else initialEntry?.title ?: platformName,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    // Platform badge
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(platformColor.copy(alpha = 0.15f))
                            .border(1.dp, platformColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = platformName,
                            color = platformColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    enabled = canSave,
                    onClick = {
                        val advancedScoresList: List<Double>? = if (advancedScoresEnabled && advancedScoresCategories.isNotEmpty()) {
                            advancedScoresCategories.map { advancedScoresMap[it] ?: 0.0 }
                        } else null
                        onConfirm(
                            if (isManual) title.trim() else initialEntry?.title.orEmpty(),
                            if (isManual) subtitle.trim() else initialEntry?.subtitle.orEmpty(),
                            if (isManual) selectedType else initialEntry?.type ?: MediaType.Anime,
                            selectedStatus,
                            if (isManual) isAdult else initialEntry?.isAdult ?: false,
                            parsedProgress, parsedTotal, parsedScore, isFavorite,
                            normalizedStart, normalizedEnd,
                            notes.trim().takeIf { it.isNotBlank() },
                            tags.trim().takeIf { it.isNotBlank() },
                            priority, isRepeating, repeatCount, repeatValue, volumeProgress,
                            isPrivate, isHiddenFromStatusLists,
                            advancedScoresList
                        )
                    }
                ) {
                    Text(
                        text = if (isAniList) "Kaydet" else "Uygula",
                        color = if (canSave) accentColor else KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))

            // ── Scrollable body ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {

                // ── Manual title fields ────────────────────────────────────
                if (isManual) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DialogTextField(
                            value = title, onValueChange = { title = it },
                            label = "Başlık", placeholder = "Örn: Sousou no Frieren"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DialogTextField(
                            value = subtitle, onValueChange = { subtitle = it },
                            label = "Açıklama / Türler", placeholder = "Örn: TV, Action"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Media type toggle
                    val typeValue = when (selectedType) {
                        MediaType.Anime  -> "Anime"
                        MediaType.Manga  -> "Manga"
                        MediaType.Movie  -> "Film"
                        MediaType.TvShow -> "Dizi"
                    }
                    val typeIcon = when (selectedType) {
                        MediaType.Manga -> Icons.Rounded.Book
                        else            -> Icons.Rounded.PlayArrow
                    }
                    SheetRow(
                        icon = typeIcon, label = "Tür", value = typeValue,
                        onClick = {
                            selectedType = when (selectedType) {
                                MediaType.Anime  -> MediaType.Manga
                                MediaType.Manga  -> MediaType.Movie
                                MediaType.Movie  -> MediaType.TvShow
                                MediaType.TvShow -> MediaType.Anime
                            }
                        }
                    )
                    HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // ── Status Pills ───────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AniList için Repeating ayrı bir status, MAL için flag
                    val statusPills = if (isAniList) {
                        listOf(
                            WatchStatus.Watching  to Icons.Rounded.PlayArrow,
                            WatchStatus.Completed to Icons.Rounded.Check,
                            WatchStatus.Paused    to Icons.Rounded.Pause,
                            WatchStatus.Dropped   to Icons.Rounded.Delete,
                            WatchStatus.Planned   to Icons.Rounded.Bookmark,
                            WatchStatus.Repeating to Icons.Rounded.Repeat
                        )
                    } else {
                        listOf(
                            WatchStatus.Watching  to Icons.Rounded.PlayArrow,
                            WatchStatus.Completed to Icons.Rounded.Check,
                            WatchStatus.Paused    to Icons.Rounded.Pause,
                            WatchStatus.Dropped   to Icons.Rounded.Delete,
                            WatchStatus.Planned   to Icons.Rounded.Bookmark
                        )
                    }
                    statusPills.forEach { (status, icon) ->
                        val selected = selectedStatus == status
                        val pillBg by animateColorAsState(
                            targetValue = if (selected) accentColor else KitsugiColors.SurfaceSoft,
                            animationSpec = tween(200), label = "pillBg"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(pillBg)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) accentColor else KitsugiColors.Border,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .tvClickable(shape = RoundedCornerShape(20.dp)) { selectedStatus = status }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = status.label,
                                tint = if (selected) KitsugiColors.Background else KitsugiColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = status.label,
                                color = if (selected) KitsugiColors.Background else KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Star Rating ────────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KitsugiColors.Surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val scoreColor by animateColorAsState(
                        targetValue = scoreColor(effectiveScoreRaw),
                        animationSpec = tween(300), label = "scoreColor"
                    )
                    Text(
                        text = if (effectiveScoreRaw <= 0) "Puanlanmadı" else "$effectiveScoreRaw / $scoreMax" + (if (advancedScoresEnabled) " (Ortalama)" else ""),
                        color = scoreColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..starCount) {
                            val filled = i <= effectiveScoreRaw
                            val starScale by animateFloatAsState(
                                targetValue = if (filled) 1.15f else 1f,
                                animationSpec = tween(150), label = "starScale$i"
                            )
                            val starModifier = Modifier
                                .size(28.dp)
                                .scale(starScale)
                            
                            val clickableModifier = if (!advancedScoresEnabled) {
                                starModifier.tvClickable(shape = CircleShape) {
                                    scoreRaw = if (scoreRaw == i) 0 else i
                                }
                            } else {
                                starModifier
                            }
                            
                            Icon(
                                imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Puan $i",
                                tint = if (filled) scoreColor else KitsugiColors.TextMuted,
                                modifier = clickableModifier
                            )
                        }
                    }
                }

                // ── Advanced Scoring categories ─────────────────────────────
                if (advancedScoresEnabled && advancedScoresCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detaylı Puanlama",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    advancedScoresCategories.forEach { category ->
                        val currentVal = advancedScoresMap[category] ?: 0.0
                        
                        val (step, maxVal) = when (scoreFormat) {
                            "POINT_100" -> 1.0 to 100.0
                            "POINT_10_DECIMAL" -> 0.1 to 10.0
                            "POINT_5" -> 1.0 to 5.0
                            "POINT_3" -> 1.0 to 3.0
                            else -> 1.0 to 10.0
                        }
                        
                        val formattedValue = if (scoreFormat == "POINT_10_DECIMAL") {
                            String.format(java.util.Locale.US, "%.1f", currentVal)
                        } else {
                            currentVal.toInt().toString()
                        }
                        
                        SheetRow(
                            icon = Icons.Rounded.Star,
                            label = category,
                            value = formattedValue,
                            onDecrement = {
                                val newVal = (currentVal - step).coerceAtLeast(0.0)
                                advancedScoresMap = advancedScoresMap.toMutableMap().apply { put(category, newVal) }
                            },
                            onIncrement = {
                                val newVal = (currentVal + step).coerceAtMost(maxVal)
                                advancedScoresMap = advancedScoresMap.toMutableMap().apply { put(category, newVal) }
                            }
                        )
                    }
                }

                // ── Progress & Total ───────────────────────────────────────
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))

                val progressSuffix = if (parsedTotal != null) " / $parsedTotal" else ""
                val progressLabel  = when (selectedType) {
                    MediaType.Manga  -> "Bölümler (Chapter)"
                    MediaType.Movie  -> "İlerleme"
                    else             -> "Bölümler"
                }
                val progressIcon = if (selectedType == MediaType.Manga) Icons.Rounded.Book else Icons.Rounded.PlayArrow

                SheetRow(
                    icon = progressIcon, label = progressLabel,
                    value = progressText,
                    onValueChange = { v ->
                        if (v.isEmpty()) progressText = ""
                        else v.toIntOrNull()?.let { n ->
                            progressText = if (parsedTotal != null) n.coerceAtMost(parsedTotal).toString() else n.toString()
                        }
                    },
                    valuePlaceholder = "0", valueSuffix = progressSuffix,
                    onDecrement = { progressText = ((progressText.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString() },
                    onIncrement = {
                        val cur = progressText.toIntOrNull() ?: 0
                        val nxt = cur + 1
                        progressText = if (parsedTotal != null) nxt.coerceAtMost(parsedTotal).toString() else nxt.toString()
                    }
                )

                // Manga volume progress (MAL & manual)
                if (selectedType == MediaType.Manga && !isAniList) {
                    SheetRow(
                        icon = Icons.Rounded.Bookmark, label = "Ciltler (Volume)",
                        value = volumeProgress.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { volumeProgress = it } },
                        valuePlaceholder = "0",
                        onDecrement = { volumeProgress = (volumeProgress - 1).coerceAtLeast(0) },
                        onIncrement = { volumeProgress++ }
                    )
                }

                // Total episodes (manual only)
                if (isManual) {
                    SheetRow(
                        icon = Icons.Rounded.FormatListNumbered, label = "Toplam Sayı",
                        value = totalText,
                        onValueChange = { v ->
                            if (v.isEmpty()) totalText = ""
                            else v.toIntOrNull()?.let { totalText = it.toString() }
                        },
                        valuePlaceholder = "Bilinmiyor",
                        onDecrement = { val c = totalText.toIntOrNull() ?: 0; totalText = if (c > 1) (c - 1).toString() else "" },
                        onIncrement = { totalText = ((totalText.toIntOrNull() ?: 0) + 1).toString() }
                    )
                }

                HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))

                // ── Dates ──────────────────────────────────────────────────
                SheetRow(
                    icon = Icons.Rounded.CalendarToday, label = "Başlangıç Tarihi",
                    value = startDateText.ifBlank { "Tarih Seçilmemiş" },
                    onClick = { selectDate(startDateText) { startDateText = it } },
                    onClearClick = if (startDateText.isNotBlank()) { { startDateText = "" } } else null
                )
                SheetRow(
                    icon = Icons.Rounded.EventAvailable, label = "Bitiş Tarihi",
                    value = endDateText.ifBlank { "Tarih Seçilmemiş" },
                    onClick = { selectDate(endDateText) { endDateText = it } },
                    onClearClick = if (endDateText.isNotBlank()) { { endDateText = "" } } else null
                )

                HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))

                // ── Platform-specific fields ───────────────────────────────
                // AniList only
                if (isAniList) {
                    if (viewerCustomLists.isNotEmpty()) {
                        val activeLists = customListsMap.filter { it.value }.keys.toList()
                        val listsValue = if (activeLists.isEmpty()) "Hiçbiri" else activeLists.joinToString(", ")
                        SheetRow(
                            icon = Icons.Rounded.List,
                            label = "Özel Listeler",
                            value = listsValue,
                            onClick = { showCustomListsDialog = true }
                        )
                    }
                    SheetRow(
                        icon = Icons.Rounded.Star, label = "Favori",
                        switchChecked = isFavorite, onSwitchCheckedChange = { isFavorite = it }
                    )
                    // AniList'te Repeating bir status (pill olarak seçilir), isRepeating toggle'ı
                    // sadece MAL içindir. Ancak repeatCount AniList 'repeat' alanına yazılır.
                    if (selectedStatus == WatchStatus.Repeating) {
                        SheetRow(
                            icon = Icons.Rounded.RepeatOne,
                            label = if (selectedType == MediaType.Manga) "Toplam Tekrar Okuma" else "Toplam Tekrar İzleme",
                            value = repeatCount.toString(),
                            onDecrement = { repeatCount = (repeatCount - 1).coerceAtLeast(0) },
                            onIncrement = { repeatCount++ }
                        )
                    }
                    SheetRow(
                        icon = Icons.Rounded.Lock, label = "Gizli",
                        switchChecked = isPrivate, onSwitchCheckedChange = { isPrivate = it }
                    )
                    SheetRow(
                        icon = Icons.Rounded.VisibilityOff, label = "Durum listelerinden gizle",
                        switchChecked = isHiddenFromStatusLists,
                        onSwitchCheckedChange = { isHiddenFromStatusLists = it }
                    )
                }

                // MAL only
                if (isMal) {
                    SheetRow(
                        icon = Icons.Rounded.Star, label = "Favori",
                        switchChecked = isFavorite, onSwitchCheckedChange = { isFavorite = it }
                    )
                    val priorityText = when (priority) { 1 -> "Orta"; 2 -> "Yüksek"; else -> "Düşük" }
                    SheetRow(
                        icon = Icons.Rounded.PriorityHigh, label = "Öncelik", value = priorityText,
                        onDecrement = { priority = (priority - 1).coerceAtLeast(0) },
                        onIncrement = { priority = (priority + 1).coerceAtMost(2) }
                    )
                    SheetRow(
                        icon = Icons.Rounded.Repeat,
                        label = if (selectedType == MediaType.Manga) "Yeniden Okunuyor" else "Yeniden İzleniyor",
                        switchChecked = isRepeating, onSwitchCheckedChange = { isRepeating = it }
                    )
                    SheetRow(
                        icon = Icons.Rounded.RepeatOne,
                        label = if (selectedType == MediaType.Manga) "Toplam Tekrar Okuma" else "Toplam Tekrar İzleme",
                        value = repeatCount.toString(),
                        onDecrement = { repeatCount = (repeatCount - 1).coerceAtLeast(0) },
                        onIncrement = { repeatCount++ }
                    )
                    val repeatValueText = when (repeatValue) {
                        1 -> "Çok Düşük"; 2 -> "Düşük"; 3 -> "Orta"; 4 -> "Yüksek"; 5 -> "Çok Yüksek"; else -> "─"
                    }
                    SheetRow(
                        icon = Icons.Rounded.DateRange,
                        label = if (selectedType == MediaType.Manga) "Tekrar Okuma Değeri" else "Tekrar İzleme Değeri",
                        value = repeatValueText,
                        onDecrement = { repeatValue = (repeatValue - 1).coerceAtLeast(0) },
                        onIncrement = { repeatValue = (repeatValue + 1).coerceAtMost(5) }
                    )
                    // Tags
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Label,
                            contentDescription = "Etiketler",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        DialogTextField(
                            value = tags, onValueChange = { tags = it },
                            label = "Etiketler", placeholder = "Virgülle ayırın: Örn: sevilen, aksiyon",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Simkl only
                if (isSimkl) {
                    SheetRow(
                        icon = Icons.Rounded.Star, label = "Favori",
                        switchChecked = isFavorite, onSwitchCheckedChange = { isFavorite = it }
                    )
                    SheetRow(
                        icon = Icons.Rounded.Repeat,
                        label = if (selectedType == MediaType.Manga) "Yeniden Okunuyor" else "Yeniden İzleniyor",
                        switchChecked = isRepeating, onSwitchCheckedChange = { isRepeating = it }
                    )
                    if (isRepeating) {
                        SheetRow(
                            icon = Icons.Rounded.RepeatOne,
                            label = if (selectedType == MediaType.Manga) "Toplam Tekrar Okuma" else "Toplam Tekrar İzleme",
                            value = repeatCount.toString(),
                            onDecrement = { repeatCount = (repeatCount - 1).coerceAtLeast(0) },
                            onIncrement = { repeatCount++ }
                        )
                    }
                    // Etiketler — lokal organizasyon için
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Label,
                            contentDescription = "Etiketler",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        DialogTextField(
                            value = tags, onValueChange = { tags = it },
                            label = "Etiketler", placeholder = "Virgülle ayırın: Örn: sevilen, bilim-kurgu",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (isManual) {
                    SheetRow(
                        icon = Icons.Rounded.Star, label = "Favori",
                        switchChecked = isFavorite, onSwitchCheckedChange = { isFavorite = it }
                    )
                    SheetRow(
                        icon = Icons.Rounded.Lock, label = "+18 İçerik",
                        switchChecked = isAdult, onSwitchCheckedChange = { isAdult = it }
                    )
                }

                // ── Notes ──────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = "Notlar",
                        tint = KitsugiColors.TextSecondary,
                        modifier = Modifier.size(24.dp).padding(top = 12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(18.dp),
                        placeholder = { Text(text = "Kişisel düşüncelerinizi yazın...", color = KitsugiColors.TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = KitsugiColors.TextPrimary,
                            unfocusedTextColor = KitsugiColors.TextPrimary,
                            focusedContainerColor = KitsugiColors.SurfaceSoft,
                            unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = KitsugiColors.Border,
                            cursorColor = accentColor
                        )
                    )
                }

                // ── Delete ─────────────────────────────────────────────────
                if (isEditing && onDeleteClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 16.dp))
                    SheetRow(
                        icon = Icons.Rounded.Delete, label = "Sil",
                        onClick = onDeleteClick,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // ── Sync footer ────────────────────────────────────────────
                Spacer(modifier = Modifier.height(18.dp))
                val syncNote = when {
                    isAniList -> "Not: Değişiklikler otomatik olarak AniList hesabınızla senkronize edilecektir."
                    isMal     -> "Not: Değişiklikler otomatik olarak MyAnimeList hesabınızla senkronize edilecektir."
                    isSimkl   -> "Not: Değişiklikler otomatik olarak Simkl hesabınızla senkronize edilecektir."
                    else      -> ""
                }
                if (syncNote.isNotBlank()) {
                    Text(
                        text = syncNote,
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showCustomListsDialog && viewerCustomLists.isNotEmpty()) {
        KitsugiCustomListsSelectionDialog(
            viewerCustomLists = viewerCustomLists,
            customListsMap = customListsMap,
            onDismiss = { showCustomListsDialog = false },
            onConfirm = { updatedMap ->
                customListsMap = updatedMap
                showCustomListsDialog = false
                if (aniListToken != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val entryId = initialEntry?.aniListEntryId
                        val resolvedMediaId = if (entryId == null || entryId <= 0) {
                            initialEntry?.let { AniListSyncManager.resolveAniListMediaId(aniListToken, it) }
                        } else null
                        
                        val activeLists = updatedMap.filter { it.value }.keys.toList()
                        AniListSyncManager.updateEntryCustomLists(
                            token = aniListToken,
                            mediaId = resolvedMediaId,
                            entryId = entryId,
                            customLists = activeLists
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun KitsugiCustomListsSelectionDialog(
    viewerCustomLists: List<String>,
    customListsMap: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Boolean>) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    var tempMap by androidx.compose.runtime.remember { mutableStateOf(customListsMap) }
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current

    val dialogContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Özel Listeler",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewerCustomLists.forEach { listName ->
                    val checked = tempMap[listName] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (checked) accentColor.copy(alpha = 0.1f) else KitsugiColors.SurfaceSoft)
                            .border(
                                width = 1.dp,
                                color = if (checked) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                tempMap = tempMap.toMutableMap().apply { put(listName, !checked) }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listName,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                        )
                        if (checked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Seçili",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "İptal",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextButton(onClick = { onConfirm(tempMap) }) {
                    Text(
                        text = "Kaydet",
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (isTvDevice) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = KitsugiColors.Surface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KitsugiColors.Border),
                    modifier = Modifier
                        .width(420.dp)
                        .padding(24.dp)
                ) {
                    dialogContent()
                }
            }
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            textContentColor = KitsugiColors.TextSecondary,
            shape = RoundedCornerShape(26.dp),
            confirmButton = {},
            dismissButton = {},
            text = {
                dialogContent()
            }
        )
    }
}

