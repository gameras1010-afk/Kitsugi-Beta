package com.kitsugi.animelist.ui.components.posteroptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * V2-A07 – ProfileAvatarCircle
 *
 * Dairesel profil avatarı bileşeni – boyut parametresiyle esnek kullanım.
 * NuvioTV ProfileAvatarCircle.kt referans alındı.
 */
@Composable
fun ProfileAvatarCircle(
    imageUrl: String?,
    displayName: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val placeholder = displayName.firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp,
                color = accentColor
            )
        }
    }
}
