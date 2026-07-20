package com.kitsugi.animelist.ui.screens.anime

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiErrorState
import com.kitsugi.animelist.ui.components.KitsugiHorizontalMediaSection
import com.kitsugi.animelist.ui.components.KitsugiInfoDialog
import com.kitsugi.animelist.ui.components.KitsugiPage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * @deprecated T4-04: Bu ekran AppNavigation/AppRoot'a bağlı değildir — ölü kod.
 * Ana keşif işlevselliği HomeScreen ve ExploreScreen üzerinden sağlanmaktadır.
 * Gelecekteki bir cleanup sprint'inde kaldırılabilir.
 */
@Deprecated("AnimeScreen is not connected to AppNavigation. Use HomeScreen or ExploreScreen instead.")
@Composable
fun AnimeScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    onSeeAllSection: (title: String, results: List<JikanSearchResult>) -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    viewModel: AnimeViewModel = viewModel()
) {
    val accentColor = LocalKitsugiAccent.current

    val filteredTopAnime = viewModel.topAnime.filter { showAdultContent || !it.isAdult }
    val filteredAiringAnime = viewModel.airingAnime.filter { showAdultContent || !it.isAdult }
    val filteredUpcomingAnime = viewModel.upcomingAnime.filter { showAdultContent || !it.isAdult }

    fun isAlreadyInList(result: JikanSearchResult): Boolean {
        return currentEntries.any { entry ->
            entry.matches(result)
        }
    }

    fun handleDirectAdd(result: JikanSearchResult, synopsis: String? = null) {
        onAddSelectionToList(ApiSearchSelection(result = result, synopsis = synopsis))
    }

    KitsugiPage(
        title = "Anime",
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        TextButton(
            enabled = !viewModel.isLoading,
            onClick = { viewModel.loadData(forceRefresh = true) }
        ) {
            Text(
                text = if (viewModel.isLoading) "Yükleniyor..." else "Yenile",
                color = if (viewModel.isLoading) KitsugiColors.TextMuted else accentColor,
                fontWeight = FontWeight.Bold
            )
        }



        if (viewModel.errorMessage != null) {
            Spacer(modifier = Modifier.height(18.dp))
            KitsugiErrorState(
                message = viewModel.errorMessage.orEmpty(),
                onRetryClick = { viewModel.loadData(forceRefresh = true) }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        KitsugiHorizontalMediaSection(
            title = "Popüler Anime",
            results = filteredTopAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = ::isAlreadyInList,
            onItemClick = onOpenApiDetail,
            onSeeAllClick = { onSeeAllSection("Popüler Anime", filteredTopAnime) }
        )

        Spacer(modifier = Modifier.height(26.dp))

        KitsugiHorizontalMediaSection(
            title = "Yayındaki Anime",
            results = filteredAiringAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = ::isAlreadyInList,
            onItemClick = onOpenApiDetail,
            onSeeAllClick = { onSeeAllSection("Yayındaki Anime", filteredAiringAnime) }
        )

        Spacer(modifier = Modifier.height(26.dp))

        KitsugiHorizontalMediaSection(
            title = "Yaklaşan Anime",
            results = filteredUpcomingAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = ::isAlreadyInList,
            onItemClick = onOpenApiDetail,
            onSeeAllClick = { onSeeAllSection("Yaklaşan Anime", filteredUpcomingAnime) }
        )

        Spacer(modifier = Modifier.height(90.dp))
    }
}