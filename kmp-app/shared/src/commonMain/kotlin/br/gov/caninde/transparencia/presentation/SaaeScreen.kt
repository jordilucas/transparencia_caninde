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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.platform.openExternalUrl

@Composable
fun SaaeScreen(
    prefeituraState: PrefeituraUiState,
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    onExercicioChange: (Int) -> Unit = {},
    onContratoClick: (Contrato) -> Unit = {},
    onLicitacaoClick: (Licitacao) -> Unit = {},
    onSobreClick: () -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transparência", "Reclamações")
    val exercicio = prefeituraState.resumo.exercicio

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
                            "SAAE — Água e Esgoto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Blue100,
                        )
                        Text(
                            "Serviço Autônomo · Exercício $exercicio · Canindé/CE",
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

        when (tab) {
            0 -> SaaeTransparenciaContent(
                prefeituraState = prefeituraState,
                onExercicioChange = onExercicioChange,
                onContratoClick = onContratoClick,
                onLicitacaoClick = onLicitacaoClick,
            )
            1 -> ReclamacaoAguaScreen(
                embedded = true,
                onSobreClick = onSobreClick,
            )
        }
    }
}

@Composable
private fun SaaeTransparenciaContent(
    prefeituraState: PrefeituraUiState,
    onExercicioChange: (Int) -> Unit,
    onContratoClick: (Contrato) -> Unit,
    onLicitacaoClick: (Licitacao) -> Unit,
) {
    val saae = prefeituraState.saaeResumo

    if (prefeituraState.isLoading && saae == null) {
        ShimmerContent()
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            ExercicioSelector(
                selected = prefeituraState.resumo.exercicio,
                onSelected = onExercicioChange,
                loading = prefeituraState.isLoading,
            )
        }

        item { SaaeHeroCard(saae) }

        saae?.aviso?.takeIf { it.isNotBlank() }?.let { aviso ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Info, null, tint = AppColors.Blue500, modifier = Modifier.size(18.dp))
                        Text(aviso, fontSize = 11.sp, lineHeight = 16.sp, color = AppColors.TextSecondary)
                    }
                }
            }
        }

        if (saae == null || !saae.disponivel) {
            item {
                EmptyState(
                    "Dados do SAAE ainda não carregados. Atualize ou consulte a plataforma oficial.",
                )
            }
        } else {
            if (saae.linhasFinanceiras.isNotEmpty()) {
                item { SectionHeader("Movimentação financeira") }
                items(saae.linhasFinanceiras, key = { it.descricao }) { linha ->
                    SaaeLinhaRow(linha)
                    HorizontalDivider(
                        color = AppColors.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (saae.contratos.isNotEmpty()) {
                item { SectionHeader("Contratos relacionados (${saae.quantidadeContratos})") }
                items(saae.contratos, key = { it.numero + it.id }) { contrato ->
                    ListRow(
                        icon = {
                            IconContainer(AppColors.Blue100) {
                                Icon(Icons.Default.Description, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                            }
                        },
                        title = contrato.numero.ifBlank { "Contrato" },
                        subtitle = listOfNotNull(
                            contrato.valor.takeIf { it.isNotBlank() },
                            contrato.empresa.takeIf { it.isNotBlank() },
                            contrato.objeto.take(80).takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                        },
                        onClick = { onContratoClick(contrato) },
                    )
                    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            if (saae.licitacoes.isNotEmpty()) {
                item { SectionHeader("Licitações relacionadas (${saae.quantidadeLicitacoes})") }
                items(saae.licitacoes, key = { it.numero + it.id }) { lic ->
                    ListRow(
                        icon = {
                            IconContainer(AppColors.Green100) {
                                Icon(Icons.Default.Gavel, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                            }
                        },
                        title = lic.numero.ifBlank { "Licitação" },
                        subtitle = listOfNotNull(
                            lic.modalidade.takeIf { it.isNotBlank() },
                            lic.situacao.takeIf { it.isNotBlank() },
                            lic.objeto.take(80).takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        trailing = {
                            Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                        },
                        onClick = { onLicitacaoClick(lic) },
                    )
                    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            if (saae.links.isNotEmpty()) {
                item { SectionHeader("Consultas oficiais", action = "") }
                items(saae.links, key = { it.url }) { link ->
                    TransparenciaLinkRow(link) { openExternalUrl(link.url) }
                    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item { LastUpdatedText(prefeituraState.lastUpdated) }
        }
    }
}

@Composable
private fun SaaeHeroCard(saae: SaaeResumo?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Blue500),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        saae?.titulo?.ifBlank { "SAAE — Água e Esgoto" } ?: "SAAE — Água e Esgoto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Blue100,
                    )
                    Text(
                        buildString {
                            append("Órgão ${saae?.codigoOrgao?.ifBlank { "044" } ?: "044"}")
                            saae?.dadosAtualizadosEm?.takeIf { it.isNotBlank() }?.let { append(" · Atualizado $it") }
                        },
                        fontSize = 11.sp,
                        color = AppColors.Blue100.copy(alpha = 0.85f),
                    )
                }
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = AppColors.Blue100,
                    modifier = Modifier.size(32.dp),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!saae?.folhaPagamento.isNullOrBlank()) {
                    SaaeMetricChip("Folha de pessoal", saae.folhaPagamento, Modifier.weight(1f))
                }
                if (!saae?.totalDespesasGt.isNullOrBlank()) {
                    SaaeMetricChip("Despesas", saae.totalDespesasGt, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SaaeMetricChip(
                    "Contratos",
                    saae?.totalContratos?.ifBlank { "${saae.quantidadeContratos}" } ?: "—",
                    Modifier.weight(1f),
                )
                SaaeMetricChip(
                    "Licitações",
                    "${saae?.quantidadeLicitacoes ?: 0}",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SaaeMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.Navy800.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 10.sp, color = AppColors.Blue100.copy(alpha = 0.8f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Blue100, lineHeight = 18.sp)
    }
}

@Composable
private fun SaaeLinhaRow(linha: SaaeLinhaFinanceira) {
    val icon = when (linha.tipo) {
        "folha" -> Icons.Default.People
        else -> Icons.Default.Payments
    }
    val container = when (linha.tipo) {
        "folha" -> AppColors.Purple100 to AppColors.Purple700
        else -> AppColors.Amber100 to AppColors.Amber700
    }
    ListRow(
        icon = {
            IconContainer(container.first) {
                Icon(icon, null, tint = container.second, modifier = Modifier.size(18.dp))
            }
        },
        title = linha.descricao,
        subtitle = when (linha.tipo) {
            "folha" -> "Folha de pagamento"
            else -> "Despesa / fornecedor"
        },
        trailing = {
            Text(linha.valor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy800)
        },
    )
}
