package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.platform.openExternalUrl
import br.gov.caninde.transparencia.domain.PREFEITURA_PORTAL_BASE
import br.gov.caninde.transparencia.domain.resolveAbsoluteUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefeituraScreen(
    state: PrefeituraUiState,
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    onContratoClick: (Contrato) -> Unit = {},
    onLicitacaoClick: (Licitacao) -> Unit = {},
    onSecretariaClick: (Secretaria) -> Unit = {},
    onGestoresClick: () -> Unit = {},
    onInstitucionalClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Contratos", "Licitações", "Publicações", "Secretarias", "Transparência")

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(Modifier.fillMaxWidth().background(AppColors.Navy800)) {
            Column {
                ConnectionBanner(connectionState)
                DataStatusBanner(error = state.error)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Prefeitura de Canindé", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.Blue100)
                        Text("Ceará · Exercício ${state.resumo.exercicio}",
                            fontSize = 11.sp, color = AppColors.Blue300)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveBadge()
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar",
                                tint = AppColors.Blue300, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AppColors.Navy800,
                    contentColor = AppColors.Blue100,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AppColors.Blue500
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { i, t ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = {
                                Text(t, fontSize = 12.sp,
                                    color = if (selectedTab == i) AppColors.Blue100 else AppColors.Blue300)
                            }
                        )
                    }
                }
            }
        }

        // ── Métricas ─────────────────────────────────────────────────────────
        if (state.isLoading) {
            ShimmerContent()
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            label = "Contratos",
                            value = "${state.resumo.totalContratos}",
                            delta = "Exercício ${state.resumo.exercicio}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Licitações",
                            value = "${state.resumo.totalLicitacoes}",
                            delta = "no exercício",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (state.resumo.totalPublicacoes > 0) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Publicações",
                                value = "${state.resumo.totalPublicacoes}",
                                delta = "diário e atos",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Secretarias",
                                value = "${state.secretarias.size}",
                                delta = "com gestor",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    LastUpdatedText(state.lastUpdated)
                }

                item {
                    ListRow(
                        icon = {
                            IconContainer(AppColors.Purple100) {
                                Icon(Icons.Default.People, contentDescription = null,
                                    tint = AppColors.Purple700, modifier = Modifier.size(18.dp))
                            }
                        },
                        title = "Prefeito e Vice",
                        subtitle = "Gestão municipal",
                        trailing = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null,
                                tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                        },
                        onClick = onGestoresClick,
                    )
                    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Conteúdo por tab
                when (selectedTab) {
                    0 -> contratosItems(state.contratos, onContratoClick)
                    1 -> licitacoesItems(state.licitacoes, onLicitacaoClick)
                    2 -> publicacoesItems(state.publicacoes, state.diariosOficiais)
                    3 -> secretariasItems(state.secretarias, onSecretariaClick)
                    4 -> {
                        item { TransparenciaLinksIntro("a Prefeitura") }
                        transparenciaLinksItems(state.linksTransparencia)
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Lista de Contratos ───────────────────────────────────────────────────────

fun LazyListScope.contratosItems(contratos: List<Contrato>, onClick: (Contrato) -> Unit) {
    item { SectionHeader(title = "Contratos Recentes") }
    if (contratos.isEmpty()) {
        item { EmptyState("Nenhum contrato encontrado") }
        return
    }
    items(contratos) { c ->
        ContratosRow(c, onClick = { onClick(c) })
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun ContratosRow(c: Contrato, onClick: (() -> Unit)? = null) {
    ListRow(
        icon = {
            IconContainer(AppColors.Blue100) {
                Icon(Icons.Default.Description, contentDescription = null,
                    tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
            }
        },
        title = c.objeto.ifEmpty { "Contrato ${c.numero}" },
        subtitle = listOfNotNull(
            c.empresa.takeIf { it.isNotBlank() },
            c.secretaria.takeIf { it.isNotBlank() },
            c.data.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifEmpty { c.numero },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(c.valor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary)
                Spacer(Modifier.height(3.dp))
                StatusBadge("Ativo")
            }
        },
        onClick = onClick,
    )
}

// ─── Lista de Licitações ──────────────────────────────────────────────────────

fun LazyListScope.licitacoesItems(licitacoes: List<Licitacao>, onClick: (Licitacao) -> Unit) {
    item { SectionHeader(title = "Licitações") }
    if (licitacoes.isEmpty()) {
        item { EmptyState("Nenhuma licitação encontrada") }
        return
    }
    items(licitacoes) { l ->
        ListRow(
            icon = {
                IconContainer(AppColors.Amber100) {
                    Icon(Icons.Default.Gavel, contentDescription = null,
                        tint = AppColors.Amber700, modifier = Modifier.size(18.dp))
                }
            },
            title = l.objeto.ifEmpty { "Licitação ${l.numero}" },
            subtitle = listOfNotNull(
                l.modalidade.takeIf { it.isNotBlank() },
                l.dataAbertura.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifEmpty { l.numero },
            trailing = { StatusBadge(l.situacao.ifEmpty { "Aberta" }) },
            onClick = { onClick(l) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Publicações / Diário ─────────────────────────────────────────────────────

fun LazyListScope.publicacoesItems(publicacoes: List<Publicacao>, diariosFallback: List<String>) {
    item { SectionHeader(title = "Publicações oficiais", action = "") }
    if (publicacoes.isNotEmpty()) {
        items(publicacoes) { p ->
            ListRow(
                icon = {
                    IconContainer(AppColors.Green100) {
                        Icon(Icons.Default.Article, contentDescription = null,
                            tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                    }
                },
                title = p.titulo,
                subtitle = listOfNotNull(
                    p.tipo.takeIf { it.isNotBlank() },
                    p.data.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                trailing = {
                    Icon(Icons.Default.OpenInNew, contentDescription = null,
                        tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                    if (p.url.isNotBlank()) {
                        openExternalUrl(resolveAbsoluteUrl(p.url, PREFEITURA_PORTAL_BASE))
                    }
                },
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    if (diariosFallback.isEmpty()) {
        item { EmptyState("Nenhuma publicação encontrada") }
        return
    }
    items(diariosFallback.size) { index ->
        val d = diariosFallback[index]
        ListRow(
            icon = {
                IconContainer(AppColors.Green100) {
                    Icon(Icons.Default.Article, contentDescription = null,
                        tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                }
            },
            title = d.take(120),
            subtitle = "Diário oficial",
            trailing = {},
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Secretarias ─────────────────────────────────────────────────────────────

fun LazyListScope.secretariasItems(secretarias: List<Secretaria>, onClick: (Secretaria) -> Unit) {
    item { SectionHeader(title = "Secretarias Municipais") }
    if (secretarias.isEmpty()) {
        item { EmptyState("Nenhuma secretaria encontrada") }
        return
    }
    items(secretarias) { s ->
        ListRow(
            icon = {
                IconContainer(AppColors.Blue100) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null,
                        tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                }
            },
            title = s.nome,
            subtitle = listOfNotNull(
                s.secretario.takeIf { it.isNotBlank() },
                s.contato.email.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            },
            onClick = { onClick(s) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Estados auxiliares ───────────────────────────────────────────────────────

@Composable
fun EmptyState(msg: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(msg, fontSize = 13.sp, color = AppColors.TextTertiary)
    }
}

@Composable
fun ShimmerContent() {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(Modifier.weight(1f).height(72.dp))
            ShimmerBox(Modifier.weight(1f).height(72.dp))
        }
        repeat(5) {
            ShimmerBox(Modifier.fillMaxWidth().height(56.dp))
        }
    }
}

// Workaround para tabIndicatorOffset
@Composable
fun Modifier.tabIndicatorOffset(tabPosition: TabPosition): Modifier =
    this.wrapContentSize(Alignment.BottomStart)
        .offset(x = tabPosition.left)
        .width(tabPosition.width)
