package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

@Composable
fun BuscaScreen(
    prefeitura: PrefeituraUiState,
    camara: CamaraUiState,
    onContratoClick: (Contrato) -> Unit,
    onVereadorClick: (Parlamentar) -> Unit,
    onSecretariaClick: (Secretaria) -> Unit,
    onLicitacaoClick: (Licitacao) -> Unit,
    onMateriaClick: (Materia) -> Unit,
    onPublicacaoClick: (Publicacao) -> Unit = {},
    onSessaoClick: (Int, Sessao) -> Unit = { _, _ -> },
    onSobreClick: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(SearchScope.Tudo) }

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
            }
        }

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
            }
        } else {
            val showPref = scope == SearchScope.Tudo || scope == SearchScope.Prefeitura
            val showCam = scope == SearchScope.Tudo || scope == SearchScope.Camara

            val contratos = if (showPref) prefeitura.contratos.filter {
                matchesAnySearch(searchQuery, it.objeto, it.empresa, it.numero, it.secretaria)
            } else emptyList()
            val licitacoes = if (showPref) prefeitura.licitacoes.filter {
                matchesAnySearch(searchQuery, it.objeto, it.numero, it.modalidade, it.situacao)
            } else emptyList()
            val secretarias = if (showPref) prefeitura.secretarias.filter {
                matchesAnySearch(searchQuery, it.nome, it.secretario)
            } else emptyList()
            val publicacoes = if (showPref) prefeitura.publicacoes.filter {
                matchesAnySearch(searchQuery, it.titulo, it.tipo, it.data)
            } else emptyList()
            val obras = if (showPref) prefeitura.obras.filter {
                matchesAnySearch(searchQuery, it.titulo, it.descricao, it.secretaria, it.situacao)
            } else emptyList()
            val lrfDocs = if (showPref) prefeitura.lrf.filter {
                matchesAnySearch(searchQuery, it.titulo, it.tipo, it.exercicio)
            } else emptyList()
            val gestores = if (showPref) prefeitura.gestores.filter {
                matchesAnySearch(searchQuery, it.nome, it.cargo)
            } else emptyList()
            val parlamentares = if (showCam) camara.parlamentares.filter {
                matchesAnySearch(searchQuery, it.nome, it.nomeCompleto, it.partido, it.cargo)
            } else emptyList()
            val materias = if (showCam) camara.materias.filter {
                matchesAnySearch(searchQuery, it.titulo, it.tipo, it.autor)
            } else emptyList()
            val sessoes = if (showCam) camara.sessoes.withIndex().filter { (_, s) ->
                matchesAnySearch(searchQuery, s.titulo, s.data, s.resumo)
            } else emptyList()

            val total = contratos.size + licitacoes.size + secretarias.size + publicacoes.size +
                obras.size + lrfDocs.size +
                gestores.size + parlamentares.size + materias.size + sessoes.size

            LaunchedEffect(searchQuery, scope, total) {
                if (searchQuery.length < 2) return@LaunchedEffect
                val queryLength = searchQuery.length
                val scopeName = scope.name.lowercase()
                val resultCount = total
                delay(600)
                if (searchQuery.length >= 2) {
                    AppAnalytics.logSearch(
                        queryLength = queryLength,
                        resultsCount = resultCount,
                        scope = scopeName,
                    )
                }
            }

            Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
                if (total > 0) {
                    Text(
                        "$total resultado${if (total == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (contratos.isNotEmpty()) {
                    SectionHeader("Contratos (${contratos.size})")
                    contratos.take(8).forEach { c ->
                        ContratosRow(c, onClick = { onContratoClick(c) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (licitacoes.isNotEmpty()) {
                    SectionHeader("Licitações (${licitacoes.size})")
                    licitacoes.take(8).forEach { l ->
                        LicitacoesRow(l, onClick = { onLicitacaoClick(l) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (publicacoes.isNotEmpty()) {
                    SectionHeader("Publicações (${publicacoes.size})")
                    publicacoes.take(8).forEach { p ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Green100) {
                                    Icon(Icons.Default.Article, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = p.titulo,
                            subtitle = listOfNotNull(p.tipo.takeIf { it.isNotBlank() }, p.data.takeIf { it.isNotBlank() }).joinToString(" · "),
                            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                            onClick = { onPublicacaoClick(p) },
                        )
                    }
                }
                if (obras.isNotEmpty()) {
                    SectionHeader("Obras (${obras.size})")
                    obras.take(8).forEach { o ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Amber100) {
                                    Icon(Icons.Default.Construction, null, tint = AppColors.Amber700, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = o.titulo,
                            subtitle = listOfNotNull(o.secretaria.takeIf { it.isNotBlank() }, o.valor.takeIf { it.isNotBlank() }).joinToString(" · "),
                            trailing = {},
                        )
                    }
                }
                if (lrfDocs.isNotEmpty()) {
                    SectionHeader("LRF (${lrfDocs.size})")
                    lrfDocs.take(8).forEach { d ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Blue100) {
                                    Icon(Icons.Default.Description, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = d.titulo,
                            subtitle = d.tipo,
                            trailing = {},
                        )
                    }
                }
                if (secretarias.isNotEmpty()) {
                    SectionHeader("Secretarias (${secretarias.size})")
                    secretarias.take(8).forEach { s ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Blue100) {
                                    Icon(Icons.Default.AccountBalance, null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = s.nome,
                            subtitle = s.secretario,
                            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                            onClick = { onSecretariaClick(s) },
                        )
                    }
                }
                if (gestores.isNotEmpty()) {
                    SectionHeader("Gestores (${gestores.size})")
                    gestores.take(5).forEach { g ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Purple100) {
                                    Icon(Icons.Default.Person, null, tint = AppColors.Purple700, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = g.nome,
                            subtitle = g.cargo,
                            trailing = {},
                        )
                    }
                }
                if (parlamentares.isNotEmpty()) {
                    SectionHeader("Vereadores (${parlamentares.size})")
                    parlamentares.take(8).forEach { p ->
                        ParlamentarRow(p, onClick = { onVereadorClick(p) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (materias.isNotEmpty()) {
                    SectionHeader("Matérias (${materias.size})")
                    materias.take(8).forEach { m ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Purple100) {
                                    Icon(Icons.Default.FilePresent, null, tint = AppColors.Purple700, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = m.titulo,
                            subtitle = m.tipo,
                            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                            onClick = { onMateriaClick(m) },
                        )
                    }
                }
                if (sessoes.isNotEmpty()) {
                    SectionHeader("Sessões (${sessoes.size})")
                    sessoes.take(8).forEach { (idx, s) ->
                        ListRow(
                            icon = {
                                IconContainer(AppColors.Green100) {
                                    Icon(Icons.Default.Event, null, tint = AppColors.Green700, modifier = Modifier.size(18.dp))
                                }
                            },
                            title = s.titulo.ifBlank { "Sessão ${idx + 1}" },
                            subtitle = s.data,
                            trailing = { Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp)) },
                            onClick = { onSessaoClick(idx, s) },
                        )
                    }
                }
                if (total == 0) {
                    EmptyState("Nenhum resultado para \"$searchQuery\"")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
