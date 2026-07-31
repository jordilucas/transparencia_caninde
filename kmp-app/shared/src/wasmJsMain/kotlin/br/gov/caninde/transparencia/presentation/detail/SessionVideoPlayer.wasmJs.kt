package br.gov.caninde.transparencia.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformVideoEmbed(embedUrl: String, modifier: Modifier) {
    // Wasm renderiza em canvas; embed HTML fica via link externo no SessionVideoPlayer.
}
