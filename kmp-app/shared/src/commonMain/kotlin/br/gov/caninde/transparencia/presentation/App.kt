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
import br.gov.caninde.transparencia.platform.hideAppLoadingScreen

enum class Screen {
    Prefeitura, Folha, Camara, Graficos, Busca, Agua, Sobre
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
    data class DocumentoCamara(val pageId: String) : AppRoute()
    data object Gestores : AppRoute()
    data class Obra(val id: String) : AppRoute()
    data class Lrf(val id: String) : AppRoute()
    data class Institucional(val camara: Boolean) : AppRoute()
}

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

val navItems: List<NavItem> by lazy {
    listOf(
        NavItem(Screen.Prefeitura, "Prefeitura", Icons.Default.AccountBalance),
        NavItem(Screen.Folha, "Folha", Icons.Default.Payments),
        NavItem(Screen.Camara, "Câmara", Icons.Default.Groups),
        NavItem(Screen.Graficos, "Gráficos", Icons.Default.BarChart),
        NavItem(Screen.Busca, "Busca", Icons.Default.Search),
        NavItem(Screen.Agua, "Água", Icons.Default.WaterDrop),
        NavItem(Screen.Sobre, "Sobre", Icons.Default.Info),
    )
}

@Composable
fun TransparenciaApp(
    viewModel: TransparenciaViewModel,
    initialWebPath: String? = null,
) {
    val parsedInitial = initialWebPath?.let { parseWebPath(it) }
    val routeStack = remember {
        mutableStateListOf(parsedInitial ?: AppRoute.Main(Screen.Prefeitura))
    }
    val currentRoute = routeStack.last()
    val showBottomBar = currentRoute is AppRoute.Main
    var skipNextWebPush by remember { mutableStateOf(false) }
    var isInitialWebSync by remember { mutableStateOf(true) }

    fun syncFromWebPath(path: String) {
        parseWebPath(path)?.let { route ->
            skipNextWebPush = true
            routeStack.clear()
            routeStack.add(route)
        }
    }

    fun navigate(route: AppRoute) {
        routeStack.add(route)
    }

    fun navigateBack() {
        if (routeStack.size > 1) {
            webHistoryBack()
            return
        }
        if (routeStack.size == 1 && routeStack[0] !is AppRoute.Main) {
            skipNextWebPush = true
            routeStack.clear()
            routeStack.add(AppRoute.Main(Screen.Prefeitura))
            updateWebPath("/", replace = true)
        }
    }

    fun selectMainScreen(screen: Screen) {
        skipNextWebPush = true
        if (routeStack.size > 1) {
            routeStack.clear()
            routeStack.add(AppRoute.Main(screen))
        } else {
            routeStack[0] = AppRoute.Main(screen)
        }
        updateWebPath(AppRoute.Main(screen).toWebPath(), replace = true)
    }

    val connectionState by viewModel.connectionState.collectAsState()
    val prefeituraState by viewModel.prefeituraState.collectAsState()
    val camaraState by viewModel.camaraState.collectAsState()

    val onOfflineSafeScreen = currentRoute is AppRoute.Main &&
        (currentRoute as AppRoute.Main).screen.let { it == Screen.Sobre || it == Screen.Agua }
    val showConnectionError = shouldShowConnectionErrorScreen(
        connectionState = connectionState,
        prefeitura = prefeituraState,
        camara = camaraState,
        onOfflineSafeScreen = onOfflineSafeScreen,
    )

    LaunchedEffect(Unit) {
        viewModel.onStart()
        hideAppLoadingScreen()
    }

    LaunchedEffect(currentRoute) {
        AppAnalytics.logScreen(currentRoute)
        when {
            isInitialWebSync -> {
                isInitialWebSync = false
                updateWebPath(currentRoute.toWebPath(), replace = true)
            }
            skipNextWebPush -> skipNextWebPush = false
            else -> updateWebPath(currentRoute.toWebPath(), replace = false)
        }
    }

    DisposableEffect(Unit) {
        val removeListener = installWebHistoryListener(::syncFromWebPath)
        onDispose { removeListener() }
    }

    LaunchedEffect(showConnectionError) {
        if (showConnectionError) AppAnalytics.logConnectionError()
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
                        MainAppContent(
                            showConnectionError = showConnectionError,
                            connectionState = connectionState,
                            currentRoute = currentRoute,
                            viewModel = viewModel,
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
                        MainAppContent(
                            showConnectionError = showConnectionError,
                            connectionState = connectionState,
                            currentRoute = currentRoute,
                            viewModel = viewModel,
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
private fun MainAppContent(
    showConnectionError: Boolean,
    connectionState: ConnectionState,
    currentRoute: AppRoute,
    viewModel: TransparenciaViewModel,
    prefeituraState: PrefeituraUiState,
    camaraState: CamaraUiState,
    onNavigate: (AppRoute) -> Unit,
    onNavigateBack: () -> Unit,
    onMainScreenSelect: (Screen) -> Unit,
) {
    if (showConnectionError && currentRoute is AppRoute.Main) {
        ConnectionErrorScreen(
            connectionState = connectionState,
            onRetry = {
                AppAnalytics.logRetryConnection()
                viewModel.reconnect()
            },
            onSobreClick = { onMainScreenSelect(Screen.Sobre) },
        )
    } else {
        AppRouteContent(
            currentRoute = currentRoute,
            viewModel = viewModel,
            connectionState = connectionState,
            prefeituraState = prefeituraState,
            camaraState = camaraState,
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
            onMainScreenSelect = onMainScreenSelect,
        )
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
                onExercicioChange = { year -> viewModel.refreshPrefeitura(year) },
                onAguaClick = { onMainScreenSelect(Screen.Agua) },
                onContratoClick = { onNavigate(AppRoute.Contrato(contratoDetailId(it))) },
                onLicitacaoClick = { onNavigate(AppRoute.Licitacao(licitacaoDetailId(it))) },
                onSecretariaClick = { s ->
                    if (s.id.isNotBlank()) onNavigate(AppRoute.Secretaria(s.id))
                },
                onGestoresClick = { onNavigate(AppRoute.Gestores) },
                onInstitucionalClick = { onNavigate(AppRoute.Institucional(false)) },
                onPublicacaoClick = { onNavigate(routeFromPublicacao(it)) },
                onObraClick = { o ->
                    val id = o.id.ifBlank { o.titulo }
                    if (id.isNotBlank()) onNavigate(AppRoute.Obra(id))
                },
                onLrfClick = { doc ->
                    val id = lrfDetailId(doc)
                    if (id.isNotBlank()) onNavigate(AppRoute.Lrf(id))
                },
                onTransparenciaLinkClick = { onNavigate(routeFromLink(it)) },
                onSobreClick = onSobreClick,
            )
            Screen.Folha -> FolhaPagamentoScreen(
                prefeituraState = prefeituraState,
                connectionState = connectionState,
                onRefresh = { viewModel.refreshPrefeitura(prefeituraState.resumo.exercicio) },
                onExercicioChange = { year -> viewModel.refreshPrefeitura(year) },
                onSobreClick = onSobreClick,
            )
            Screen.Camara -> CamaraScreen(
                state = camaraState,
                connectionState = connectionState,
                onRefresh = { viewModel.refreshCamara() },
                onVereadorClick = { p ->
                    parlamentarSlug(p).takeIf { it.isNotBlank() }?.let { onNavigate(AppRoute.Vereador(it)) }
                },
                onMateriaClick = { m ->
                    materiaSlug(m).takeIf { it.isNotBlank() }?.let { onNavigate(AppRoute.Materia(it)) }
                },
                onSessaoClick = { idx, s -> onNavigate(AppRoute.Sessao(sessaoRouteId(s, idx))) },
                onInstitucionalClick = { onNavigate(AppRoute.Institucional(true)) },
                onTransparenciaLinkClick = { onNavigate(routeFromLink(it)) },
                onDocumentoClick = { doc -> onNavigate(AppRoute.DocumentoCamara(documentoCamaraRouteId(doc))) },
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
                connectionState = connectionState,
                onContratoClick = { onNavigate(AppRoute.Contrato(contratoDetailId(it))) },
                onVereadorClick = { p ->
                    parlamentarSlug(p).takeIf { it.isNotBlank() }?.let { onNavigate(AppRoute.Vereador(it)) }
                },
                onSecretariaClick = { s ->
                    if (s.id.isNotBlank()) onNavigate(AppRoute.Secretaria(s.id))
                },
                onLicitacaoClick = { onNavigate(AppRoute.Licitacao(licitacaoDetailId(it))) },
                onMateriaClick = { m ->
                    materiaSlug(m).takeIf { it.isNotBlank() }?.let { onNavigate(AppRoute.Materia(it)) }
                },
                onPublicacaoClick = { onNavigate(routeFromPublicacao(it)) },
                onSessaoClick = { idx, s -> onNavigate(AppRoute.Sessao(sessaoRouteId(s, idx))) },
                onTransparenciaLinkClick = { onNavigate(routeFromLink(it)) },
                onDocumentoClick = { doc -> onNavigate(AppRoute.DocumentoCamara(documentoCamaraRouteId(doc))) },
                onObraClick = { o ->
                    val id = o.id.ifBlank { o.titulo }
                    if (id.isNotBlank()) onNavigate(AppRoute.Obra(id))
                },
                onLrfClick = { doc ->
                    val id = lrfDetailId(doc)
                    if (id.isNotBlank()) onNavigate(AppRoute.Lrf(id))
                },
                onSobreClick = onSobreClick,
            )
            Screen.Agua -> ReclamacaoAguaScreen(onSobreClick = onSobreClick)
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
        is AppRoute.Obra -> ObraDetailScreen(prefeituraState.obras, route.id, onNavigateBack)
        is AppRoute.Lrf -> LrfDetailScreen(prefeituraState.lrf, route.id, onNavigateBack)
        is AppRoute.PaginaPortal -> PaginaPortalDetailScreen(viewModel, route.pageId, onNavigateBack)
        is AppRoute.DocumentoCamara -> DocumentoCamaraDetailScreen(viewModel, route.pageId, onNavigateBack)
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
