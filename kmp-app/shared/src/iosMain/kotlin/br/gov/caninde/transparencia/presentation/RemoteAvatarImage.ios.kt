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
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import org.jetbrains.skia.Image as SkiaImage
import androidx.compose.ui.graphics.toComposeImageBitmap

private val imageHttp by lazy { HttpClient(Darwin) }

@Composable
actual fun RemoteAvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        bitmap = null
        try {
            val bytes = imageHttp.get(url).readRawBytes()
            bitmap = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Throwable) {
            bitmap = null
        }
    }

    when (val loaded = bitmap) {
        null -> InitialAvatar(name, size.value.toInt())
        else -> Image(
            bitmap = loaded,
            contentDescription = name,
            modifier = modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}
