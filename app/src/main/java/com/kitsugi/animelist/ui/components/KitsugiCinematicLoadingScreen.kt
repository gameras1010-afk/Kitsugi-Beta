package com.kitsugi.animelist.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun KitsugiCinematicLoadingScreen(
    title: String,
    imageUrl: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null
) {
    val accentColor = LocalKitsugiAccent.current

    // Subtle scale-up animation for the center cover image to make it feel alive
    val infiniteTransition = rememberInfiniteTransition(label = "cinematic_loading")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // 1. Fullscreen blurred background image with subtle pulse
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scalePulse * 1.05f
                        scaleY = scalePulse * 1.05f
                        alpha = 0.35f
                    }
                    .blur(16.dp)
            )
        }

        // 2. High-quality dark overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f),
                            KitsugiColors.Background
                        )
                    )
                )
        )

        // 3. Top-left back button
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .tvClickable(shape = RoundedCornerShape(99.dp)) { onBackClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = KitsugiColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Geri",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // 4. Center Content: Poster + Title + Fluid Circular Loader
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Poster thumbnail card
            if (!imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scalePulse
                            scaleY = scalePulse
                        }
                        .shadow(24.dp, shape = RoundedCornerShape(20.dp), spotColor = accentColor)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, KitsugiColors.Border.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .background(KitsugiColors.Surface)
                        .size(width = 130.dp, height = 190.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Title or Logo
            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
            val showLogo = !logoUrl.isNullOrBlank() && !logoFailed

            if (showLogo) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(65.dp),
                    onError = { logoFailed = true }
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Sleek CircularProgressIndicator + Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.Surface.copy(alpha = 0.6f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor,
                        strokeWidth = 3.dp,
                        trackColor = KitsugiColors.Border.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Yükleniyor...",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
