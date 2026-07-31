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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.presentation.detail.ChartBarSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamaraScreen(
    state: CamaraUiState,
    connectionState: ConnectionState,
    onRefresh: () -> Unit,
    onVereadorClick: (Parlamentar) -> Unit = {},
    onMateriaClick: (Materia) -> Unit = {},
    onSessaoClick: (Int, Sessao) -> Unit = { _, _ -> },
    onInstitucionalClick: () -> Unit = {},
    onTransparenciaLinkClick: (LinkExterno) -> Unit = {},
    onDocumentoClick: (DocumentoCamara) -> Unit = {},
    onSobreClick: () -> Unit = {},
) {
    var areaLegislativo by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var materiaFilter by remember { mutableStateOf(MATERIA_FILTER_TODAS) }
    var sessoesVisible by remember(state.sessoes) { mutableIntStateOf(CAMARA_LIST_PAGE_SIZE) }
    var materiasVisible by remember(state.materias, materiaFilter) { mutableIntStateOf(CAMARA_LIST_PAGE_SIZE) }
    val tabs = listOf("Parlamentares", "Sessões", "Matérias", "Mesa Diretora")
    val materiaFilters = remember(state.materias) { materiaFilterOptions(state.materias) }
    val filteredMaterias = remember(state.materias, materiaFilter) {
        state.materias.filter { it.matchesMateriaFilter(materiaFilter) }
    }
    LaunchedEffect(materiaFilters) {
        if (materiaFilter !in materiaFilters) materiaFilter = MATERIA_FILTER_TODAS
    }
    LaunchedEffect(selectedTab, materiaFilter) {
        materiasVisible = CAMARA_LIST_PAGE_SIZE
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) sessoesVisible = CAMARA_LIST_PAGE_SIZE
    }

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
                        Text("Câmara Municipal", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = AppColors.Blue100)
                        Text("Canindé · CE · Legislativo ${state.resumo.exercicio}",
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

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CamaraAreaFilterChip(
                        label = "Legislativo",
                        selected = areaLegislativo,
                        onClick = { areaLegislativo = true },
                    )
                    CamaraAreaFilterChip(
                        label = "Transparência",
                        selected = !areaLegislativo,
                        onClick = { areaLegislativo = false },
                    )
                }

                if (areaLegislativo) {
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
        }

        // ── Conteúdo ──────────────────────────────────────────────────────────
        if (state.isLoading) {
            ShimmerContent()
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (!areaLegislativo) {
                    item { TransparenciaLinksIntro("a Câmara Municipal") }
                    transparenciaLinksItems(
                        state.linksTransparencia.filter { it.categoria != "pessoal" },
                        "Canindé Transparente",
                        onClick = onTransparenciaLinkClick,
                    )
                    documentosTransparenciaItems(
                        state.documentosTransparencia,
                        onClick = onDocumentoClick,
                    )
                    item { Spacer(Modifier.height(80.dp)) }
                } else item {
                    // Cards resumo
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                icon = Icons.Default.People,
                                label = "Vereadores",
                                value = "${state.resumo.totalParlamentares}",
                                color = AppColors.Blue500,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.Event,
                                label = "Sessões em ${state.resumo.exercicio}",
                                value = "${state.resumo.totalSessoes2025}",
                                color = AppColors.Green500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                icon = Icons.Default.CheckCircle,
                                label = "Matérias",
                                value = "${state.resumo.totalMaterias}",
                                color = AppColors.Amber700,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.Groups,
                                label = "Mesa diretora",
                                value = "${state.mesaDiretora.size}",
                                color = AppColors.Purple700,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    LastUpdatedText(state.lastUpdated)
                    state.graficos?.camara
                        ?.firstOrNull { it.titulo.contains("tipo", ignoreCase = true) && it.labels.isNotEmpty() }
                        ?.let { series ->
                            Column(Modifier.padding(horizontal = 12.dp)) {
                                ChartBarSection(series)
                            }
                        }
                }

                if (areaLegislativo) {
                    item {
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Blue100) {
                                    Icon(Icons.Default.Info, contentDescription = null,
                                        tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = "Dados institucionais",
                            subtitle = "Contato da Câmara",
                            trailing = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null,
                                    tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                            },
                            onClick = onInstitucionalClick,
                        )
                    }

                    when (selectedTab) {
                        0 -> parlamentaresItems(state.parlamentares, onVereadorClick)
                        1 -> {
                            sessoesItems(state.sessoes.take(sessoesVisible), onSessaoClick)
                            verMaisItem(
                                total = state.sessoes.size,
                                visible = sessoesVisible,
                                pageSize = CAMARA_LIST_PAGE_SIZE,
                            ) { sessoesVisible += CAMARA_LIST_PAGE_SIZE }
                        }
                        2 -> {
                            materiasFilterItems(materiaFilters, materiaFilter) { materiaFilter = it }
                            materiasItems(filteredMaterias.take(materiasVisible), onMateriaClick)
                            verMaisItem(
                                total = filteredMaterias.size,
                                visible = materiasVisible,
                                pageSize = CAMARA_LIST_PAGE_SIZE,
                            ) { materiasVisible += CAMARA_LIST_PAGE_SIZE }
                        }
                        3 -> mesaDiretoraItems(state.mesaDiretora, state.parlamentares, onVereadorClick)
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CamaraAreaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color.White else AppColors.Blue100,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = AppColors.Blue100,
            iconColor = AppColors.Blue300,
            selectedContainerColor = AppColors.Blue500,
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.Blue300,
            selectedBorderColor = AppColors.Blue500,
        ),
    )
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 10.sp, color = AppColors.TextTertiary)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        }
    }
}

// ─── Lista de Parlamentares ───────────────────────────────────────────────────

fun LazyListScope.parlamentaresItems(parlamentares: List<Parlamentar>, onClick: (Parlamentar) -> Unit) {
    item { SectionHeader(title = "Vereadores") }
    if (parlamentares.isEmpty()) {
        item { EmptyState("Nenhum vereador encontrado") }
        return
    }
    items(parlamentares) { p ->
        ParlamentarRow(p, onClick = { onClick(p) })
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun ParlamentarRow(p: Parlamentar, onClick: (() -> Unit)? = null) {
    val stats = buildList {
        if (p.totalMaterias > 0) add("${p.totalMaterias} matérias")
        if (p.totalSessoes > 0) add("${p.totalSessoes} sessões")
    }
    ListRow(
        icon = { PersonAvatar(name = p.nome, fotoUrl = p.foto, size = 36) },
        title = p.nome,
        subtitle = listOfNotNull(
            p.partido.takeIf { it.isNotBlank() },
            p.cargo.takeIf { it.isNotBlank() },
            p.vinculo.takeIf { it.isNotBlank() },
            stats.joinToString(" · ").takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        trailing = {
            if (p.legislatura.isNotBlank()) {
                Text(
                    p.legislatura,
                    fontSize = 10.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                )
            } else if (p.cargo.isNotBlank()) {
                Text(
                    p.cargo,
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                )
            }
        },
        onClick = onClick,
    )
}

// ─── Lista de Sessões ─────────────────────────────────────────────────────────

fun LazyListScope.sessoesItems(sessoes: List<Sessao>, onClick: (Int, Sessao) -> Unit) {
    item { SectionHeader(title = "Sessões Realizadas") }
    if (sessoes.isEmpty()) {
        item { EmptyState("Nenhuma sessão encontrada") }
        return
    }
    itemsIndexed(sessoes) { index, s ->
        ListRow(
            icon = {
                IconContainer(AppColors.Green100) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null,
                        tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                }
            },
            title = s.titulo.ifEmpty { "Sessão" },
            subtitle = listOfNotNull(
                s.data.takeIf { it.isNotBlank() },
                s.modifiedAt.takeIf { it.isNotBlank() }?.substringBefore('T')?.let { "Atualizado $it" },
            ).joinToString(" · ").ifBlank { "Sessão legislativa" },
            trailing = {
                if (s.isVideoSession()) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo",
                        tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                } else if (s.url.isNotBlank()) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
                }
            },
            onClick = { onClick(index, s) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Lista de Matérias ────────────────────────────────────────────────────────

fun LazyListScope.materiasFilterItems(
    filters: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    if (filters.size <= 1) return
    item {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelected(filter) },
                    label = {
                        Text(
                            filter,
                            fontSize = 11.sp,
                            fontWeight = if (selected == filter) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Blue500,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
    }
}

fun LazyListScope.materiasItems(materias: List<Materia>, onClick: (Materia) -> Unit) {
    item { SectionHeader(title = "Matérias em Votação") }
    if (materias.isEmpty()) {
        item { EmptyState("Nenhuma matéria encontrada") }
        return
    }
    items(materias) { m ->
        val bgColor = when {
            m.tipo.contains("Projeto") -> AppColors.Blue100
            m.tipo.contains("Requerimento") -> AppColors.Amber100
            else -> AppColors.Purple100
        }
        val fgColor = when {
            m.tipo.contains("Projeto") -> AppColors.Navy800
            m.tipo.contains("Requerimento") -> AppColors.Amber700
            else -> AppColors.Purple700
        }

        ListRow(
            icon = {
                IconContainer(bgColor) {
                    Icon(Icons.Default.FilePresent, contentDescription = null,
                        tint = fgColor, modifier = Modifier.size(18.dp))
                }
            },
            title = m.titulo.ifEmpty { "Matéria" },
            subtitle = listOfNotNull(
                m.tipo.takeIf { it.isNotBlank() },
                m.dataPublicacao.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            },
            onClick = { onClick(m) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Mesa Diretora ────────────────────────────────────────────────────────────

fun LazyListScope.mesaDiretoraItems(
    mesa: List<MembroMesa>,
    parlamentares: List<Parlamentar>,
    onVereadorClick: (Parlamentar) -> Unit,
) {
    item { SectionHeader(title = "Mesa Diretora") }
    if (mesa.isEmpty()) {
        item { EmptyState("Nenhum membro encontrado") }
        return
    }
    items(mesa) { m ->
        val parlamentar = parlamentares.find { it.matchesMembroMesa(m) }
        ListRow(
            icon = {
                PersonAvatar(
                    name = m.nome,
                    fotoUrl = parlamentar?.foto.orEmpty(),
                    size = 36,
                )
            },
            title = m.nome,
            subtitle = m.cargo,
            trailing = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.Blue100)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(m.cargo.split(" ").first(), fontSize = 9.sp,
                        color = AppColors.Navy800, fontWeight = FontWeight.SemiBold)
                }
            },
            onClick = parlamentar?.let { { onVereadorClick(it) } },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

fun LazyListScope.verMaisItem(
    total: Int,
    visible: Int,
    pageSize: Int,
    onShowMore: () -> Unit,
) {
    if (total <= visible) return
    item {
        TextButton(
            onClick = onShowMore,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Text(
                "Ver mais (${(total - visible).coerceAtMost(pageSize)} de ${total - visible} restantes)",
                fontSize = 12.sp,
                color = AppColors.Blue500,
            )
        }
    }
}

fun LazyListScope.documentosTransparenciaItems(
    documentos: List<DocumentoCamara>,
    onClick: (DocumentoCamara) -> Unit,
) {
    if (documentos.isEmpty()) return
    item { SectionHeader(title = "Documentos publicados") }
    items(documentos.take(30)) { doc ->
        val icon = when (doc.categoria) {
            "licitacao" -> Icons.Default.Gavel
            "contrato" -> Icons.Default.Description
            else -> Icons.Default.Article
        }
        ListRow(
            icon = {
                IconContainer(AppColors.Amber100) {
                    Icon(icon, contentDescription = null,
                        tint = AppColors.Amber700, modifier = Modifier.size(18.dp))
                }
            },
            title = doc.titulo.ifBlank { "Documento" },
            subtitle = listOfNotNull(
                doc.categoria.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() },
                doc.data.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            },
            onClick = { if (doc.url.isNotBlank()) onClick(doc) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}
