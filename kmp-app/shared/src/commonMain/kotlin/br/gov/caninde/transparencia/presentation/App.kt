package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.domain.*

enum class Screen {
    Prefeitura, Camara, Graficos, Busca
}

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

val navItems: List<NavItem> by lazy {
    listOf(
        NavItem(Screen.Prefeitura, "Prefeitura", Icons.Default.AccountBalance),
        NavItem(Screen.Camara, "Câmara", Icons.Default.Groups),
        NavItem(Screen.Graficos, "Gráficos", Icons.Default.BarChart),
        NavItem(Screen.Busca, "Busca", Icons.Default.Search),
    )
}

@Composable
fun TransparenciaApp(viewModel: TransparenciaViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Prefeitura) }
    
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomAppBar(
                    containerColor = AppColors.Card,
                    contentColor = AppColors.TextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(60.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentScreen == item.screen,
                                onClick = { currentScreen = item.screen },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        item.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Navy800,
                                    selectedTextColor = AppColors.Navy800,
                                    unselectedIconColor = AppColors.TextTertiary,
                                    unselectedTextColor = AppColors.TextTertiary,
                                    indicatorColor = AppColors.Blue100
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.Prefeitura -> {
                        PrefeituraScreen(
                            state = prefeituraState,
                            connectionState = connectionState,
                            onRefresh = { viewModel.refreshPrefeitura() }
                        )
                    }
                    Screen.Camara -> {
                        CamaraScreen(
                            state = camaraState,
                            connectionState = connectionState,
                            onRefresh = { viewModel.refreshCamara() }
                        )
                    }
                    Screen.Graficos -> {
                        GraficosScreen(prefeituraState = prefeituraState)
                    }
                    Screen.Busca -> {
                        BuscaScreen(
                            prefeitura = prefeituraState,
                            camara = camaraState
                        )
                    }
                }
            }
        }
    }
}

// ─── Tela de Gráficos ─────────────────────────────────────────────────────────

@Composable
fun GraficosScreen(prefeituraState: PrefeituraUiState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(AppColors.Navy800)
        ) {
            Column {
                DataStatusBanner(error = prefeituraState.error)
                Text(
                    "Resumo — Prefeitura",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Blue100,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (prefeituraState.isLoading) {
            ShimmerContent()
            return@Column
        }

        val hasData = prefeituraState.contratos.isNotEmpty()
            || prefeituraState.licitacoes.isNotEmpty()
            || prefeituraState.secretarias.isNotEmpty()

        if (!hasData) {
            EmptyState(
                prefeituraState.error
                    ?: "Sem dados para exibir. Conecte ao servidor e abra a aba Prefeitura para carregar informações reais.",
            )
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    label = "Contratos",
                    value = "${prefeituraState.resumo.totalContratos}",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Licitações",
                    value = "${prefeituraState.resumo.totalLicitacoes}",
                    modifier = Modifier.weight(1f),
                )
            }

            if (prefeituraState.secretarias.isNotEmpty()) {
                SectionHeader("Secretarias (${prefeituraState.secretarias.size})")
                prefeituraState.secretarias.take(8).forEach { nome ->
                    ListRow(
                        icon = {
                            IconContainer(AppColors.Blue100) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = AppColors.Navy800,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        title = nome,
                        subtitle = "",
                        trailing = {},
                    )
                }
            }

            LastUpdatedText(prefeituraState.lastUpdated)
        }
    }
}

// ─── Tela de Busca ────────────────────────────────────────────────────────────

@Composable
fun BuscaScreen(
    prefeitura: PrefeituraUiState,
    camara: CamaraUiState
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        // Header com SearchBar
        Box(
            Modifier
                .fillMaxWidth()
                .background(AppColors.Navy800)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    "Buscar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Blue100,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    placeholder = { Text("Procurar contratos, vereadores...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Blue500,
                        unfocusedBorderColor = AppColors.Divider,
                        cursorColor = AppColors.Blue500,
                        focusedContainerColor = AppColors.Surface,
                        unfocusedContainerColor = AppColors.Surface
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }
        }

        if (searchQuery.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Digite para buscar",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }
        } else {
            // Resultados da busca
            val query = searchQuery.lowercase()
            val contratosEncontrados = prefeitura.contratos.filter {
                it.objeto.lowercase().contains(query) || 
                it.empresa.lowercase().contains(query) ||
                it.numero.lowercase().contains(query)
            }
            val parlamentaresEncontrados = camara.parlamentares.filter {
                it.nome.lowercase().contains(query) ||
                it.partido.lowercase().contains(query)
            }

            Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
                if (contratosEncontrados.isNotEmpty()) {
                    SectionHeader("Contratos (${contratosEncontrados.size})")
                    contratosEncontrados.take(3).forEach { c ->
                        ContratosRow(c)
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                if (parlamentaresEncontrados.isNotEmpty()) {
                    SectionHeader("Vereadores (${parlamentaresEncontrados.size})")
                    parlamentaresEncontrados.take(3).forEach { p ->
                        ParlamentarRow(p)
                        HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                if (contratosEncontrados.isEmpty() && parlamentaresEncontrados.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nenhum resultado encontrado", fontSize = 13.sp,
                            color = AppColors.TextTertiary)
                    }
                }
            }
        }
    }
}
