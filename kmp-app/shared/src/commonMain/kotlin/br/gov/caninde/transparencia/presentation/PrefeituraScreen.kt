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
import androidx.compose.ui.text.style.TextOverflow
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
                        ConnectionStatusBadge(connectionState, onRefresh)
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
                    val gestoresResumo = formatGestoresResumo(state.gestores)
                    ListRow(
                        icon = {
                            IconContainer(AppColors.Purple100) {
                                Icon(Icons.Default.People, contentDescription = null,
                                    tint = AppColors.Purple700, modifier = Modifier.size(18.dp))
                            }
                        },
                        title = "Prefeito e Vice",
                        subtitle = gestoresResumo.ifBlank { "Gestão municipal" },
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
    val info = c.displayInfo()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconContainer(AppColors.Blue100) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = AppColors.Navy800,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = info.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (info.descricao.isNotBlank()) {
                Text(
                    text = info.descricao,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
            }
            if (info.meta.isNotBlank()) {
                Text(
                    text = info.meta,
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (info.valor.isNotBlank()) {
                Text(
                    text = info.valor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Green700,
                )
            }
            StatusBadge(info.situacao)
        }
    }
}

// ─── Lista de Licitações ──────────────────────────────────────────────────────

fun LazyListScope.licitacoesItems(licitacoes: List<Licitacao>, onClick: (Licitacao) -> Unit) {
    item { SectionHeader(title = "Licitações") }
    if (licitacoes.isEmpty()) {
        item { EmptyState("Nenhuma licitação encontrada") }
        return
    }
    items(licitacoes) { l ->
        LicitacoesRow(l, onClick = { onClick(l) })
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun LicitacoesRow(l: Licitacao, onClick: (() -> Unit)? = null) {
    val info = l.displayInfo()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconContainer(AppColors.Amber100) {
            Icon(
                Icons.Default.Gavel,
                contentDescription = null,
                tint = AppColors.Amber700,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = info.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            if (info.descricao.isNotBlank()) {
                Text(
                    text = info.descricao,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (info.meta.isNotBlank()) {
                Text(
                    text = "Abertura: ${info.meta}",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StatusBadge(
                text = info.situacao,
                maxLines = if (info.situacao.length > 28) 2 else 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
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
        val resumo = s.resumoFinanceiro
        val stats = buildList {
            if (resumo.totalProjetosAndamento > 0) add("${resumo.totalProjetosAndamento} em andamento")
            if (resumo.totalContratos > 0) add("${resumo.totalContratos} contratos")
            if (resumo.totalLicitacoes > 0) add("${resumo.totalLicitacoes} licitações")
            if (resumo.totalGastos.isNotBlank()) add(resumo.totalGastos)
        }
        ListRow(
            icon = {
                IconContainer(AppColors.Blue100) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null,
                        tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                }
            },
            title = s.nome,
            subtitle = listOfNotNull(
                s.secretario.takeIf { it.isNotBlank() }?.let { sec ->
                    if (s.cargoGestor.isNotBlank()) "$sec · ${s.cargoGestor}" else sec
                },
                stats.joinToString(" · ").takeIf { it.isNotBlank() },
                s.contato.email.takeIf { it.isNotBlank() },
            ).joinToString("\n"),
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
