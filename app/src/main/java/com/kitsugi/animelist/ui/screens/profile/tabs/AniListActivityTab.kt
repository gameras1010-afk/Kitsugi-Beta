package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.ui.screens.profile.ActivityCard
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun AniListActivityTab(
    state: AniListProfileState,
    accentColor: Color,
    appSettings: AppSettings,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
    onActivityClick: ((Int) -> Unit)? = null,
    onLikeClick: ((Int) -> Unit)? = null,
    onDeleteClick: ((Int) -> Unit)? = null
) {
    if (state.activities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Henüz aktivite bulunmuyor.", color = KitsugiColors.TextMuted)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.activities.forEach { act ->
                ActivityCard(
                    activity = act,
                    accentColor = accentColor,
                    blurAdultMedia = appSettings.blurAdultMedia,
                    onMediaClick = { mediaId, mType ->
                        onFavoriteMediaClick(mediaId, mType, "anilist", "", null)
                    },
                    onActivityClick = { actId -> onActivityClick?.invoke(actId) },
                    onLikeClick = { actId -> onLikeClick?.invoke(actId) },
                    onDeleteClick = { actId -> onDeleteClick?.invoke(actId) }
                )
            }
        }
    }
}
