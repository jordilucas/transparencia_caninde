package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.data.RecentSearchStore
import kotlinx.coroutines.delay

@Composable
fun BuscaScreen(
    prefeitura: PrefeituraUiState,
    camara: CamaraUiState,
    connectionState: ConnectionState = ConnectionState.Connected,
    onContratoClick: (Contrato) -> Unit,
    onVereadorClick: (Parlamentar) -> Unit,
    onSecretariaClick: (Secretaria) -> Unit,
    onLicitacaoClick: (Licitacao) -> Unit,
    onMateriaClick: (Materia) -> Unit,
    onPublicacaoClick: (Publicacao) -> Unit = {},
    onSessaoClick: (Int, Sessao) -> Unit = { _, _ -> },
    onTransparenciaLinkClick: (LinkExterno) -> Unit = {},
    onDocumentoClick: (DocumentoCamara) -> Unit = {},
    onObraClick: (Obra) -> Unit = {},
    onLrfClick: (LrfDocumento) -> Unit = {},
    onSobreClick: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(SearchScope.Tudo) }
    var entityFilter by remember { mutableStateOf(SearchEntityFilter.Todos) }
    val sectionLimits = remember { mutableStateMapOf<String, Int>() }
    var recentSearches by remember { mutableStateOf(RecentSearchStore.load()) }

    val hits = remember(prefeitura, camara, searchQuery, scope, entityFilter) {
        SearchIndex.search(prefeitura, camara, searchQuery, scope, entityFilter)
    }
    val grouped = remember(hits) { SearchIndex.grouped(hits) }

    LaunchedEffect(grouped) {
        val sections = grouped.map { it.first }.toSet()
        sectionLimits.keys.filter { it !in sections }.forEach { sectionLimits.remove(it) }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(AppColors.Navy800)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Buscar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Blue100,
                )
                IconButton(onClick = onSobreClick) {
                    Icon(Icons.Default.Info, contentDescription = "Sobre", tint = AppColors.Blue100)
                }
            }
            Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Contratos, vereadores, publicações…", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Blue500,
                        unfocusedBorderColor = AppColors.Divider,
                        cursorColor = AppColors.Blue500,
                        focusedContainerColor = AppColors.Surface,
                        unfocusedContainerColor = AppColors.Surface,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchScope.entries.forEach { filter ->
                        FilterChip(
                            selected = scope == filter,
                            onClick = { scope = filter },
                            label = { Text(filter.label, fontSize = 11.sp) },
                        )
                    }
                }
                if (searchQuery.isNotBlank()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SearchEntityFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = entityFilter == filter,
                                onClick = { entityFilter = filter },
                                label = { Text(filter.label, fontSize = 10.sp) },
                            )
                        }
                    }
                }
            }
        }

        ConnectionBanner(connectionState)

        if (searchQuery.isBlank()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Digite para buscar", fontSize = 14.sp, color = AppColors.TextSecondary)
                Text(
                    "A busca ignora acentos (ex.: educacao = educação)",
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (recentSearches.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Buscas recentes", fontSize = 12.sp, color = AppColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recentSearches.forEach { term ->
                            SuggestionChip(
                                onClick = { searchQuery = term },
                                label = { Text(term, fontSize = 11.sp) },
                            )
                        }
                    }
                }
                if (prefeitura.contratos.isEmpty() && camara.parlamentares.isEmpty()) {
                    Text(
                        "Aguarde o carregamento dos dados ou verifique a conexão.",
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LaunchedEffect(searchQuery, scope, entityFilter, hits.size) {
                if (searchQuery.length < 2) return@LaunchedEffect
                val queryLength = searchQuery.length
                val querySnapshot = searchQuery.trim()
                delay(600)
                if (searchQuery.length >= 2) {
                    RecentSearchStore.add(querySnapshot)
                    recentSearches = RecentSearchStore.load()
                    AppAnalytics.logSearch(
                        queryLength = queryLength,
                        resultsCount = hits.size,
                        scope = "${scope.name.lowercase()}:${entityFilter.name.lowercase()}",
                    )
                }
            }

            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                if (hits.isNotEmpty()) {
                    item {
                        Text(
                            "${hits.size} resultado${if (hits.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }

                grouped.forEach { (section, sectionHits) ->
                    val limit = sectionLimits[section] ?: SEARCH_PAGE_SIZE
                    val visible = sectionHits.take(limit)

                    item { SectionHeader("$section (${sectionHits.size})") }
                    items(visible, key = { "${section}-${it.hashCode()}" }) { hit ->
                        SearchHitRow(
                            hit,
                            onContratoClick,
                            onLicitacaoClick,
                            onPublicacaoClick,
                            onSecretariaClick,
                            onVereadorClick,
                            onMateriaClick,
                            onSessaoClick,
                            onTransparenciaLinkClick,
                            onDocumentoClick,
                            onObraClick,
                            onLrfClick,
                        )
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    if (sectionHits.size > limit) {
                        item {
                            TextButton(
                                onClick = { sectionLimits[section] = limit + SEARCH_PAGE_SIZE },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Ver mais (${sectionHits.size - limit} restantes)",
                                    fontSize = 12.sp,
                                    color = AppColors.Blue500,
                                )
                            }
                        }
                    }
                }

                if (hits.isEmpty()) {
                    item { EmptyState("Nenhum resultado para \"$searchQuery\"") }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    hit: SearchHit,
    onContratoClick: (Contrato) -> Unit,
    onLicitacaoClick: (Licitacao) -> Unit,
    onPublicacaoClick: (Publicacao) -> Unit,
    onSecretariaClick: (Secretaria) -> Unit,
    onVereadorClick: (Parlamentar) -> Unit,
    onMateriaClick: (Materia) -> Unit,
    onSessaoClick: (Int, Sessao) -> Unit,
    onTransparenciaLinkClick: (LinkExterno) -> Unit,
    onDocumentoClick: (DocumentoCamara) -> Unit,
    onObraClick: (Obra) -> Unit,
    onLrfClick: (LrfDocumento) -> Unit,
) {
    when (hit) {
        is SearchHit.ContratoHit -> ContratosRow(hit.item, onClick = { onContratoClick(hit.item) })
        is SearchHit.LicitacaoHit -> LicitacoesRow(hit.item, onClick = { onLicitacaoClick(hit.item) })
        is SearchHit.PublicacaoHit -> ListRow(
            icon = { IconContainer(AppColors.Green100) { Icon(Icons.Default.Article, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onPublicacaoClick(hit.item) },
        )
        is SearchHit.SecretariaHit -> ListRow(
            icon = { IconContainer(AppColors.Blue100) { Icon(Icons.Default.AccountBalance, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onSecretariaClick(hit.item) },
        )
        is SearchHit.ObraHit -> ListRow(
            icon = { IconContainer(AppColors.Amber100) { Icon(Icons.Default.Construction, null, tint = AppColors.Amber700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onObraClick(hit.item) },
        )
        is SearchHit.LrfHit -> ListRow(
            icon = { IconContainer(AppColors.Blue100) { Icon(Icons.Default.Description, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onLrfClick(hit.item) },
        )
        is SearchHit.GestorHit -> ListRow(
            icon = { IconContainer(AppColors.Purple100) { Icon(Icons.Default.Person, null, tint = AppColors.Purple700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = {},
        )
        is SearchHit.ParlamentarHit -> ParlamentarRow(hit.item, onClick = { onVereadorClick(hit.item) })
        is SearchHit.MateriaHit -> ListRow(
            icon = { IconContainer(AppColors.Purple100) { Icon(Icons.Default.FilePresent, null, tint = AppColors.Purple700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onMateriaClick(hit.item) },
        )
        is SearchHit.SessaoHit -> ListRow(
            icon = { IconContainer(AppColors.Green100) { Icon(Icons.Default.Event, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { onSessaoClick(hit.index, hit.item) },
        )
        is SearchHit.MesaHit -> ListRow(
            icon = { IconContainer(AppColors.Blue100) { Icon(Icons.Default.Groups, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = {},
        )
        is SearchHit.LinkCamaraHit -> TransparenciaLinkRow(hit.item, onClick = onTransparenciaLinkClick)
        is SearchHit.LinkPrefeituraHit -> TransparenciaLinkRow(hit.item, onClick = onTransparenciaLinkClick)
        is SearchHit.DocumentoCamaraHit -> ListRow(
            icon = { IconContainer(AppColors.Amber100) { Icon(Icons.Default.Description, null, tint = AppColors.Amber700, modifier = Modifier.size(18.dp)) } },
            title = hit.title,
            subtitle = hit.subtitle,
            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
            onClick = { if (hit.item.url.isNotBlank()) onDocumentoClick(hit.item) },
        )
    }
}
