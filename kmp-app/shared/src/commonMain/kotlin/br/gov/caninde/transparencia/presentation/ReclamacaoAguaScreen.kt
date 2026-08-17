package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.data.ReclamacaoAguaViewModel
import br.gov.caninde.transparencia.domain.ReclamacaoAgua
import br.gov.caninde.transparencia.domain.ReclamacaoAguaStats
import br.gov.caninde.transparencia.domain.ShareTexts
import br.gov.caninde.transparencia.platform.openExternalUrl
import br.gov.caninde.transparencia.platform.rememberMediaPicker
import br.gov.caninde.transparencia.platform.shareContent
import org.koin.compose.koinInject

@Composable
fun ReclamacaoAguaScreen(
    onSobreClick: () -> Unit = {},
    viewModel: ReclamacaoAguaViewModel = koinInject(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Registrar", "Painel")
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tab) {
        if (tab == 1) viewModel.carregarDashboard()
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(Modifier.fillMaxWidth().background(AppColors.Navy800)) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)) {
                        Text(
                            "Falta de água",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Blue100,
                        )
                        Text(
                            "Registre e acompanhe reclamações em Canindé/CE",
                            fontSize = 11.sp,
                            color = AppColors.Blue300,
                        )
                    }
                    Row(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = {
                            AppAnalytics.logShare("reclamacao_agua")
                            shareContent("Falta de água — Canindé", ShareTexts.reclamacaoAgua())
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = AppColors.Blue100)
                        }
                        IconButton(onClick = onSobreClick) {
                            Icon(Icons.Default.Info, contentDescription = "Sobre", tint = AppColors.Blue100)
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = AppColors.Navy800,
                    contentColor = AppColors.Blue100,
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }
            }
        }

        if (!uiState.firebaseConfigured || !uiState.supabaseConfigured) {
            BackendSetupBanner(
                firebaseConfigured = uiState.firebaseConfigured,
                supabaseConfigured = uiState.supabaseConfigured,
            )
        }

        when (tab) {
            0 -> ReclamacaoFormTab(
                uiState = uiState,
                onEnderecoChange = viewModel::onEnderecoChange,
                onSetorChange = viewModel::onSetorChange,
                onDiasChange = viewModel::onDiasSemAguaChange,
                onMediaPicked = viewModel::onMediaPicked,
                onSubmit = viewModel::enviarReclamacao,
                onDismissSuccess = viewModel::clearSubmitStatus,
            )
            1 -> ReclamacaoDashboardTab(
                uiState = uiState,
                onRefresh = { viewModel.carregarDashboard(force = true) },
            )
        }
    }
}

@Composable
private fun BackendSetupBanner(
    firebaseConfigured: Boolean,
    supabaseConfigured: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Amber100),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.Amber700)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Configuração pendente",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Amber700,
                )
                if (!firebaseConfigured) {
                    Text(
                        "Firebase: preencha FirebaseConfig.kt (dados e painel).",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = AppColors.TextSecondary,
                    )
                }
                if (!supabaseConfigured) {
                    Text(
                        "Supabase: preencha SupabaseConfig.kt (fotos/vídeos, plano free). Veja supabase/README.md.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = AppColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReclamacaoFormTab(
    uiState: br.gov.caninde.transparencia.domain.ReclamacaoAguaUiState,
    onEnderecoChange: (String) -> Unit,
    onSetorChange: (String) -> Unit,
    onDiasChange: (String) -> Unit,
    onMediaPicked: (br.gov.caninde.transparencia.domain.PickedMedia?) -> Unit,
    onSubmit: () -> Unit,
    onDismissSuccess: () -> Unit,
) {
    val pickMedia = rememberMediaPicker(onMediaPicked)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.submitSuccess) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.Green100),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.Green700)
                    Text(
                        "Reclamação enviada com sucesso. Obrigado por contribuir.",
                        fontSize = 13.sp,
                        color = AppColors.Green700,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissSuccess) {
                        Text("OK", color = AppColors.Green700)
                    }
                }
            }
        }

        uiState.submitError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.Red100),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppColors.Red700)
                    Text(error, fontSize = 13.sp, color = AppColors.Red700)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Nova reclamação",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Navy800,
                )
                Text(
                    "Informe endereço, setor de rodízio do SAAE, dias sem água e anexe foto ou vídeo como comprovante.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AppColors.TextSecondary,
                )

                OutlinedTextField(
                    value = uiState.endereco,
                    onValueChange = onEnderecoChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Endereço / bairro / referência") },
                    minLines = 2,
                    singleLine = false,
                )

                Text("Setor de rodízio", fontSize = 12.sp, color = AppColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.setor == "1",
                        onClick = { onSetorChange("1") },
                        label = { Text("Setor 1") },
                    )
                    FilterChip(
                        selected = uiState.setor == "2",
                        onClick = { onSetorChange("2") },
                        label = { Text("Setor 2") },
                    )
                }

                OutlinedTextField(
                    value = uiState.diasSemAgua,
                    onValueChange = onDiasChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dias sem água") },
                    singleLine = true,
                )

                OutlinedButton(
                    onClick = pickMedia,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.pickedMedia == null) "Anexar foto ou vídeo" else "Trocar anexo")
                }

                uiState.pickedMedia?.let { media ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.Blue100),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (media.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                contentDescription = null,
                                tint = AppColors.Navy800,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    media.fileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (media.isVideo) "Vídeo anexado" else "Foto anexada",
                                    fontSize = 11.sp,
                                    color = AppColors.TextSecondary,
                                )
                            }
                            IconButton(onClick = { onMediaPicked(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remover anexo")
                            }
                        }
                    }
                }

                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting &&
                        uiState.firebaseConfigured &&
                        uiState.supabaseConfigured,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Navy800,
                        contentColor = AppColors.Card,
                    ),
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Card,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Enviando…")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar reclamação")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Privacidade", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy800)
                Text(
                    "O registro é anônimo. Use o endereço apenas para localizar o problema no município. " +
                        "Os dados ficam disponíveis no painel para acompanhamento coletivo.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AppColors.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun ReclamacaoDashboardTab(
    uiState: br.gov.caninde.transparencia.domain.ReclamacaoAguaUiState,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Painel público",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
            )
            TextButton(onClick = onRefresh, enabled = !uiState.dashboardLoading) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Atualizar")
            }
        }

        if (uiState.dashboardLoading && uiState.reclamacoes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Navy800)
            }
            return
        }

        uiState.dashboardError?.let { error ->
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Red100),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(error, Modifier.padding(12.dp), fontSize = 13.sp, color = AppColors.Red700)
            }
            Spacer(Modifier.height(8.dp))
        }

        DashboardStatsRow(uiState.stats)
        ReclamacaoMapaSection(uiState.reclamacoes)

        if (uiState.reclamacoes.isEmpty()) {
            EmptyState("Nenhuma reclamação registrada ainda.")
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.reclamacoes, key = { it.id }) { item ->
                ReclamacaoCard(item)
            }
        }
    }
}

@Composable
private fun ReclamacaoMapaSection(reclamacoes: List<ReclamacaoAgua>) {
    if (reclamacoes.isEmpty()) return
    val setor1 = reclamacoes.count { it.setor == "1" }
    val setor2 = reclamacoes.count { it.setor == "2" }
    val total = reclamacoes.size.coerceAtLeast(1)
    val topEnderecos = reclamacoes
        .groupBy { it.endereco.trim().lowercase() }
        .entries
        .sortedByDescending { it.value.size }
        .take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Distribuição por setor (rodízio SAAE)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
            )
            SetorBar("Setor 1", setor1, total, AppColors.Blue500)
            SetorBar("Setor 2", setor2, total, AppColors.Green700)
            if (topEnderecos.isNotEmpty()) {
                Text(
                    "Endereços com mais registros",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                )
                topEnderecos.forEach { (endereco, lista) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            lista.first().endereco,
                            fontSize = 12.sp,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${lista.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Blue500,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SetorBar(label: String, count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val fraction = count.toFloat() / total.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy800)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.Divider),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun DashboardStatsRow(stats: ReclamacaoAguaStats) {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Total", "${stats.total}", modifier = Modifier.weight(1f))
            MetricCard("Setor 1", "${stats.setor1}", modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Setor 2", "${stats.setor2}", modifier = Modifier.weight(1f))
            MetricCard(
                "Média dias",
                if (stats.total == 0) "0" else formatOneDecimal(stats.mediaDiasSemAgua),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReclamacaoCard(item: ReclamacaoAgua) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Setor ${item.setor}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Blue500,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.Blue100)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    formatRelativeDate(item.criadoEmMillis),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                )
            }
            Text(
                item.endereco,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
            )
            Text(
                "${item.diasSemAgua} dia(s) sem água",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
            )
            item.mediaUrl?.let { url ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { openExternalUrl(url) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (item.mediaType?.startsWith("video/") == true) Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = AppColors.Blue500,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Ver comprovante", fontSize = 12.sp, color = AppColors.Blue500)
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = AppColors.Blue500,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

private fun formatRelativeDate(millis: Long): String {
    if (millis <= 0L) return "Recente"
    val now = br.gov.caninde.transparencia.data.currentTimeMillis()
    val diffDays = ((now - millis) / 86_400_000L).coerceAtLeast(0)
    return when {
        diffDays == 0L -> "Hoje"
        diffDays == 1L -> "Ontem"
        else -> "Há $diffDays dias"
    }
}

private fun formatOneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toInt()
    val whole = tenths / 10
    val frac = kotlin.math.abs(tenths % 10)
    return "$whole,$frac"
}
