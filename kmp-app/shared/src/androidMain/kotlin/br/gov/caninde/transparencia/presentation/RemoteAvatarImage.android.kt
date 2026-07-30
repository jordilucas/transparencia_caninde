package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage

@Composable
actual fun RemoteAvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier,
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = name,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = { InitialAvatar(name, size.value.toInt()) },
        error = { InitialAvatar(name, size.value.toInt()) },
    )
}
