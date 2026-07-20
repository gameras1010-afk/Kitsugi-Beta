package com.kitsugi.animelist

import androidx.compose.runtime.Composable
import com.kitsugi.animelist.ui.app.AppNavigationState
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.ui.screens.manga.MangaBrowseViewModel
import com.kitsugi.animelist.ui.screens.manga.MangaReaderScreen
import com.kitsugi.animelist.ui.screens.manga.MangaDetailScreen
import com.kitsugi.animelist.ui.screens.manga.MangaSourceHealthScreen
import com.kitsugi.animelist.ui.screens.manga.MangaBrowseScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppRootMangaPages(
    key: AppStateKey,
    navState: AppNavigationState,
    mangaViewModel: MangaViewModel,
    mangaBrowseViewModel: MangaBrowseViewModel,
    coroutineScope: CoroutineScope,
    onExportMangaSourceReportFile: (String) -> Unit
) {
    when (key) {
        is AppStateKey.MangaReader -> {
            val readerState = navState.mangaReaderNavState
            if (readerState != null) {
                MangaReaderScreen(
                    chapter          = readerState.chapter,
                    mangaDetails     = readerState.mangaDetails,
                    source           = readerState.source,
                    onBack           = { navState.mangaReaderNavState = null }
                )
            }
        }

        is AppStateKey.MangaDetail -> {
            val detailState = navState.mangaDetailNavState
            if (detailState != null) {
                MangaDetailScreen(
                    source        = detailState.source,
                    mangaDetails  = detailState.mangaDetails,
                    onOpenChapter = { chapter ->
                        navState.mangaReaderNavState = MangaReaderNavState(
                            source       = detailState.source,
                            mangaDetails = detailState.mangaDetails,
                            chapter      = chapter
                        )
                    },
                    onBack        = { navState.mangaDetailNavState = null }
                )
            }
        }

        is AppStateKey.MangaSourceHealth -> {
            MangaSourceHealthScreen(
                report = mangaViewModel.mangaSourceStateReport,
                installedSources = mangaViewModel.mangaSources,
                onBack = { navState.closeMangaSourceHealth() },
                onExportReportFile = { text -> onExportMangaSourceReportFile(text) },
                onQuickCheckSource = { src -> mangaViewModel.quickCheckSource(src) },
                onRefreshSourceMirror = { src -> mangaViewModel.refreshSourceMirror(src) },
                onResetSourceDiagnostics = { src -> mangaViewModel.resetSourceDiagnostics(src) },
                onClearSourceMirror = { src -> mangaViewModel.clearSourceMirror(src) },
                onClearAllSourceDiagnostics = { mangaViewModel.clearAllSourceDiagnostics() },
                onClearAllSourceConfigs = { mangaViewModel.clearAllSourceConfigs() },
                onIsSourceBusy = { src -> mangaViewModel.isSourceBusy(src) },
                onGetSourceFailureStreak = { src -> mangaViewModel.getSourceFailureStreak(src) },
                onGetSourceCooldownUntil = { src -> mangaViewModel.getSourceCooldownUntil(src) },
                onGetMangaConfiguredDomain = { src -> mangaViewModel.getConfiguredSourceDomain(src) ?: "" },
                onGetMangaConfiguredBaseUrl = { src -> mangaViewModel.getConfiguredSourceBaseUrl(src) ?: "" },
                onSetMangaSourceDomain = { src, value -> mangaViewModel.setSourceDomainOverride(src, value) },
            )
        }

        is AppStateKey.MangaBrowse -> {
            MangaBrowseScreen(
                repository    = mangaViewModel.mangaRepository,
                initialQuery  = navState.mangaBrowseQuery,
                vm            = mangaBrowseViewModel,
                onMangaClick  = { source, mangaDetails ->
                    val targetMediaId = navState.mangaBrowseMediaId
                    if (targetMediaId != null) {
                        coroutineScope.launch {
                            mangaViewModel.mangaRepository.saveMangaMapping(
                                mediaId = targetMediaId,
                                source = source.name,
                                url = mangaDetails.url,
                                title = mangaDetails.title,
                                thumbnailUrl = mangaDetails.thumbnailUrl
                            )
                        }
                        mangaBrowseViewModel.reset()
                        navState.closeMangaBrowse()
                    }
                    navState.mangaDetailNavState = MangaDetailNavState(
                        source       = source,
                        mangaDetails = mangaDetails
                    )
                },
                onBack        = {
                    mangaBrowseViewModel.reset()
                    navState.closeMangaBrowse()
                }
            )
        }
        else -> {}
    }
}
