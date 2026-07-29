package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.presentation.detail.*

enum class Screen {
    Prefeitura, Camara, Graficos, Busca, Sobre
}

sealed class AppRoute {
    data class Main(val screen: Screen) : AppRoute()
    data class Vereador(val slug: String) : AppRoute()
    data class Materia(val slug: String) : AppRoute()
    data class Secretaria(val id: String) : AppRoute()
    data class Contrato(val numero: String) : AppRoute()
    data class Licitacao(val numero: String) : AppRoute()
    data class Sessao(val id: String) : AppRoute()
    data class Publicacao(val id: String) : AppRoute()
    data class PaginaPortal(val pageId: String) : AppRoute()
    data object Gestores : AppRoute()
    data class Institucional(val camara: Boolean) : AppRoute()
}

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

val navItems: List<NavItem> by lazy {
    listOf(
        NavItem(Screen.Prefeitura, "Prefeitura", Icons.Default.AccountBalance),
        NavItem(Screen.Camara, "Câmara", Icons.Default.Groups),
        NavItem(Screen.Graficos, "Gráficos", Icons.Default.BarChart),
        NavItem(Screen.Busca, "Busca", Icons.Default.Search),
        NavItem(Screen.Sobre, "Sobre", Icons.Default.Info),
    )
}

@Composable
fun TransparenciaApp(viewModel: TransparenciaViewModel) {
    val routeStack = remember { mutableStateListOf<AppRoute>(AppRoute.Main(Screen.Prefeitura)) }
    val currentRoute = routeStack.last()
    val showBottomBar = currentRoute is AppRoute.Main

    fun navigate(route: AppRoute) {
        routeStack.add(route)
    }

    fun navigateBack() {
        if (routeStack.size > 1) routeStack.removeAt(routeStack.lastIndex)
    }

    fun selectMainScreen(screen: Screen) {
        if (routeStack.size > 1) {
            routeStack.clear()
            routeStack.add(AppRoute.Main(screen))
        } else {
            routeStack[0] = AppRoute.Main(screen)
        }
    }

    val connectionState by viewModel.connectionState.collectAsState()
    val prefeituraState by viewModel.prefeituraState.collectAsState()
    val camaraState by viewModel.camaraState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onStart()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onStop() }
    }

    TransparenciaTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 840.dp && showBottomBar

            if (useRail) {
                Row(Modifier.fillMaxSize()) {
                    MainNavigationRail(
                        currentScreen = (currentRoute as AppRoute.Main).screen,
                        onScreenSelected = ::selectMainScreen,
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        AppRouteContent(
                            currentRoute = currentRoute,
                            viewModel = viewModel,
                            connectionState = connectionState,
                            prefeituraState = prefeituraState,
                            camaraState = camaraState,
                            onNavigate = ::navigate,
                            onNavigateBack = ::navigateBack,
                            onMainScreenSelect = ::selectMainScreen,
                        )
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (showBottomBar) {
                            MainNavigationBar(
                                currentScreen = (currentRoute as AppRoute.Main).screen,
                                onScreenSelected = ::selectMainScreen,
                            )
                        }
                    },
                ) { paddingValues ->
                    val layoutDirection = LocalLayoutDirection.current
                    val contentPadding = if (showBottomBar) {
                        paddingValues
                    } else {
                        PaddingValues(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    ) {
                        AppRouteContent(
                            currentRoute = currentRoute,
                            viewModel = viewModel,
                            connectionState = connectionState,
                            prefeituraState = prefeituraState,
                            camaraState = camaraState,
                            onNavigate = ::navigate,
                            onNavigateBack = ::navigateBack,
                            onMainScreenSelect = ::selectMainScreen,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    NavigationBar(
        containerColor = AppColors.Card,
        contentColor = AppColors.TextPrimary,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.Navy800,
                    selectedTextColor = AppColors.Navy800,
                    unselectedIconColor = AppColors.TextTertiary,
                    unselectedTextColor = AppColors.TextTertiary,
                    indicatorColor = AppColors.Blue100,
                ),
            )
        }
    }
}

@Composable
private fun MainNavigationRail(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    NavigationRail(
        containerColor = AppColors.Card,
        contentColor = AppColors.TextPrimary,
        modifier = Modifier.fillMaxHeight().width(88.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Canindé",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Navy800,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        navItems.forEach { item ->
            NavigationRailItem(
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AppColors.Navy800,
                    selectedTextColor = AppColors.Navy800,
                    unselectedIconColor = AppColors.TextTertiary,
                    unselectedTextColor = AppColors.TextTertiary,
                    indicatorColor = AppColors.Blue100,
                ),
            )
        }
    }
}

@Composable
private fun AppRouteContent(
    currentRoute: AppRoute,
    viewModel: TransparenciaViewModel,
    connectionState: ConnectionState,
    prefeituraState: PrefeituraUiState,
    camaraState: CamaraUiState,
    onNavigate: (AppRoute) -> Unit,
    onNavigateBack: () -> Unit,
    onMainScreenSelect: (Screen) -> Unit,
) {
    val onSobreClick = { onMainScreenSelect(Screen.Sobre) }
    when (val route = currentRoute) {
        is AppRoute.Main -> when (route.screen) {
            Screen.Prefeitura -> PrefeituraScreen(
                state = prefeituraState,
                connectionState = connectionState,
                onRefresh = { viewModel.refreshPrefeitura() },
                onContratoClick = { onNavigate(AppRoute.Contrato(it.numero)) },
                onLicitacaoClick = { onNavigate(AppRoute.Licitacao(it.numero)) },
                onSecretariaClick = { onNavigate(AppRoute.Secretaria(it.id.ifBlank { it.nome })) },
                onGestoresClick = { onNavigate(AppRoute.Gestores) },
                onInstitucionalClick = { onNavigate(AppRoute.Institucional(false)) },
                onPublicacaoClick = { onNavigate(routeFromPublicacao(it)) },
                onTransparenciaLinkClick = { onNavigate(routeFromLink(it)) },
                onSobreClick = onSobreClick,
            )
            Screen.Camara -> CamaraScreen(
                state = camaraState,
                connectionState = connectionState,
                onRefresh = { viewModel.refreshCamara() },
                onVereadorClick = { onNavigate(AppRoute.Vereador(it.slug.ifBlank { it.nome })) },
                onMateriaClick = { onNavigate(AppRoute.Materia(it.slug.ifBlank { it.titulo })) },
                onSessaoClick = { idx, _ -> onNavigate(AppRoute.Sessao(idx.toString())) },
                onInstitucionalClick = { onNavigate(AppRoute.Institucional(true)) },
                onTransparenciaLinkClick = { onNavigate(routeFromLink(it)) },
                onSobreClick = onSobreClick,
            )
            Screen.Graficos -> GraficosScreen(
                prefeituraState = prefeituraState,
                camaraState = camaraState,
                onSobreClick = onSobreClick,
            )
            Screen.Busca -> BuscaScreen(
                prefeitura = prefeituraState,
                camara = camaraState,
                onContratoClick = { onNavigate(AppRoute.Contrato(it.numero)) },
                onVereadorClick = { onNavigate(AppRoute.Vereador(it.slug.ifBlank { it.nome })) },
                onSecretariaClick = { onNavigate(AppRoute.Secretaria(it.id.ifBlank { it.nome })) },
                onLicitacaoClick = { onNavigate(AppRoute.Licitacao(it.numero)) },
                onMateriaClick = { onNavigate(AppRoute.Materia(it.slug.ifBlank { it.titulo })) },
                onSobreClick = onSobreClick,
            )
            Screen.Sobre -> SobreScreen(
                prefeituraState = prefeituraState,
                camaraState = camaraState,
            )
        }
        is AppRoute.Vereador -> VereadorDetailScreen(viewModel, route.slug, onNavigateBack)
        is AppRoute.Materia -> MateriaDetailScreen(viewModel, route.slug, onNavigateBack)
        is AppRoute.Secretaria -> SecretariaDetailScreen(
            viewModel,
            route.id,
            onNavigateBack,
            onNavigate = onNavigate,
        )
        is AppRoute.Contrato -> ContratoDetailScreen(viewModel, route.numero, onNavigateBack)
        is AppRoute.Licitacao -> LicitacaoDetailScreen(viewModel, route.numero, onNavigateBack)
        is AppRoute.Sessao -> SessaoDetailScreen(viewModel, route.id, onNavigateBack)
        is AppRoute.Publicacao -> PublicacaoDetailScreen(viewModel, route.id, onNavigateBack)
        is AppRoute.PaginaPortal -> PaginaPortalDetailScreen(viewModel, route.pageId, onNavigateBack)
        AppRoute.Gestores -> GestoresDetailScreen(viewModel, onNavigateBack)
        is AppRoute.Institucional -> InstitucionalDetailScreen(viewModel, route.camara, onNavigateBack)
    }
}

@Composable
fun GraficosScreen(
    prefeituraState: PrefeituraUiState,
    camaraState: CamaraUiState,
    onSobreClick: () -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Prefeitura", "Câmara")

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(Modifier.fillMaxWidth().background(AppColors.Navy800)) {
            Column {
                DataStatusBanner(error = prefeituraState.error ?: camaraState.error)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Gráficos — dados reais",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Blue100,
                        modifier = Modifier.padding(16.dp),
                    )
                    IconButton(onClick = onSobreClick, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Sobre", tint = AppColors.Blue100)
                    }
                }
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = AppColors.Navy800,
                    contentColor = AppColors.Blue100,
                ) {
                    tabs.forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, fontSize = 12.sp) })
                    }
                }
            }
        }

        val loading = if (tab == 0) prefeituraState.isLoading else camaraState.isLoading
        if (loading) {
            ShimmerContent()
            return@Column
        }

        val series = if (tab == 0) {
            prefeituraState.graficos?.prefeitura.orEmpty()
        } else {
            camaraState.graficos?.camara.orEmpty()
        }

        if (series.isEmpty()) {
            val hasListData = if (tab == 0) {
                prefeituraState.contratos.isNotEmpty() || prefeituraState.licitacoes.isNotEmpty()
            } else {
                camaraState.materias.isNotEmpty() || camaraState.parlamentares.isNotEmpty()
            }
            EmptyState(
                if (!hasListData) {
                    "Sem dados para gráficos. Conecte ao servidor e aguarde o carregamento."
                } else {
                    "Agregações ainda não disponíveis; atualize os dados."
                },
            )
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tab == 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Contratos", "${prefeituraState.resumo.totalContratos}", modifier = Modifier.weight(1f))
                    MetricCard("Licitações", "${prefeituraState.resumo.totalLicitacoes}", modifier = Modifier.weight(1f))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Vereadores", "${camaraState.resumo.totalParlamentares}", modifier = Modifier.weight(1f))
                    MetricCard("Matérias", "${camaraState.resumo.totalMaterias}", modifier = Modifier.weight(1f))
                }
            }
            series.forEach { br.gov.caninde.transparencia.presentation.detail.ChartBarSection(it) }
            LastUpdatedText(if (tab == 0) prefeituraState.lastUpdated else camaraState.lastUpdated)
        }
    }
}

@Composable
fun BuscaScreen(
    prefeitura: PrefeituraUiState,
    camara: CamaraUiState,
    onContratoClick: (Contrato) -> Unit,
    onVereadorClick: (Parlamentar) -> Unit,
    onSecretariaClick: (Secretaria) -> Unit,
    onLicitacaoClick: (Licitacao) -> Unit,
    onMateriaClick: (Materia) -> Unit,
    onSobreClick: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

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
            Column(Modifier.padding(top = 40.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Procurar contratos, vereadores...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Blue500,
                        unfocusedBorderColor = AppColors.Divider,
                        cursorColor = AppColors.Blue500,
                        focusedContainerColor = AppColors.Surface,
                        unfocusedContainerColor = AppColors.Surface,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                )
            }
        }

        if (searchQuery.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Digite para buscar", fontSize = 14.sp, color = AppColors.TextSecondary)
            }
        } else {
            val query = searchQuery.lowercase()
            val contratos = prefeitura.contratos.filter {
                it.objeto.lowercase().contains(query) || it.empresa.lowercase().contains(query) || it.numero.lowercase().contains(query)
            }
            val licitacoes = prefeitura.licitacoes.filter {
                it.objeto.lowercase().contains(query) || it.numero.lowercase().contains(query)
            }
            val secretarias = prefeitura.secretarias.filter { it.nome.lowercase().contains(query) }
            val parlamentares = camara.parlamentares.filter {
                it.nome.lowercase().contains(query) || it.partido.lowercase().contains(query)
            }
            val materias = camara.materias.filter { it.titulo.lowercase().contains(query) }

            Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
                if (contratos.isNotEmpty()) {
                    SectionHeader("Contratos (${contratos.size})")
                    contratos.take(5).forEach { c ->
                        ContratosRow(c, onClick = { onContratoClick(c) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (licitacoes.isNotEmpty()) {
                    SectionHeader("Licitações (${licitacoes.size})")
                    licitacoes.take(5).forEach { l ->
                        LicitacoesRow(l, onClick = { onLicitacaoClick(l) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (secretarias.isNotEmpty()) {
                    SectionHeader("Secretarias (${secretarias.size})")
                    secretarias.take(5).forEach { s ->
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
                if (parlamentares.isNotEmpty()) {
                    SectionHeader("Vereadores (${parlamentares.size})")
                    parlamentares.take(5).forEach { p ->
                        ParlamentarRow(p, onClick = { onVereadorClick(p) })
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                if (materias.isNotEmpty()) {
                    SectionHeader("Matérias (${materias.size})")
                    materias.take(5).forEach { m ->
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
                if (contratos.isEmpty() && licitacoes.isEmpty() && secretarias.isEmpty() && parlamentares.isEmpty() && materias.isEmpty()) {
                    EmptyState("Nenhum resultado encontrado")
                }
            }
        }
    }
}
