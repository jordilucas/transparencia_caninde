package br.gov.caninde.transparencia.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PersonAvatar(
    name: String,
    fotoUrl: String,
    size: Int = 36,
    modifier: Modifier = Modifier,
) {
    val sizeDp = size.dp
    if (fotoUrl.isNotBlank()) {
        RemoteAvatarImage(
            url = fotoUrl,
            name = name,
            size = sizeDp,
            modifier = modifier,
        )
    } else {
        InitialAvatar(name, size)
    }
}

@Composable
expect fun RemoteAvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
)
