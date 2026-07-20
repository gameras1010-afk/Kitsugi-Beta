package com.kitsugi.animelist.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import java.net.URLEncoder

/**
 * TV-optimized QR Login Dialog.
 *
 * Displayed automatically on Android TV when user taps a login button.
 * Shows QR codes for AniList, MAL, and Simkl — user scans with phone.
 *
 * On mobile this dialog is never shown (browser intent is used instead).
 */

private fun generateQrBitmapFromUrl(url: String, sizePx: Int = 400): Bitmap? {
    return runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}

private enum class QrAuthService(
    val displayName: String,
    val accentColor: Color
) {
    AniList("AniList", Color(0xFF02A9FF)),
    MAL("MyAnimeList", Color(0xFF2E51A2)),
    Simkl("Simkl", Color(0xFFE8BC0E))
}

@Composable
fun KitsugiTvQrLoginDialog(
    authViewModel: AuthViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val firstServiceFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        authViewModel.refreshAuthState()
        try { firstServiceFocusRequester.requestFocusAfterFrames(frames = 3) } catch (_: Exception) {}
    }

    var selectedService by remember { mutableStateOf<QrAuthService?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedService) {
        val service = selectedService ?: run {
            qrBitmap = null
            return@LaunchedEffect
        }
        isLoading = true
        qrBitmap = null

        // T3-01: BuildConfig'den alınır
        val ANILIST_CLIENT_ID = com.kitsugi.animelist.BuildConfig.ANILIST_CLIENT_ID
        val MAL_CLIENT_ID = com.kitsugi.animelist.BuildConfig.MAL_CLIENT_ID
        val REDIRECT = "malapp://auth"

        var url = ""
        var simklUserCode: String? = null

        when (service) {
            QrAuthService.AniList -> {
                url = "https://anilist.co/api/v2/oauth/authorize" +
                    "?client_id=$ANILIST_CLIENT_ID" +
                    "&redirect_uri=${URLEncoder.encode(REDIRECT, "UTF-8")}" +
                    "&response_type=code"
            }
            QrAuthService.MAL -> {
                url = "https://myanimelist.net/v1/oauth2/authorize?" +
                    "response_type=code" +
                    "&client_id=$MAL_CLIENT_ID" +
                    "&redirect_uri=${URLEncoder.encode(REDIRECT, "UTF-8")}"
            }
            QrAuthService.Simkl -> {
                runCatching {
                    val (code, verificationUrl) = ExternalAuthManager.getSimklPinSuspend(context)
                    simklUserCode = code
                    url = verificationUrl
                }.onFailure {
                    url = "https://simkl.com/pin"
                }
            }
        }

        qrBitmap = generateQrBitmapFromUrl(url, sizePx = 360)
        isLoading = false

        // Eğer Simkl seçildiyse ve user code alındıysa, polling yap
        if (service == QrAuthService.Simkl && !simklUserCode.isNullOrEmpty()) {
            val code = simklUserCode!!
            while (selectedService == QrAuthService.Simkl) {
                kotlinx.coroutines.delay(5000L)
                val success = runCatching {
                    ExternalAuthManager.pollSimklTokenSuspend(context, code)
                }.getOrDefault(false)

                if (success) {
                    android.widget.Toast.makeText(context, "Simkl bağlantısı başarılı.", android.widget.Toast.LENGTH_SHORT).show()
                    authViewModel.refreshAuthState()
                    // Profil bilgisini çekip settings'e yaz
                    runCatching {
                        val token = ExternalAuthManager.getSimklToken(context)
                        if (!token.isNullOrBlank()) {
                            val settingsDataStore = com.kitsugi.animelist.data.settings.SettingsDataStore(context)
                            val profile = com.kitsugi.animelist.data.auth.SimklImportManager.fetchUserProfile(token)
                            settingsDataStore.setSimklProfileInfo(
                                username = profile.name,
                                profileImageUri = profile.avatarUrl.orEmpty(),
                                bannerImageUri = ""
                            )
                        }
                    }
                    selectedService = null
                    break
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(KitsugiColors.Surface)
                .padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // ── Sol Panel: Servis Seçimi ──────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hesap Bağla",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telefonunuzla QR kodu okutun",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = KitsugiColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Servis butonları
                QrAuthService.entries.forEachIndexed { index, service ->
                    val isConnected = when (service) {
                        QrAuthService.AniList -> authViewModel.isAniListConnected
                        QrAuthService.MAL -> authViewModel.isMalConnected
                        QrAuthService.Simkl -> authViewModel.isSimklConnected
                    }
                    val isSelected = selectedService == service

                    QrServiceRow(
                        label = service.displayName,
                        isConnected = isConnected,
                        isSelected = isSelected,
                        accentColor = service.accentColor,
                        onSelect = {
                            selectedService = if (isSelected && !isConnected) null else service
                        },
                        onDisconnect = {
                            val svcName = when (service) {
                                QrAuthService.AniList -> "anilist"
                                QrAuthService.MAL -> "mal"
                                QrAuthService.Simkl -> "simkl"
                            }
                            authViewModel.disconnectExternalAccount(svcName)
                            if (selectedService == service) selectedService = null
                        },
                        modifier = if (index == 0) Modifier.focusRequester(firstServiceFocusRequester) else Modifier
                    )

                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Sağ Panel: QR Görseli ─────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "QR hazırlanıyor...",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    qrBitmap != null -> {
                        val service = selectedService
                        val serviceColor = service?.accentColor ?: accentColor

                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${service?.displayName} ile Giriş",
                                    color = serviceColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(12.dp)
                                ) {
                                    Image(
                                        bitmap = qrBitmap!!.asImageBitmap(),
                                        contentDescription = "QR Kod",
                                        modifier = Modifier.size(220.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Kamera veya tarayıcı ile okutun",
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📱",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Soldan bir servis seçin",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrServiceRow(
    label: String,
    isConnected: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isSelected -> accentColor
        isFocused -> Color.White.copy(alpha = 0.4f)
        else -> KitsugiColors.Border
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> accentColor.copy(alpha = 0.12f)
                    isFocused -> KitsugiColors.SurfaceStrong
                    else -> KitsugiColors.SurfaceSoft
                }
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .tvClickable(shape = RoundedCornerShape(12.dp)) { if (isConnected) onDisconnect() else onSelect() }
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused && !isConnected) onSelect()
            }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isConnected) Color(0xFF4CAF50) else accentColor)
            )
            Column {
                Text(
                    text = label,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = if (isConnected) "✓ Bağlı" else "QR ile bağlan",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) Color(0xFF4CAF50) else KitsugiColors.TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isConnected) KitsugiColors.SurfaceStrong else accentColor)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isConnected) "Çıkış" else "Seç",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
