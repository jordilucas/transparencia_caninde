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
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Parlamentares", "Sessões", "Matérias", "Mesa Diretora")

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
                        Text("Canindé · CE · Legislativo 2025",
                            fontSize = 11.sp, color = AppColors.Blue300)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSessionActive = state.sessoes.isNotEmpty()
                        if (isSessionActive) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AppColors.Green100)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Sessão ativa", fontSize = 10.sp, 
                                    color = AppColors.Green700, fontWeight = FontWeight.SemiBold)
                            }
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

        // ── Conteúdo ──────────────────────────────────────────────────────────
        if (state.isLoading) {
            ShimmerContent()
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
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
                                label = "Sessões em 2025",
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
                }

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
                    1 -> sessoesItems(state.sessoes, onSessaoClick)
                    2 -> materiasItems(state.materias, onMateriaClick)
                    3 -> mesaDiretoraItems(state.mesaDiretora)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
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
    ListRow(
        icon = { InitialAvatar(p.nome, size = 36) },
        title = p.nome,
        subtitle = listOfNotNull(
            p.partido.takeIf { it.isNotBlank() },
            p.cargo.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        trailing = {
            if (p.cargo.isNotBlank()) {
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
            subtitle = s.data,
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
            },
            onClick = { onClick(index, s) },
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ─── Lista de Matérias ────────────────────────────────────────────────────────

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
            subtitle = m.tipo,
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

fun LazyListScope.mesaDiretoraItems(mesa: List<MembroMesa>) {
    item { SectionHeader(title = "Mesa Diretora") }
    if (mesa.isEmpty()) {
        item { EmptyState("Nenhum membro encontrado") }
        return
    }
    items(mesa) { m ->
        ListRow(
            icon = { InitialAvatar(m.nome, size = 36) },
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
            }
        )
        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}
