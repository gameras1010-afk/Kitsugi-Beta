package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.profile.ActivityCard
import com.kitsugi.animelist.ui.screens.profile.KitsugiUserProfileViewModel
import com.kitsugi.animelist.ui.screens.profile.OtherUserProfileState
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch

@Composable
fun UserProfileActivityTab(
    state: OtherUserProfileState,
    accentColor: Color,
    appSettings: AppSettings,
    userId: Int,
    fallbackUsername: String?,
    fallbackAvatar: String?,
    viewModel: KitsugiUserProfileViewModel,
    apiClient: JikanApiClient,
    onFavoriteMediaClick: (Int, MediaType, String, String, String?) -> Unit,
    onActiveActivityIdChange: (Int?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    if (state.activities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Son aktivite bulunamadı.", color = KitsugiColors.TextMuted)
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
                    onActivityClick = { actId ->
                        onActiveActivityIdChange(actId)
                    },
                    onLikeClick = { actId ->
                        coroutineScope.launch {
                            apiClient.toggleLike(actId, "ACTIVITY")
                            viewModel.loadUser(userId, fallbackUsername, fallbackAvatar)
                        }
                    }
                )
            }

            if (state.activitiesHasNext) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .clickable { viewModel.loadNextActivitiesPage() }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Daha Fazla Yükle",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
