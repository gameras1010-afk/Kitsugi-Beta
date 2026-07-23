package com.kitsugi.animelist.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

@Composable
fun KitsugiHtmlWebView(
    html: String,
    modifier: Modifier = Modifier,
    hardwareEnabled: Boolean = true,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    
    val bgColor = KitsugiColors.Background
    val fontColor = KitsugiColors.TextPrimary
    val fontColorSecondary = KitsugiColors.TextSecondary
    val surfaceColor = KitsugiColors.Surface
    val borderColor = KitsugiColors.SurfaceSoft
    
    val htmlConverted by remember(html, accentColor, bgColor, fontColor, fontColorSecondary, surfaceColor, borderColor) {
        derivedStateOf {
            generateHtml(
                html = html,
                backgroundColor = bgColor,
                fontColor = fontColor,
                fontColorSecondary = fontColorSecondary,
                linkColor = accentColor,
                surfaceColor = surfaceColor,
                borderColor = borderColor
            )
        }
    }
    
    val webClient = remember {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString()
                if (url != null) {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
                return true
            }
        }
    }

    WebViewWrapper(
        data = htmlConverted,
        modifier = modifier.fillMaxWidth(),
        hardwareEnabled = hardwareEnabled,
        onCreated = { webView ->
            webView.background = Color.Transparent.toArgb().toDrawable()
            webView.isScrollContainer = false
            webView.isVerticalScrollBarEnabled = false
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.javaScriptEnabled = true
            if (onImageClick != null) {
                webView.addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onImageClick(urlsJson: String, index: Int) {
                        runCatching {
                            val jsonArray = org.json.JSONArray(urlsJson)
                            val urls = mutableListOf<String>()
                            for (i in 0 until jsonArray.length()) {
                                urls.add(jsonArray.getString(i))
                            }
                            webView.post {
                                onImageClick(urls, index)
                            }
                        }
                    }
                }, "AndroidInterface")
            }
        },
        client = webClient
    )
}

@Composable
private fun WebViewWrapper(
    data: String,
    modifier: Modifier = Modifier,
    hardwareEnabled: Boolean = true,
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
    client: WebViewClient = remember { WebViewClient() },
    chromeClient: WebChromeClient? = null,
) {
    BoxWithConstraints(modifier) {
        val width = if (this.constraints.hasFixedWidth) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val height = if (this.constraints.hasFixedHeight) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        val layoutParams = FrameLayout.LayoutParams(width, height)

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    this.alpha = 0.99f
                    onCreated(this)
                    if (!hardwareEnabled) setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    this.layoutParams = layoutParams
                    chromeClient?.let { webChromeClient = it }
                    webViewClient = client
                    loadDataWithBaseURL(null, data, null, "utf-8", null)
                }
            },
            modifier = Modifier,
            onRelease = {
                it.visibility = View.INVISIBLE
                onDispose(it)
                it.stopLoading()
                it.destroy()
            }
        )
    }
}

private fun Color.toHexHtml(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

private fun generateHtml(
    html: String,
    backgroundColor: Color,
    fontColor: Color,
    fontColorSecondary: Color,
    linkColor: Color,
    surfaceColor: Color,
    borderColor: Color
): String {
    val bgHex = backgroundColor.toHexHtml()
    val fontHex = fontColor.toHexHtml()
    val secHex = fontColorSecondary.toHexHtml()
    val linkHex = linkColor.toHexHtml()
    val surfHex = surfaceColor.toHexHtml()
    val borderHex = borderColor.toHexHtml()

    val css = """
        <style type='text/css'>
            body {
                background-color: $bgHex;
                color: $fontHex;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                font-size: 14px;
                line-height: 1.6;
                margin: 0;
                padding: 0;
            }
            a {
                color: $linkHex;
                text-decoration: none;
                font-weight: bold;
            }
            a:hover {
                text-decoration: underline;
            }
            img {
                max-width: 100%;
                height: auto;
                border-radius: 12px;
                margin: 8px 0;
                display: block;
            }
            .markdown_spoiler {
                color: $fontHex;
                background-color: $fontHex;
                border-radius: 4px;
                padding: 0 4px;
                cursor: pointer;
                transition: background-color 0.2s;
            }
            .markdown_spoiler:hover, .markdown_spoiler:focus, .markdown_spoiler:active {
                background-color: transparent;
            }
            .markdown_spoiler.revealed {
                background-color: transparent;
            }
            .markdown_spoiler:not(.revealed) a {
                color: $fontHex !important;
            }
            blockquote {
                margin: 12px 0;
                padding: 8px 16px;
                background-color: $surfHex;
                border-left: 4px solid $linkHex;
                border-radius: 0 8px 8px 0;
                color: $secHex;
                font-style: italic;
            }
            pre, code {
                font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
                background-color: $surfHex;
                border-radius: 6px;
                padding: 2px 6px;
                font-size: 0.9em;
            }
            pre {
                padding: 12px;
                overflow-x: auto;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 16px 0;
            }
            th, td {
                border: 1px solid $borderHex;
                padding: 8px 12px;
                text-align: left;
            }
            th {
                background-color: $surfHex;
                font-weight: bold;
            }
        </style>
    """.trimIndent()

    return """
        <HTML>
        <head>
            <meta name='viewport' content='width=device-width, shrink-to-fit=YES'>
            $css
        </head>
        <BODY>
        <div id="kitsugi-container">${formatCompatibleHtml(html)}</div>
        <script type="text/javascript">
            document.addEventListener("DOMContentLoaded", function() {
                var spoilers = document.querySelectorAll(".markdown_spoiler");
                spoilers.forEach(function(el) {
                    el.addEventListener("click", function() {
                        el.classList.toggle("revealed");
                    });
                });
                
                var images = document.querySelectorAll("img");
                var imageUrls = Array.from(images).map(function(img) { return img.src; });
                images.forEach(function(img, index) {
                    img.addEventListener("click", function() {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onImageClick(JSON.stringify(imageUrls), index);
                        }
                    });
                });
            });
        </script>
        </BODY>
        </HTML>
    """.trimIndent()
}

private fun formatCompatibleHtml(html: String): String {
    if (html.isBlank()) return ""
    return html
        .replace(Regex("~!(.*?)!~", RegexOption.DOT_MATCHES_ALL)) { match ->
            "<span class=\"markdown_spoiler\" onclick=\"this.classList.toggle('revealed')\">${match.groupValues[1]}</span>"
        }
        .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
        .replace(Regex("__(.+)__"), "<b>$1</b>")
        .replace(Regex("~~~(.*?)~~~", RegexOption.DOT_MATCHES_ALL)) { match ->
            "<div style=\"text-align: center;\">${match.groupValues[1]}</div>"
        }
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}
