package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.platform.openExternalUrl

@Composable
fun FolhaPagamentoScreen(
    prefeituraState: PrefeituraUiState,
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    onSobreClick: () -> Unit = {},
) {
    val folha = prefeituraState.folhaPagamento
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Por secretaria", "Por mês")

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(Modifier.fillMaxWidth().background(AppColors.Navy800)) {
            Column {
                ConnectionBanner(connectionState)
                DataStatusBanner(error = prefeituraState.error)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Folha de pagamento",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Blue100,
                        )
                        Text(
                            "Totais por secretaria · Exercício ${folha?.exercicio ?: prefeituraState.resumo.exercicio}",
                            fontSize = 11.sp,
                            color = AppColors.Blue300,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ConnectionStatusBadge(connectionState, onRefresh)
                        IconButton(onClick = onSobreClick) {
                            Icon(Icons.Default.Info, contentDescription = "Sobre", tint = AppColors.Blue100)
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = AppColors.Blue100)
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

        if (prefeituraState.isLoading && folha == null) {
            ShimmerContent()
            return@Column
        }

        if (folha == null || (folha.porSetor.isEmpty() && folha.competencias.isEmpty())) {
            EmptyState(
                "Dados de folha ainda não carregados. Atualize ou consulte o portal oficial.",
            )
            FolhaPortalLinks(folha)
            return@Column
        }

        folha.avisoPrivacidade.takeIf { it.isNotBlank() }?.let { aviso ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Blue100),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Shield, null, tint = AppColors.Navy800, modifier = Modifier.size(20.dp))
                    Text(aviso, fontSize = 11.sp, lineHeight = 16.sp, color = AppColors.Navy800)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricCard(
                        label = "Total pago (setores)",
                        value = folha.totalPagoSetores.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Secretarias",
                        value = "${folha.porSetor.size}",
                        modifier = Modifier.weight(1f),
                    )
                }
                LastUpdatedText(prefeituraState.lastUpdated)
            }

            when (tab) {
                0 -> {
                    item { SectionHeader("Pagamentos de folha por órgão (${folha.porSetor.size})") }
                    if (folha.porSetor.isEmpty()) {
                        item { EmptyState("Nenhum pagamento de folha encontrado no exercício.") }
                    } else {
                        val maxValor = folha.porSetor.maxOfOrNull { it.totalPagoNumerico }?.coerceAtLeast(1.0) ?: 1.0
                        items(folha.porSetor, key = { it.secretaria }) { setor ->
                            FolhaSetorRow(setor, maxValor)
                            HorizontalDivider(
                                color = AppColors.Divider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
                1 -> {
                    item { SectionHeader("Totais mensais (${folha.competencias.size})") }
                    if (folha.competencias.isEmpty()) {
                        item { EmptyState("Competências mensais indisponíveis no portal.") }
                    } else {
                        items(folha.competencias, key = { it.competencia }) { comp ->
                            FolhaCompetenciaRow(comp)
                            HorizontalDivider(
                                color = AppColors.Divider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }

            item { FolhaPortalLinks(folha) }
        }
    }
}

@Composable
private fun FolhaSetorRow(setor: FolhaSetorResumo, maxValor: Double) {
    val fraction = (setor.totalPagoNumerico / maxValor).toFloat().coerceIn(0f, 1f)
    ListRow(
        icon = {
            IconContainer(AppColors.Purple100) {
                Icon(Icons.Default.AccountBalance, null, tint = AppColors.Purple700, modifier = Modifier.size(18.dp))
            }
        },
        title = setor.secretaria,
        subtitle = buildString {
            append(setor.totalPago)
            if (setor.quantidadePagamentos > 0) append(" · ${setor.quantidadePagamentos} pagamento(s)")
        },
        trailing = {},
    )
    ProgressRow(
        label = setor.codigoOrgao.ifBlank { setor.secretaria.take(28) },
        pct = fraction,
        color = AppColors.Blue500,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun FolhaCompetenciaRow(comp: FolhaCompetencia) {
    ListRow(
        icon = {
            IconContainer(AppColors.Green100) {
                Icon(Icons.Default.CalendarMonth, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp))
            }
        },
        title = comp.competencia,
        subtitle = listOfNotNull(
            comp.proventos.takeIf { it.isNotBlank() }?.let { "Proventos: $it" },
            comp.descontos.takeIf { it.isNotBlank() }?.let { "Descontos: $it" },
            comp.liquido.takeIf { it.isNotBlank() }?.let { "Líquido: $it" },
        ).joinToString(" · "),
        trailing = {},
    )
}

@Composable
private fun FolhaPortalLinks(folha: FolhaPagamentoResumo?) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Consulta nominal no portal oficial",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Navy800,
        )
        Text(
            "Detalhes com nomes de servidores ficam apenas no site da Prefeitura.",
            fontSize = 11.sp,
            color = AppColors.TextSecondary,
        )
        val folhaUrl = folha?.fonteUrl?.takeIf { it.isNotBlank() }
            ?: "https://www.caninde.ce.gov.br/folhadepagamento.php"
        OutlinedButton(onClick = { openExternalUrl(folhaUrl) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Folha de pagamento (Prefeitura)", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        folha?.fontePagamentosUrl?.takeIf { it.isNotBlank() }?.let { pagUrl ->
            OutlinedButton(onClick = { openExternalUrl(pagUrl) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pagamentos por órgão", fontSize = 12.sp)
            }
        }
    }
}
