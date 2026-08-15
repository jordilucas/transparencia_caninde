package br.gov.caninde.transparencia.platform

import androidx.compose.runtime.Composable
import br.gov.caninde.transparencia.domain.PickedMedia

@Composable
expect fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): () -> Unit
