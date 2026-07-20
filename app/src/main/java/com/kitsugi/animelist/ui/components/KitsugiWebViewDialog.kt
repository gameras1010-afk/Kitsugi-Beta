package com.kitsugi.animelist.ui.components

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import eu.kanade.tachiyomi.network.NetworkHelper


@Composable
fun KitsugiWebViewDialog(
    title: String,
    url: String,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = {
            CookieManager.getInstance().flush()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = KitsugiColors.Surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = url,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Yenile", tint = KitsugiColors.TextSecondary)
                    }
                    IconButton(onClick = {
                        CookieManager.getInstance().flush()
                        onDismiss()
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextPrimary)
                    }
                }

                // Progress Bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor,
                        trackColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.1f)
                    )
                } else {
                    HorizontalDivider(color = KitsugiColors.Border)
                }

                // WebView Container
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        isLoading = true
                                        progress = 0
                                    }

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        isLoading = false
                                        // WebView cookie'lerini OkHttp CookieJar'a aktar (Cloudflare bypass için kritik)
                                        val finalUrl = pageUrl ?: url
                                        try {
                                            CookieManager.getInstance().flush()
                                            // Injekt singleton NetworkHelper üzerinden cookieJar'ı güncelle
                                            val nh = try {
                                                uy.kohesive.injekt.Injekt.get(NetworkHelper::class.java)
                                            } catch (_: Exception) {
                                                NetworkHelper(context)
                                            }
                                            nh.cookieJar.syncFromWebView(finalUrl)
                                        } catch (_: Exception) {}
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                        if (newProgress >= 100) {
                                            isLoading = false
                                        }
                                    }
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                }
                                // Üçüncü parti cookie'lere izin ver (bazı Türk kaynaklarda gerekli)
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                loadUrl(url)
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }


                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            CookieManager.getInstance().flush()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Tamamlandı", color = KitsugiColors.Surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
