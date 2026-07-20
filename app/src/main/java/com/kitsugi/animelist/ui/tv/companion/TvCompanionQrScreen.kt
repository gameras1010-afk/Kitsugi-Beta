package com.kitsugi.animelist.ui.tv.companion

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kitsugi.animelist.core.companion.DeviceIpAddress
import com.kitsugi.animelist.core.companion.QrCodeGenerator
import com.kitsugi.animelist.core.companion.TvCompanionServer
import com.kitsugi.animelist.core.companion.TvCompanionSessionManager
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Local palette tokens ───────────────────────────────────────────────────────

private val CompanionBg = Color(0xFF0D0D1A)
private val CompanionCard = Color(0xFF16213E)
private val CompanionAccent = Color(0xFF4F8EF7)
private val CompanionGreen = Color(0xFF4CAF50)
private val CompanionOrange = Color(0xFFFF9800)
private val CompanionRed = Color(0xFFE53935)
private val CompanionBorder = Color(0xFF2A2A4A)
private val CompanionText = Color(0xFFE8EAF6)
private val CompanionSubtext = Color(0xFF9FA8DA)

/**
 * Full-screen D-pad-compatible TV Companion QR pairing screen.
 *
 * Displays the local companion URL, port, and a live QR code generated from the
 * current session token. Reacts to [TvCompanionSessionManager] state changes to
 * show connection, pending-approval, and expired states.
 *
 * @param sessionManager  The shared [TvCompanionSessionManager] instance.
 * @param companionServer The running [TvCompanionServer] (used to read bound port).
 * @param onRefreshToken  Called when the user requests a token rotation.
 * @param onBack          Called when the user presses Back / Close.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCompanionQrScreen(
    sessionManager: TvCompanionSessionManager,
    companionServer: TvCompanionServer?,
    onRefreshToken: () -> Unit,
    onBack: () -> Unit
) {
    val sessionState by sessionManager.sessionState.collectAsState()
    val port = companionServer?.listeningPort ?: TvCompanionServer.PORT_RANGE_START
    val ip = remember { DeviceIpAddress.get() ?: "—" }
    val companionUrl = remember(ip, port) { "http://$ip:$port/companion" }

    // ── QR Bitmap generation ─────────────────────────────────────────────────
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(sessionState.token) {
        qrBitmap = null
        val token = sessionState.token.ifBlank { sessionManager.currentToken() }
        val fullUrl = "$companionUrl?token=$token"
        qrBitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generate(fullUrl, sizePx = 400)
        }
    }

    // ── Focus ─────────────────────────────────────────────────────────────────
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        closeFocusRequester.requestFocusAfterFrames(frames = 2)
    }

    // ── Animated pulse for "waiting" state ───────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "companion_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    // ── Root ──────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A3E), CompanionBg),
                    radius = 1800f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: Info panel ─────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = CompanionAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Telefon ile Yönet",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = CompanionText
                    )
                }

                Text(
                    text = "Aynı Wi-Fi ağındaki telefonunuzdan QR kodu okutun " +
                           "veya aşağıdaki adrese gidin.",
                    fontSize = 16.sp,
                    color = CompanionSubtext,
                    lineHeight = 22.sp
                )

                // URL card
                CompanionInfoCard(
                    icon = Icons.Filled.Wifi,
                    label = "Bağlantı Adresi",
                    value = companionUrl
                )

                // Connection status badge
                CompanionStatusBadge(state = sessionState.connectionState)

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onRefreshToken,
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .focusable(),
                        colors = ButtonDefaults.colors(
                            containerColor = CompanionCard,
                            contentColor = CompanionText,
                            focusedContainerColor = CompanionAccent,
                            focusedContentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Yeni QR")
                    }
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.colors(
                            containerColor = CompanionCard,
                            contentColor = CompanionSubtext,
                            focusedContainerColor = CompanionRed,
                            focusedContentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kapat")
                    }
                }
            }

            // ── Right: QR code ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .scale(
                        if (sessionState.connectionState ==
                            TvCompanionSessionManager.ConnectionState.IDLE) pulse else 1f
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(3.dp, CompanionAccent, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = qrBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Companion QR Kodu",
                        modifier = Modifier
                            .size(230.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    // Placeholder while generating
                    Text(
                        text = "QR Yükleniyor…",
                        color = CompanionBg,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ── Sub-components ─────────────────────────────────────────────────────────────

@Composable
private fun CompanionInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CompanionCard)
            .border(1.dp, CompanionBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CompanionAccent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontSize = 11.sp, color = CompanionSubtext, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                color = CompanionText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompanionStatusBadge(state: TvCompanionSessionManager.ConnectionState) {
    val (color, label, icon) = when (state) {
        TvCompanionSessionManager.ConnectionState.IDLE ->
            Triple(CompanionSubtext, "Bağlantı bekleniyor…", Icons.Filled.Wifi)
        TvCompanionSessionManager.ConnectionState.PENDING_APPROVAL ->
            Triple(CompanionOrange, "Onay bekliyor — TV'den onaylayın", Icons.Filled.Link)
        TvCompanionSessionManager.ConnectionState.CONNECTED ->
            Triple(CompanionGreen, "Bağlı", Icons.Filled.PhoneAndroid)
        TvCompanionSessionManager.ConnectionState.EXPIRED ->
            Triple(CompanionRed, "Oturum süresi doldu — Yeni QR oluşturun", Icons.Filled.Refresh)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
