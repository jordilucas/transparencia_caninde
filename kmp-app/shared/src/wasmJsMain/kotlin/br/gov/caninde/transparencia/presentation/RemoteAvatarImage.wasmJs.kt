package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import br.gov.caninde.transparencia.data.WebSocketEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import org.jetbrains.skia.Image as SkiaImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.koin.compose.koinInject

private val imageHttp by lazy { HttpClient(Js) }

@Composable
actual fun RemoteAvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier,
) {
    val endpoint: WebSocketEndpoint = koinInject()
    val fetchUrl = remember(url, endpoint) {
        endpoint.mediaProxyUrl(url, ::encodeUriSegment)
    }

    var bitmap by remember(fetchUrl) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(fetchUrl) { mutableStateOf(false) }

    LaunchedEffect(fetchUrl) {
        if (url.isBlank()) {
            failed = true
            bitmap = null
            return@LaunchedEffect
        }
        failed = false
        bitmap = null
        try {
            val bytes = imageHttp.get(fetchUrl).readRawBytes()
            bitmap = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Throwable) {
            failed = true
        }
    }

    when {
        bitmap != null -> Image(
            bitmap = bitmap!!,
            contentDescription = name,
            modifier = modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        else -> InitialAvatar(name, size.value.toInt())
    }
}
