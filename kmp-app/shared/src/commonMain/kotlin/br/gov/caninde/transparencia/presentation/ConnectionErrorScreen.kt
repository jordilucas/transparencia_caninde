package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.CamaraUiState
import br.gov.caninde.transparencia.domain.ConnectionState
import br.gov.caninde.transparencia.domain.PrefeituraUiState

fun hasCachedServerData(prefeitura: PrefeituraUiState, camara: CamaraUiState): Boolean =
    prefeitura.contratos.isNotEmpty() ||
        camara.parlamentares.isNotEmpty() ||
        prefeitura.secretarias.isNotEmpty() ||
        prefeitura.licitacoes.isNotEmpty() ||
        prefeitura.obras.isNotEmpty() ||
        prefeitura.lrf.isNotEmpty() ||
        prefeitura.lastUpdated.isNotBlank() ||
        camara.lastUpdated.isNotBlank()

fun shouldShowConnectionErrorScreen(
    connectionState: ConnectionState,
    prefeitura: PrefeituraUiState,
    camara: CamaraUiState,
    onOfflineSafeScreen: Boolean,
): Boolean {
    if (onOfflineSafeScreen) return false
    if (hasCachedServerData(prefeitura, camara)) return false
    return when (connectionState) {
        ConnectionState.Error -> true
        ConnectionState.Reconnecting -> true
        else -> false
    }
}

@Composable
fun ConnectionErrorScreen(
    connectionState: ConnectionState,
    onRetry: () -> Unit,
    onSobreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRetrying = connectionState == ConnectionState.Reconnecting ||
        connectionState == ConnectionState.Connecting

    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .background(AppColors.Red100, RoundedCornerShape(44.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = AppColors.Red700,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Sem conexão com o servidor",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Não foi possível carregar os dados públicos da Prefeitura e da Câmara. " +
                "O servidor pode estar iniciando — isso pode levar até um minuto no plano gratuito.",
            fontSize = 14.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(28.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "O que você pode fazer",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                ErrorHintRow("Aguarde um momento e toque em Tentar novamente.")
                ErrorHintRow("Verifique sua conexão com a internet.")
                ErrorHintRow("Consulte a página Sobre para entender como os dados são obtidos.")
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onRetry,
            enabled = !isRetrying,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Navy800,
                contentColor = AppColors.Card,
            ),
        ) {
            if (isRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.Card,
                )
                Spacer(Modifier.width(10.dp))
                Text("Conectando…", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tentar novamente", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSobreClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Navy800),
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver página Sobre", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ErrorHintRow(text: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("•", fontSize = 13.sp, color = AppColors.Blue500)
        Text(text, fontSize = 12.sp, color = AppColors.TextSecondary, lineHeight = 17.sp)
    }
}
