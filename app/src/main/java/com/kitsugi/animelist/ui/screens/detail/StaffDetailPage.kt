package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiStaffDetail
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.utils.copyOnDoubleTap
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester

sealed interface StaffDetailState {
    object Loading : StaffDetailState
    data class Error(val message: String) : StaffDetailState
    data class Success(val detail: KitsugiStaffDetail) : StaffDetailState
}

@Composable
fun StaffDetailPage(
    staffId: Int,
    source: String,
    onBackClick: () -> Unit,
    onCharacterClick: (characterId: Int, characterSource: String, name: String?, imageUrl: String?) -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    name: String? = null,
    imageUrl: String? = null,
    titleLanguage: String = "ROMAJI",
    preferredTranslator: String = "DEFAULT"
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current

    // Obtain ViewModel
    val viewModel: StaffDetailViewModel = viewModel(key = "staff_${source}_${staffId}")

    // Load staff in ViewModel
    LaunchedEffect(staffId, source) {
        viewModel.loadStaff(staffId, source, name)
    }

    // Collect states from ViewModel
    val state by viewModel.state.collectAsState()
    val translatedBio by viewModel.translatedBio.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val isAniListSource = source.lowercase() == "anilist"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        when (val currentState = state) {
            is StaffDetailState.Loading -> {
                KitsugiCinematicLoadingScreen(
                    title = name ?: "Ekip Üyesi Yükleniyor...",
                    imageUrl = imageUrl,
                    onBackClick = onBackClick
                )
            }
            is StaffDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentState.message,
                            color = KitsugiColors.AccentRed,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                viewModel.retry()
                            }
                        ) {
                            Text("Yeniden Dene", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is StaffDetailState.Success -> {
                val detail = currentState.detail
                val listState = rememberLazyListState()
                val showFloatingHeader = listState.firstVisibleItemIndex >= 1
                val tabs = listOf("Hakkında", "Karakterler", "Yapımlar")
                @OptIn(ExperimentalFoundationApi::class)
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
                val selectedTab = pagerState.currentPage
                val coroutineScope = rememberCoroutineScope()
                val isTv = LocalIsTv.current
                val tabListState = rememberLazyListState()
                var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
                var activeGalleryIndex by remember { mutableStateOf(0) }
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                // TV odak highway
                val leftPanelFocusRequester = remember { FocusRequester() }
                val tabBarFocusRequester = remember { FocusRequester() }

                if (isLandscape) {
                    // ── LANDSCAPE: Sol hero/bio paneli + Sağ tab paneli ──
                    KitsugiPageEnter {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Sol panel
                                Column(
                                    modifier = Modifier
                                        .weight(0.38f)
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                    ) {
                                        if (!detail.imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = detail.imageUrl,
                                                contentDescription = detail.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .focusRequester(leftPanelFocusRequester)
                                                    .focusProperties { right = tabBarFocusRequester }
                                                    .tvClickable {
                                                        activeGalleryImages = listOfNotNull(detail.imageUrl)
                                                        activeGalleryIndex = 0
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(accentColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(detail.name.take(2).uppercase(), color = accentColor, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(listOf(
                                                    KitsugiColors.Background.copy(alpha = 0.05f),
                                                    KitsugiColors.Background.copy(alpha = 0.30f),
                                                    KitsugiColors.Background.copy(alpha = 0.72f),
                                                    KitsugiColors.Background
                                                ))
                                            )
                                        )
                                        // Top Action Bar: Back (left) + Share & Favourite (right)
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, top = 24.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(onClick = onBackClick) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription = "Geri",
                                                        tint = KitsugiColors.TextPrimary
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    IconButton(onClick = {
                                                        val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                                                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Share,
                                                            contentDescription = "Paylaş",
                                                            tint = KitsugiColors.TextPrimary
                                                        )
                                                    }
                                                }

                                                if (isAniListSource) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        IconButton(onClick = { viewModel.toggleFavourite() }) {
                                                            Icon(
                                                                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                                contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                                                tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        DetailPill(text = source.uppercase(), color = accentColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = detail.name, modifier = Modifier.copyOnDoubleTap(context, detail.name), color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                        if (!detail.nativeName.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = detail.nativeName, modifier = Modifier.copyOnDoubleTap(context, detail.nativeName), color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                // Sağ panel
                                Column(
                                    modifier = Modifier.weight(0.62f).fillMaxSize()
                                ) {
                                    LaunchedEffect(selectedTab) {
                                        tabListState.animateScrollToItem(selectedTab)
                                    }
                                    LazyRow(
                                        state = tabListState,
                                        modifier = Modifier.fillMaxWidth()
                                            .focusRequester(tabBarFocusRequester)
                                            .focusProperties { left = leftPanelFocusRequester }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(tabs.size) { index ->
                                            val title = tabs[index]
                                            val isSelected = selectedTab == index
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (isSelected) accentColor else KitsugiColors.Surface).tvClickable(shape = RoundedCornerShape(999.dp)) { coroutineScope.launch { pagerState.animateScrollToPage(index) } }.padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(title, color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold)
                                            }
                                        }
                                    }
                                    @OptIn(ExperimentalFoundationApi::class)
                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isTv,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        when (page) {
                                            0 -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    if (detail.alternativeNames.isNotEmpty()) {
                                                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.Surface).padding(16.dp)) {
                                                            Text("Diğer İsimler", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(detail.alternativeNames.joinToString(", "), color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                    }
                                                    val hasDemographics = detail.gender != null || detail.age != null || detail.birthday != null || detail.homeTown != null || detail.occupation != null
                                                    if (hasDemographics) {
                                                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.Surface).padding(16.dp)) {
                                                            Text("Bilgiler", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                if (detail.occupation != null) InfoRow("Görev / Meslek", detail.occupation)
                                                                if (detail.gender != null) InfoRow("Cinsiyet", detail.gender)
                                                                if (detail.age != null) InfoRow("Yaş", detail.age)
                                                                if (detail.birthday != null) InfoRow("Doğum Günü", detail.birthday)
                                                                if (detail.homeTown != null) InfoRow("Memleket", detail.homeTown)
                                                            }
                                                        }
                                                    }
                                                    val displayBio = translatedBio ?: detail.biography
                                                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.Surface).padding(16.dp)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Biyografi", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                            if (!detail.biography.isNullOrBlank()) {
                                                                IconButton(onClick = { context.openTranslator(detail.biography, preferredTranslator) }, modifier = Modifier.size(36.dp)) {
                                                                    Icon(Icons.Rounded.Translate, contentDescription = "Çevir", tint = accentColor)
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                IconButton(onClick = {
                                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("biography", displayBio))
                                                                    android.widget.Toast.makeText(context, "Panoya kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                                                                }, modifier = Modifier.size(36.dp)) {
                                                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopyala", tint = KitsugiColors.TextSecondary)
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        if (displayBio.isNullOrBlank()) Text("Biyografi bulunmuyor.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                                                        else KitsugiMarkdownText(text = displayBio)
                                                    }
                                                }
                                            }
                                            1 -> {
                                                if (detail.characterRoles.isEmpty()) Text("Karakter seslendirme bilgisi bulunmuyor.", color = KitsugiColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
                                                else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    detail.characterRoles.forEach { role -> StaffCharacterRoleCard(role = role, titleLanguage = titleLanguage, onCharacterClick = onCharacterClick, onMediaClick = onMediaClick) }
                                                }
                                            }
                                            2 -> {
                                                if (detail.mediaWorks.isEmpty()) Text("Yapım katkı bilgisi bulunmuyor.", color = KitsugiColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
                                                else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    detail.mediaWorks.forEach { work -> StaffMediaWorkRow(work = work, titleLanguage = titleLanguage, onMediaClick = onMediaClick) }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                    } // end HorizontalPager
                                }
                            }
                            if (activeGalleryImages.isNotEmpty()) {
                                KitsugiImageGalleryDialog(imageUrls = activeGalleryImages, initialIndex = activeGalleryIndex, title = detail.name, onDismiss = { activeGalleryImages = emptyList() })
                            }
                        }
                    }
                } else {
                    // ── PORTRAIT: Mevcut LazyColumn düzeni ──
                KitsugiPageEnter {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Hero section
                            item(key = "hero") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(380.dp)
                                ) {
                                    if (!detail.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = detail.imageUrl,
                                            contentDescription = detail.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .tvClickable {
                                                    activeGalleryImages = listOfNotNull(detail.imageUrl)
                                                    activeGalleryIndex = 0
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = detail.name.take(2).uppercase(),
                                                color = accentColor,
                                                style = MaterialTheme.typography.displayLarge,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    // Gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        KitsugiColors.Background.copy(alpha = 0.05f),
                                                        KitsugiColors.Background.copy(alpha = 0.30f),
                                                        KitsugiColors.Background.copy(alpha = 0.72f),
                                                        KitsugiColors.Background
                                                    )
                                                )
                                            )
                                    )

                                    // Top Action Bar: Back (left) + Share & Favourite (right)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(onClick = onBackClick) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Geri",
                                                    tint = KitsugiColors.TextPrimary
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(onClick = {
                                                    val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                                                    com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Share,
                                                        contentDescription = "Paylaş",
                                                        tint = KitsugiColors.TextPrimary
                                                    )
                                                }
                                            }

                                            if (isAniListSource) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    IconButton(onClick = { viewModel.toggleFavourite() }) {
                                                        Icon(
                                                            imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                            contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                                            tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Staff Name Info
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(20.dp)
                                    ) {
                                        DetailPill(
                                            text = source.uppercase(),
                                            color = accentColor
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = detail.name,
                                            modifier = Modifier.copyOnDoubleTap(context, detail.name),
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!detail.nativeName.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = detail.nativeName,
                                                modifier = Modifier.copyOnDoubleTap(context, detail.nativeName),
                                                color = KitsugiColors.TextSecondary,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Tabs row
                            stickyHeader(key = "tabs") {
                                LaunchedEffect(selectedTab) {
                                    if (listState.firstVisibleItemIndex > 1) {
                                        listState.scrollToItem(1)
                                    }
                                    tabListState.animateScrollToItem(selectedTab)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(KitsugiColors.Surface.copy(alpha = 0.97f))
                                ) {
                                    AnimatedVisibility(
                                        visible = showFloatingHeader,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            IconButton(onClick = onBackClick) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Geri",
                                                    tint = KitsugiColors.TextPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = detail.name,
                                                color = KitsugiColors.TextPrimary,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                                                com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Share,
                                                    contentDescription = "Paylaş",
                                                    tint = KitsugiColors.TextSecondary
                                                )
                                            }
                                            if (isAniListSource) {
                                                IconButton(onClick = { viewModel.toggleFavourite() }) {
                                                    Icon(
                                                        imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                        contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                                        tint = if (isFavourite) accentColor else KitsugiColors.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    LazyRow(
                                        state = tabListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(tabs.size) { index ->
                                            val title = tabs[index]
                                            val isSelected = selectedTab == index
                                            val bgColor = if (isSelected) accentColor else KitsugiColors.Surface
                                            val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(bgColor)
                                                    .tvClickable(shape = RoundedCornerShape(999.dp)) { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = title,
                                                    color = textColor,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Tab content rendering
                            item(key = "content") {
                                val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val currentPage = pagerState.currentPage
                                val currentPageOffset = pagerState.currentPageOffsetFraction
                                val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

                                val currentHeightPx = pageHeights[currentPage] ?: 0
                                val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

                                val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                                    val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                                        currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                                    } else if (currentHeightPx > 0) {
                                        currentHeightPx.toFloat()
                                    } else {
                                        0f
                                    }
                                    if (heightPx > 0f) with(density) { heightPx.toDp() } else null
                                }

                                val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

                                @OptIn(ExperimentalFoundationApi::class)
                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = !isTv,
                                    beyondViewportPageCount = 1,
                                    pageSpacing = 12.dp,
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .layout { measurable, constraints ->
                                            val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                                            val placeable = measurable.measure(
                                                constraints.copy(
                                                    minHeight = minPagerHeightPx,
                                                    maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                                                )
                                            )
                                            val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                                            layout(placeable.width, height) {
                                                placeable.placeRelative(0, 0)
                                            }
                                        }
                                        .clipToBounds()
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 600.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    pageHeights[page] = coordinates.size.height
                                                }
                                        ) {
                                            when (page) {
                                                0 -> { // Hakkında
                                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                        if (detail.alternativeNames.isNotEmpty()) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(18.dp))
                                                                    .background(KitsugiColors.Surface)
                                                                    .padding(16.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Diğer İsimler",
                                                                    color = KitsugiColors.TextPrimary,
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = detail.alternativeNames.joinToString(", "),
                                                                    color = KitsugiColors.TextSecondary,
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                            }
                                                        }

                                                        val hasDemographics = detail.gender != null || detail.age != null || detail.birthday != null || detail.homeTown != null || detail.occupation != null
                                                        if (hasDemographics) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(18.dp))
                                                                    .background(KitsugiColors.Surface)
                                                                    .padding(16.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Bilgiler",
                                                                    color = KitsugiColors.TextPrimary,
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(modifier = Modifier.height(12.dp))

                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    if (detail.occupation != null) InfoRow(label = "Görev / Meslek", value = detail.occupation)
                                                                    if (detail.gender != null) InfoRow(label = "Cinsiyet", value = detail.gender)
                                                                    if (detail.age != null) InfoRow(label = "Yaş", value = detail.age)
                                                                    if (detail.birthday != null) InfoRow(label = "Doğum Günü", value = detail.birthday)
                                                                    if (detail.homeTown != null) InfoRow(label = "Memleket", value = detail.homeTown)
                                                                }
                                                            }
                                                        }

                                                        val displayBio = translatedBio ?: detail.biography
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(18.dp))
                                                                .background(KitsugiColors.Surface)
                                                                .padding(16.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "Biyografi",
                                                                    color = KitsugiColors.TextPrimary,
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.weight(1f)
                                                                )

                                                                if (!detail.biography.isNullOrBlank()) {
                                                                    IconButton(
                                                                        onClick = { context.openTranslator(detail.biography, preferredTranslator) },
                                                                        modifier = Modifier.size(36.dp)
                                                                    ) {
                                                                        Icon(Icons.Rounded.Translate, contentDescription = "Çevir", tint = accentColor)
                                                                    }
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    IconButton(
                                                                        onClick = {
                                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("biography", displayBio))
                                                                            android.widget.Toast.makeText(context, "Panoya kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                                                                        },
                                                                        modifier = Modifier.size(36.dp)
                                                                    ) {
                                                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopyala", tint = KitsugiColors.TextSecondary)
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            if (displayBio.isNullOrBlank()) {
                                                                Text("Biyografi bulunmuyor.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                                                            } else {
                                                                KitsugiMarkdownText(text = displayBio)
                                                            }
                                                        }
                                                    }
                                                }
                                                1 -> { // Karakterler
                                                    if (detail.characterRoles.isEmpty()) {
                                                        Text("Karakter seslendirme bilgisi bulunmuyor.", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                                                    } else {
                                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            detail.characterRoles.forEach { role ->
                                                                StaffCharacterRoleCard(role = role, titleLanguage = titleLanguage, onCharacterClick = onCharacterClick, onMediaClick = onMediaClick)
                                                            }
                                                        }
                                                    }
                                                }
                                                2 -> { // Yapımlar
                                                    if (detail.mediaWorks.isEmpty()) {
                                                        Text("Yapım katkı bilgisi bulunmuyor.", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                                                    } else {
                                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            detail.mediaWorks.forEach { work ->
                                                                StaffMediaWorkRow(work = work, titleLanguage = titleLanguage, onMediaClick = onMediaClick)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(50.dp))
                                        }
                                    }
                                } // end HorizontalPager
                            }
                        }

                        if (activeGalleryImages.isNotEmpty()) {
                            KitsugiImageGalleryDialog(
                                imageUrls = activeGalleryImages,
                                initialIndex = activeGalleryIndex,
                                title = detail.name,
                                onDismiss = { activeGalleryImages = emptyList() }
                            )
                        }


                    }
                }
            } // end else (portrait)
        } // end Success
    } // end when
} // end outer Box
} // end StaffDetailPage

