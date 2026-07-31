package br.gov.caninde.transparencia.presentation.detail

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformVideoEmbed(embedUrl: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) webView.loadUrl(embedUrl)
        },
    )
}
