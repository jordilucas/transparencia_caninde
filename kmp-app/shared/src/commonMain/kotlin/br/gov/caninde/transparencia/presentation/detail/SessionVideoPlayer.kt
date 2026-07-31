package br.gov.caninde.transparencia.presentation.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.gov.caninde.transparencia.domain.CAMARA_PORTAL_BASE
import br.gov.caninde.transparencia.domain.youtubeWatchUrl

@Composable
fun SessionVideoPlayer(
    embedUrl: String,
    watchUrl: String,
    modifier: Modifier = Modifier,
) {
    val resolvedWatch = youtubeWatchUrl(embedUrl).ifBlank { watchUrl }
    if (embedUrl.isNotBlank()) {
        PlatformVideoEmbed(
            embedUrl = embedUrl,
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
    if (resolvedWatch.isNotBlank()) {
        DetailLinkAction(
            label = "Transmissão",
            url = resolvedWatch,
            baseUrl = CAMARA_PORTAL_BASE,
            actionText = if (embedUrl.isBlank()) "Assistir sessão" else "Abrir no YouTube",
            usePdfIcon = false,
        )
    }
}

@Composable
expect fun PlatformVideoEmbed(embedUrl: String, modifier: Modifier)
