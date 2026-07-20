package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.components.KitsugiEmptyState

/**
 * Empty state shown when the user hasn't connected their account.
 */
@Composable
internal fun MyListNotConnectedState(
    selectedTabIndex: Int,
    isSimklSessionExpired: Boolean,
    onLogin: () -> Unit
) {
    val title = when (selectedTabIndex) {
        0 -> "AniList Bağlı Değil"
        1 -> "MyAnimeList Bağlı Değil"
        else -> {
            if (isSimklSessionExpired) "Simkl Oturum Süresi Doldu"
            else "Simkl Bağlı Değil"
        }
    }

    val subtitle = when (selectedTabIndex) {
        0 -> "AniList kütüphanenizi görüntülemek için hesabınızı bağlayın."
        1 -> "MyAnimeList kütüphanenizi görüntülemek için hesabınızı bağlayın."
        else -> {
            if (isSimklSessionExpired)
                "Simkl oturum süresi doldu. Senkronizasyonu sürdürmek için tekrar bağlayın."
            else
                "Simkl kütüphanenizi görüntülemek için hesabınızı bağlayın."
        }
    }

    val actionText = if (isSimklSessionExpired && selectedTabIndex == 2) "Yeniden Bağlan" else "Hesabı Bağla"

    KitsugiEmptyState(
        title = title,
        subtitle = subtitle,
        icon = if (isSimklSessionExpired && selectedTabIndex == 2) Icons.Rounded.Refresh else Icons.Rounded.AccountCircle,
        actionText = actionText,
        onActionClick = onLogin
    )
    Spacer(modifier = Modifier.height(18.dp))
}

/**
 * Empty state shown when the connected list has no entries yet.
 */
@Composable
internal fun MyListSyncPromptState() {
    KitsugiEmptyState(
        title = "Listeniz boş",
        subtitle = "Hesabınızdaki verileri çekmek için \"Senkronize Et\" butonunu kullanın.",
        icon = Icons.Rounded.FormatListBulleted
    )
    Spacer(modifier = Modifier.height(90.dp))
}
