package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import br.gov.caninde.transparencia.domain.PREFEITURA_PORTAL_BASE
import br.gov.caninde.transparencia.platform.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefeituraScreen(
    state: PrefeituraUiState,
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    onExercicioChange: (Int) -> Unit = {},
    onAguaClick: () -> Unit = {},
    onContratoClick: (Contrato) -> Unit = {},
    onLicitacaoClick: (Licitacao) -> Unit = {},
    onSecretariaClick: (Secretaria) -> Unit = {},
    onGestoresClick: () -> Unit = {},
    onInstitucionalClick: () -> Unit = {},
    onPublicacaoClick: (Publicacao) -> Unit = {},
    onObraClick: (Obra) -> Unit = {},
    onLrfClick: (LrfDocumento) -> Unit = {},
    onTransparenciaLinkClick: (LinkExterno) -> Unit = {},
    onSobreClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var contratoFilter by remember { mutableStateOf(ContratoListFilter.Todos) }
    var licitacaoFilter by remember { mutableStateOf(LicitacaoListFilter.Todas) }
    val tabs = listOf("Finanças", "Contratos", "Licitações", "Publicações", "Secretarias", "Obras/LRF", "Transparência")

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
                        IconButton(onClick = onSobreClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Sobre",
                                tint = AppColors.Blue300, modifier = Modifier.size(18.dp))
                        }
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
                item { AguaPromoCard(onClick = onAguaClick) }
                item {
                    ExercicioSelector(
                        selected = state.resumo.exercicio,
                        onSelected = onExercicioChange,
                        loading = state.isLoading,
                    )
                }
                state.resumoFinanceiro?.let { resumo ->
                    if (resumo.gtDisponivel) {
                        item {
                            FinancasHeroCard(
                                resumo = resumo,
                                onLinkClick = onTransparenciaLinkClick,
                            )
                        }
                    }
                }
                item {
                    if (state.resumoFinanceiro?.gtDisponivel != true) {
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
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Contratos publicados",
                                value = "${state.resumo.totalContratos}",
                                delta = state.resumoFinanceiro?.contratosPeriodoReferencia
                                    ?.ifBlank { "Exercício ${state.resumo.exercicio}" }
                                    ?: "Exercício ${state.resumo.exercicio}",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Licitações",
                                value = "${state.resumo.totalLicitacoes}",
                                delta = "no exercício",
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    0 -> {
                        state.resumoFinanceiro?.let { resumo ->
                            item { ResumoFinanceiroCard(resumo) }
                            financasLinksItems(
                                links = resumo.linksFinanceiros,
                                onClick = onTransparenciaLinkClick,
                            )
                            gtPortalLinksItems(
                                links = resumo.linksPortal,
                                onClick = onTransparenciaLinkClick,
                            )
                        } ?: item {
                            EmptyState("Dados financeiros indisponíveis no momento. Tente atualizar.")
                        }
                    }
                    1 -> {
                        contratosFilterItems(contratoFilter) { contratoFilter = it }
                        contratosItems(
                            state.contratos.filter { it.matchesListFilter(contratoFilter) },
                            onContratoClick,
                        )
                    }
                    2 -> {
                        licitacoesFilterItems(licitacaoFilter) { licitacaoFilter = it }
                        licitacoesItems(
                            state.licitacoes.filter { it.matchesListFilter(licitacaoFilter) },
                            onLicitacaoClick,
                        )
                    }
                    3 -> publicacoesItems(state.publicacoes, state.diarios, state.diariosOficiais, onPublicacaoClick)
                    4 -> secretariasItems(state.secretarias, onSecretariaClick)
                    5 -> obrasLrfItems(state.obras, state.lrf, onObraClick, onLrfClick)
                    6 -> {
                        item { TransparenciaDestaquesCard(state.linksTransparencia) }
                        item { TransparenciaLinksIntro("a Prefeitura") }
                        transparenciaLinksItems(state.linksTransparencia, onClick = onTransparenciaLinkClick)
                        state.resumoFinanceiro?.linksPortal?.let { portalLinks ->
                            if (portalLinks.isNotEmpty()) {
                                gtPortalLinksItems(portalLinks, onClick = onTransparenciaLinkClick)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Lista de Contratos ───────────────────────────────────────────────────────

fun LazyListScope.contratosFilterItems(
    selected: ContratoListFilter,
    onSelected: (ContratoListFilter) -> Unit,
) {
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContratoListFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelected(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                )
            }
        }
    }
}

fun LazyListScope.licitacoesFilterItems(
    selected: LicitacaoListFilter,
    onSelected: (LicitacaoListFilter) -> Unit,
) {
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LicitacaoListFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelected(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                )
            }
        }
    }
}

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

fun LazyListScope.publicacoesItems(
    publicacoes: List<Publicacao>,
    diarios: List<DiarioOficial>,
    diariosFallback: List<String>,
    onClick: (Publicacao) -> Unit = {},
) {
    if (diarios.isNotEmpty()) {
        item { SectionHeader(title = "Diário Oficial", action = "") }
        items(diarios) { d ->
            ListRow(
                icon = {
                    IconContainer(AppColors.Amber100) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                            tint = AppColors.Amber700, modifier = Modifier.size(18.dp))
                    }
                },
                title = d.titulo.ifBlank { "Diário oficial" },
                subtitle = listOfNotNull(
                    d.numero.takeIf { it.isNotBlank() },
                    d.data.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir PDF",
                        tint = AppColors.Blue500, modifier = Modifier.size(16.dp))
                },
                onClick = {
                    val url = d.pdfUrl.ifBlank { d.url }
                    if (url.isNotBlank()) openExternalUrl(url)
                },
            )
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
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
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                    if (p.id.isNotBlank() || p.url.isNotBlank()) onClick(p)
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

// ─── Obras e LRF ──────────────────────────────────────────────────────────────

fun LazyListScope.obrasLrfItems(
    obras: List<Obra>,
    lrf: List<LrfDocumento>,
    onObraClick: (Obra) -> Unit = {},
    onLrfClick: (LrfDocumento) -> Unit = {},
) {
    if (obras.isNotEmpty()) {
        item { SectionHeader("Obras (${obras.size})") }
        obras.take(20).forEach { obra ->
            item {
                ListRow(
                    icon = {
                        IconContainer(AppColors.Amber100) {
                            Icon(Icons.Default.Construction, null, tint = AppColors.Amber700, modifier = Modifier.size(18.dp))
                        }
                    },
                    title = obra.titulo,
                    subtitle = listOfNotNull(
                        obra.secretaria.takeIf { it.isNotBlank() },
                        obra.valor.takeIf { it.isNotBlank() },
                        obra.situacao.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    trailing = {
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                    },
                    onClick = {
                        val obraId = obra.id.ifBlank { obra.titulo }
                        if (obraId.isNotBlank()) onObraClick(obra)
                    },
                )
                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
    if (lrf.isNotEmpty()) {
        item { SectionHeader("LRF — Responsabilidade fiscal (${lrf.size})") }
        lrf.take(20).forEach { doc ->
            item {
                ListRow(
                    icon = {
                        IconContainer(AppColors.Blue100) {
                            Icon(Icons.Default.Description, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                        }
                    },
                    title = doc.titulo,
                    subtitle = listOfNotNull(
                        doc.tipo.takeIf { it.isNotBlank() },
                        doc.exercicio.takeIf { it.isNotBlank() },
                        doc.data.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    trailing = {
                        if (doc.url.isNotBlank()) {
                            Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        val docId = doc.id.ifBlank { doc.titulo }
                        if (docId.isNotBlank()) onLrfClick(doc)
                        else doc.url.takeIf { it.isNotBlank() }?.let { openExternalUrl(it) }
                    },
                )
                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
    if (obras.isEmpty() && lrf.isEmpty()) {
        item { EmptyState("Nenhuma obra ou documento LRF no exercício atual.") }
    }
}

@Composable
fun FinancasHeroCard(
    resumo: ResumoFinanceiroPortal,
    onLinkClick: (LinkExterno) -> Unit = {},
) {
    val progress = remember(resumo.percentualArrecadacao) {
        resumo.percentualArrecadacao
            .replace("%", "")
            .replace(".", "")
            .replace(",", ".")
            .toFloatOrNull()
            ?.div(100f)
            ?.coerceIn(0f, 1f)
            ?: 0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy800),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Execução orçamentária",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Blue100,
                    )
                    Text(
                        "Exercício ${resumo.exercicio} · Dados oficiais abertos",
                        fontSize = 11.sp,
                        color = AppColors.Blue300,
                    )
                    val metaDatas = formatFinanceMeta(
                        resumo.periodoReferencia,
                        resumo.dadosAtualizadosEm,
                        resumo.consultadoEm,
                    )
                    if (metaDatas.isNotBlank()) {
                        Text(
                            metaDatas,
                            fontSize = 10.sp,
                            color = AppColors.Blue300.copy(alpha = 0.85f),
                            lineHeight = 14.sp,
                        )
                    }
                }
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = AppColors.Blue300,
                    modifier = Modifier.size(28.dp),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (resumo.receitaArrecadada.isNotBlank()) {
                    Column(Modifier.weight(1f)) {
                        Text("Receita arrecadada", fontSize = 10.sp, color = AppColors.Blue300)
                        Text(
                            resumo.receitaArrecadada,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Green100,
                            lineHeight = 20.sp,
                        )
                    }
                }
                if (resumo.despesaPaga.isNotBlank()) {
                    Column(Modifier.weight(1f)) {
                        Text("Despesa paga", fontSize = 10.sp, color = AppColors.Blue300)
                        Text(
                            resumo.despesaPaga,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Blue100,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            if (resumo.percentualArrecadacao.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AppColors.Green100,
                        trackColor = AppColors.Navy700,
                    )
                    Text(
                        "${resumo.percentualArrecadacao} do orçamento previsto (${resumo.receitaPrevista})",
                        fontSize = 11.sp,
                        color = AppColors.Blue300,
                        lineHeight = 15.sp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resumo.gtReceitasPainelUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { openExternalUrl(resumo.gtReceitasPainelUrl) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppColors.Navy700,
                            contentColor = AppColors.Blue100,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Receitas", fontSize = 12.sp)
                    }
                }
                if (resumo.gtDespesasPainelUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { openExternalUrl(resumo.gtDespesasPainelUrl) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AppColors.Navy700,
                            contentColor = AppColors.Blue100,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.TrendingDown, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Despesas", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun LazyListScope.financasLinksItems(
    links: List<LinkExterno>,
    onClick: (LinkExterno) -> Unit = {},
) {
    val receitas = links.filter { it.categoria == "receita" || (it.categoria == "financeiro" && it.titulo.contains("receita", ignoreCase = true)) }
    val despesas = links.filter { it.categoria == "despesa" || (it.categoria == "financeiro" && it.titulo.contains("despesa", ignoreCase = true)) }

    if (receitas.isNotEmpty()) {
        item { SectionHeader(title = "Consultas de receita (GT)", action = "") }
        items(receitas) { link ->
            TransparenciaLinkRow(link, onClick)
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
    if (despesas.isNotEmpty()) {
        item { SectionHeader(title = "Consultas de despesa (GT)", action = "") }
        items(despesas) { link ->
            TransparenciaLinkRow(link, onClick)
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

fun LazyListScope.gtPortalLinksItems(
    links: List<LinkExterno>,
    onClick: (LinkExterno) -> Unit = {},
) {
    if (links.isEmpty()) return

    val grupos = listOf(
        "portal" to "Portal oficial",
        "financeiro" to "Finanças e recursos",
        "despesa" to "Despesas e pagamentos",
        "compras" to "Licitações, contratos e convênios",
        "obras" to "Obras e projetos",
        "emendas" to "Emendas parlamentares",
        "pessoal" to "Pessoal e folha",
        "dadosabertos" to "Dados abertos",
    )

    for ((categoria, tituloSecao) in grupos) {
        val secao = links.filter { it.categoria == categoria }
        if (secao.isEmpty()) continue
        item { SectionHeader(title = tituloSecao, action = "") }
        items(secao, key = { it.url.ifBlank { it.titulo } }) { link ->
            TransparenciaLinkRow(link, onClick)
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun ResumoFinanceiroCard(resumo: ResumoFinanceiroPortal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Resumo financeiro · ${resumo.exercicio}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
            )
            val metaDatas = formatFinanceMeta(
                resumo.periodoReferencia,
                resumo.dadosAtualizadosEm,
                resumo.consultadoEm,
            )
            if (metaDatas.isNotBlank()) {
                Text(
                    metaDatas,
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 15.sp,
                )
            }
            if (resumo.gtDisponivel && (resumo.receitaArrecadada.isNotBlank() || resumo.despesaPaga.isNotBlank())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (resumo.receitaArrecadada.isNotBlank()) {
                        MetricCard(
                            "Receita arrecadada",
                            resumo.receitaArrecadada,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (resumo.despesaPaga.isNotBlank()) {
                        MetricCard(
                            "Despesa paga",
                            resumo.despesaPaga,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (resumo.receitaPrevista.isNotBlank()) {
                    Text(
                        "Previsto no orçamento: ${resumo.receitaPrevista}",
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    "Contratos (portal)",
                    resumo.totalContratosValor.ifBlank { "${resumo.totalContratos}" },
                    delta = resumo.contratosPeriodoReferencia.ifBlank { "Exercício ${resumo.exercicio}" },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    "Licitações abertas",
                    "${resumo.licitacoesAbertas}",
                    delta = "Exercício ${resumo.exercicio}",
                    modifier = Modifier.weight(1f),
                )
            }
            if (resumo.topFornecedores.isNotEmpty()) {
                Text(
                    "Maiores fornecedores (empresas)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Navy800,
                )
                resumo.topFornecedores.take(5).forEach { fornecedor ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                fornecedor.nome,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = AppColors.TextPrimary,
                                maxLines = 2,
                            )
                            if (fornecedor.cnpj.isNotBlank()) {
                                Text(fornecedor.cnpj, fontSize = 10.sp, color = AppColors.TextTertiary)
                            }
                        }
                        Text(
                            fornecedor.valor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Navy800,
                        )
                    }
                }
            }
            if (resumo.aviso.isNotBlank()) {
                Text(resumo.aviso, fontSize = 11.sp, lineHeight = 16.sp, color = AppColors.TextTertiary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resumo.gtReceitasUrl.isNotBlank()) {
                    TextButton(onClick = { openExternalUrl(resumo.gtReceitasUrl) }) {
                        Text("Receitas no GT", fontSize = 12.sp)
                    }
                }
                if (resumo.gtDespesasUrl.isNotBlank()) {
                    TextButton(onClick = { openExternalUrl(resumo.gtDespesasUrl) }) {
                        Text("Despesas no GT", fontSize = 12.sp)
                    }
                }
                if (resumo.gtDadosAbertosUrl.isNotBlank()) {
                    TextButton(onClick = { openExternalUrl(resumo.gtDadosAbertosUrl) }) {
                        Text("Exportar dados", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Estados auxiliares ───────────────────────────────────────────────────────

@Composable
fun AguaPromoCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Blue100),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconContainer(AppColors.Navy800.copy(alpha = 0.12f)) {
                Icon(Icons.Default.WaterDrop, null, tint = AppColors.Navy800, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "SAAE — água, esgoto e transparência",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Navy800,
                )
                Text(
                    "Folha, contratos, licitações e registro de falta de água",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary,
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Navy800, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ExercicioSelector(
    selected: Int,
    onSelected: (Int) -> Unit,
    loading: Boolean,
) {
    val options = remember { exercicioYearOptions() }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Exercício:", fontSize = 11.sp, color = AppColors.TextSecondary)
        options.forEach { year ->
            FilterChip(
                selected = selected == year,
                onClick = { if (!loading && selected != year) onSelected(year) },
                enabled = !loading,
                label = { Text("$year", fontSize = 11.sp) },
            )
        }
    }
}

@Composable
fun TransparenciaDestaquesCard(links: List<LinkExterno>) {
    val emendas = links.firstOrNull { it.categoria == "emendas" }
    val convenios = links.firstOrNull { it.categoria == "compras" && it.titulo.contains("Convênio", ignoreCase = true) }
        ?: links.firstOrNull { it.titulo.contains("Convênio", ignoreCase = true) }
    if (emendas == null && convenios == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Emendas e convênios",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Navy800,
            )
            emendas?.let {
                Text(
                    "Emendas parlamentares repassadas ao município — consulta detalhada na plataforma oficial.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                )
            }
            convenios?.let {
                Text(
                    "Convênios firmados pela Prefeitura — valores, parceiros e situação conforme publicação oficial.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}

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
