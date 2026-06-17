package br.gov.caninde.transparencia.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.platform.openExternalUrl
import br.gov.caninde.transparencia.presentation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        TopAppBar(
            windowInsets = WindowInsets.statusBars,
            title = {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppColors.Navy800,
                titleContentColor = AppColors.Blue100,
                navigationIconContentColor = AppColors.Blue100,
            ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
fun DetailBodyText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = AppColors.TextSecondary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun DetailPortalLink(url: String, baseUrl: String = CAMARA_PORTAL_BASE) {
    DetailLinkAction(
        label = "Portal",
        url = url,
        baseUrl = baseUrl,
        actionText = "Abrir no portal",
        usePdfIcon = false,
    )
}

/** Link ou PDF clicável — abre em app externo (navegador / visualizador de PDF). */
@Composable
fun DetailLinkAction(
    label: String,
    url: String,
    baseUrl: String = CAMARA_PORTAL_BASE,
    actionText: String? = null,
    usePdfIcon: Boolean? = null,
) {
    val resolved = remember(url, baseUrl) { resolveAbsoluteUrl(url, baseUrl) }
    if (resolved.isBlank()) return
    val isPdf = usePdfIcon ?: isPdfLink(resolved)
    val text = actionText ?: if (isPdf) pdfLinkLabel(resolved) else "Abrir link"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, color = AppColors.TextTertiary, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { openExternalUrl(resolved) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (isPdf) Icons.Default.PictureAsPdf else Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = AppColors.Blue500,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text,
                fontSize = 13.sp,
                color = AppColors.Blue500,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun VereadorProfileHeader(parlamentar: Parlamentar) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InitialAvatar(parlamentar.nome, size = 72)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = parlamentar.nomeCompleto.ifBlank { parlamentar.nome },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (parlamentar.cargo.isNotBlank()) {
                    Text(
                        parlamentar.cargo,
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (parlamentar.partido.isNotBlank()) {
                    Text(
                        parlamentar.partido,
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
fun DetailLoadingOrError(
    state: DetailUiState,
    onRetry: (() -> Unit)? = null,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.Blue500)
        }
        return
    }
    state.error?.let { err ->
        DataStatusBanner(error = err)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

@Composable
fun WhatsappContactRow(raw: String) {
    if (raw.isBlank()) return
    val parsed = remember(raw) { parseWhatsapp(raw) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("WhatsApp", fontSize = 10.sp, color = AppColors.TextTertiary, fontWeight = FontWeight.Medium)
        if (parsed != null) {
            Text(
                parsed.displayLabel,
                fontSize = 13.sp,
                color = AppColors.Blue500,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { openExternalUrl(parsed.openUrl) },
            )
        } else {
            Text(
                "Não informado no portal",
                fontSize = 12.sp,
                color = AppColors.TextTertiary,
            )
        }
    }
}

@Composable
fun ContatoSection(contato: Contato) {
    val hasWhatsapp = parseWhatsapp(contato.whatsapp) != null
    if (contato.email.isNotBlank()) DetailField("E-mail", contato.email)
    if (contato.telefone.isNotBlank()) DetailField("Telefone", contato.telefone)
    if (contato.whatsapp.isNotBlank()) WhatsappContactRow(contato.whatsapp)
    if (contato.endereco.isNotBlank()) DetailField("Endereço", contato.endereco)
    if (contato.horarioFuncionamento.isNotBlank()) DetailField("Horário", contato.horarioFuncionamento)
    if (contato.email.isBlank() && contato.telefone.isBlank() && !hasWhatsapp && contato.whatsapp.isBlank()
        && contato.endereco.isBlank() && contato.horarioFuncionamento.isBlank()
    ) {
        Text(
            "Não informado no portal",
            fontSize = 12.sp,
            color = AppColors.TextTertiary,
        )
    }
}

@Composable
fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, color = AppColors.TextTertiary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = AppColors.TextPrimary)
    }
}

@Composable
fun VereadorDetailScreen(viewModel: TransparenciaViewModel, slug: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadDetail(DetailEntity.Vereador, slug) }
    val p = state.payload?.parlamentar
    val toolbarTitle = truncateToolbarTitle(p?.nome?.ifBlank { "Vereador" } ?: "Vereador")
    DetailScaffold(title = toolbarTitle, onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Vereador, slug) }
        p?.let { vereador ->
            VereadorProfileHeader(vereador)
            DetailSectionHeader("Contato")
            ContatoSection(vereador.contato)
            val biografia = sanitizeBiography(vereador.biografia)
            if (biografia.isNotBlank()) {
                DetailSectionHeader("Biografia")
                DetailBodyText(biografia)
            }
            if (vereador.profileUrl.isNotBlank()) {
                DetailPortalLink(vereador.profileUrl)
            }
        }
    }
}

@Composable
fun MateriaDetailScreen(viewModel: TransparenciaViewModel, slug: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(slug) { viewModel.loadDetail(DetailEntity.Materia, slug) }
    val m = state.payload?.materia
    DetailScaffold(title = truncateToolbarTitle(m?.titulo ?: "Matéria"), onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Materia, slug) }
        m?.let {
            if (it.tipo.isNotBlank()) DetailField("Tipo", it.tipo)
            if (it.autor.isNotBlank()) DetailField("Autor", it.autor)
            if (it.dataPublicacao.isNotBlank()) DetailField("Publicação", it.dataPublicacao)
            if (it.resumo.isNotBlank()) DetailField("Resumo", it.resumo)
            if (it.pdfUrl.isNotBlank()) {
                DetailLinkAction(
                    label = "Documento PDF",
                    url = it.pdfUrl,
                    baseUrl = CAMARA_PORTAL_BASE,
                    usePdfIcon = true,
                )
            }
            if (it.url.isNotBlank()) DetailPortalLink(it.url, CAMARA_PORTAL_BASE)
        }
    }
}

@Composable
fun SecretariaDetailScreen(viewModel: TransparenciaViewModel, id: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(id) { viewModel.loadDetail(DetailEntity.Secretaria, id) }
    val s = state.payload?.secretaria
    DetailScaffold(title = s?.nome ?: "Secretaria", onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Secretaria, id) }
        s?.let {
            if (it.secretario.isNotBlank()) DetailField("Secretário(a)", it.secretario)
            DetailSectionHeader("Contato")
            ContatoSection(it.contato)
            if (it.url.isNotBlank()) DetailPortalLink(it.url, PREFEITURA_PORTAL_BASE)
        }
    }
}

@Composable
fun ContratoDetailScreen(viewModel: TransparenciaViewModel, numero: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(numero) { viewModel.loadDetail(DetailEntity.Contrato, numero) }
    val c = state.payload?.contrato?.normalized()
    val info = c?.displayInfo()
    DetailScaffold(title = info?.titulo ?: "Contrato $numero", onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Contrato, numero) }
        c?.let {
            if (it.valor.isNotBlank()) DetailField("Valor", it.valor)
            if (it.objeto.isNotBlank()) DetailField("Descrição", it.objeto)
            if (it.empresa.isNotBlank()) DetailField("Empresa", it.empresa)
            if (it.cnpjCredor.isNotBlank()) DetailField("CNPJ/CPF", it.cnpjCredor)
            DetailField("Número", it.numero.replace("CONTRATO ORIGINAL", "", ignoreCase = true).trim().ifBlank { "—" })
            if (it.modalidade.isNotBlank()) DetailField("Modalidade", it.modalidade)
            DetailField("Vigência", it.data.ifBlank { "—" })
            val docUrl = it.pdfUrl.ifBlank { it.url }
            if (docUrl.isNotBlank()) {
                val resolved = resolveAbsoluteUrl(docUrl, PREFEITURA_PORTAL_BASE)
                DetailLinkAction(
                    label = if (isPdfLink(resolved)) "Documento PDF" else "Ver no portal",
                    url = docUrl,
                    baseUrl = PREFEITURA_PORTAL_BASE,
                )
            }
        }
    }
}

@Composable
fun LicitacaoDetailScreen(viewModel: TransparenciaViewModel, numero: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(numero) { viewModel.loadDetail(DetailEntity.Licitacao, numero) }
    val l = state.payload?.licitacao
    val info = l?.displayInfo()
    DetailScaffold(title = info?.titulo?.take(48) ?: "Licitação $numero", onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Licitacao, numero) }
        l?.let {
            val display = it.displayInfo()
            if (display.descricao.isNotBlank()) DetailField("Modalidade / processo", display.descricao)
            DetailField("Número", it.numero.ifBlank { "—" })
            DetailField("Situação", display.situacao)
            if (display.meta.isNotBlank()) DetailField("Abertura", display.meta)
            if (it.objeto.isNotBlank() && it.objeto != display.titulo) DetailField("Objeto", it.objeto)
            if (it.url.isNotBlank()) {
                val resolved = resolveAbsoluteUrl(it.url, PREFEITURA_PORTAL_BASE)
                DetailLinkAction(
                    label = if (isPdfLink(resolved)) "Documento PDF" else "Link",
                    url = it.url,
                    baseUrl = PREFEITURA_PORTAL_BASE,
                )
            }
        }
    }
}

@Composable
fun SessaoDetailScreen(viewModel: TransparenciaViewModel, id: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(id) { viewModel.loadDetail(DetailEntity.Sessao, id) }
    val s = state.payload?.sessao
    DetailScaffold(title = truncateToolbarTitle(s?.titulo ?: "Sessão"), onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Sessao, id) }
        s?.let {
            if (it.data.isNotBlank()) DetailField("Data", it.data)
            if (it.resumo.isNotBlank()) DetailField("Resumo", it.resumo)
            if (it.url.isNotBlank()) DetailPortalLink(it.url, CAMARA_PORTAL_BASE)
        }
    }
}

@Composable
fun GestoresDetailScreen(viewModel: TransparenciaViewModel, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadDetail(DetailEntity.Gestores, "all") }
    DetailScaffold(title = "Prefeito e Vice", onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Gestores, "all") }
        val gestores = state.payload?.gestores.orEmpty()
        if (gestores.isEmpty() && !state.isLoading && state.error.isNullOrBlank()) {
            EmptyState("Nenhum gestor encontrado no portal")
        }
        gestores.forEach { g ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(g.nome, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppColors.Navy800)
                    Text(g.cargo, fontSize = 12.sp, color = AppColors.TextSecondary)
                    ContatoSection(g.contato)
                }
            }
        }
    }
}

@Composable
fun InstitucionalDetailScreen(
    viewModel: TransparenciaViewModel,
    camara: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.detailState.collectAsState()
    val entity = if (camara) DetailEntity.InstitucionalCamara else DetailEntity.InstitucionalPrefeitura
    LaunchedEffect(camara) { viewModel.loadDetail(entity, if (camara) "camara" else "prefeitura") }
    val inst = state.payload?.institucional
    DetailScaffold(title = inst?.orgao ?: "Institucional", onBack = onBack) {
        DetailLoadingOrError(state) { viewModel.loadDetail(entity, if (camara) "camara" else "prefeitura") }
        inst?.let {
            if (it.endereco.isNotBlank()) DetailField("Endereço", it.endereco)
            DetailSectionHeader("Contato")
            ContatoSection(it.contato)
            if (it.siteUrl.isNotBlank()) DetailPortalLink(it.siteUrl)
        }
    }
}

@Composable
fun ChartBarSection(series: ChartSeries) {
    if (series.labels.isEmpty()) return
    val max = (series.valores.maxOrNull() ?: 1).coerceAtLeast(1)
    DetailSectionHeader(series.titulo)
    series.labels.zip(series.valores).forEach { (label, value) ->
        val pct = value.toFloat() / max
        ProgressRow(label, pct, AppColors.Blue500)
        Text("$value", fontSize = 10.sp, color = AppColors.TextTertiary, modifier = Modifier.padding(start = 106.dp))
    }
}
