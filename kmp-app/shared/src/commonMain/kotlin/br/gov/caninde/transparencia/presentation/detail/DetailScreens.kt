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
import br.gov.caninde.transparencia.platform.shareContent
import br.gov.caninde.transparencia.presentation.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    shareTitle: String? = null,
    shareText: String? = null,
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
            actions = {
                if (!shareText.isNullOrBlank()) {
                    IconButton(onClick = {
                        AppAnalytics.logShare(shareTitle ?: title)
                        shareContent(shareTitle ?: title, shareText)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = AppColors.Blue100)
                    }
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
fun DetailCamposExtras(campos: List<DetalheCampo>) {
    campos.filter { it.rotulo.isNotBlank() && it.valor.isNotBlank() }.forEach { campo ->
        DetailField(campo.rotulo, campo.valor)
    }
}

@Composable
fun DetailAnexos(anexos: List<DetalheAnexo>, baseUrl: String) {
    if (anexos.isEmpty()) return
    DetailSectionHeader("Documentos")
    anexos.forEach { anexo ->
        DetailLinkAction(
            label = anexo.titulo.ifBlank { "Documento" },
            url = anexo.url,
            baseUrl = baseUrl,
            usePdfIcon = anexo.extensao.equals("PDF", ignoreCase = true) || isPdfLink(anexo.url),
        )
    }
}

@Composable
fun DetailAndamentos(andamentos: List<String>) {
    if (andamentos.isEmpty()) return
    DetailSectionHeader("Andamentos")
    andamentos.forEach { item ->
        DetailBodyText(item)
    }
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
            PersonAvatar(name = parlamentar.nome, fotoUrl = parlamentar.foto, size = 72)
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
    DetailScaffold(
        title = toolbarTitle,
        onBack = onBack,
        shareTitle = p?.nome?.ifBlank { "Vereador" } ?: "Vereador",
        shareText = p?.let { ShareTexts.vereador(it) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Vereador, slug) }
        p?.let { vereador ->
            VereadorProfileHeader(vereador)
            if (vereador.legislatura.isNotBlank()) {
                DetailField("Legislatura", vereador.legislatura)
            }
            if (vereador.vinculo.isNotBlank()) {
                DetailField("Vínculo", vereador.vinculo)
            }
            val mandato = listOfNotNull(
                vereador.mandatoInicio.takeIf { it.isNotBlank() },
                vereador.mandatoFim.takeIf { it.isNotBlank() },
            ).joinToString(" — ")
            if (mandato.isNotBlank()) {
                DetailField("Mandato", mandato)
            }
            if (vereador.naturalidade.isNotBlank()) {
                DetailField("Naturalidade", vereador.naturalidade)
            }
            if (vereador.dataNascimento.isNotBlank()) {
                DetailField("Data de nascimento", vereador.dataNascimento)
            }
            if (vereador.estadoCivil.isNotBlank()) {
                DetailField("Estado civil", vereador.estadoCivil)
            }
            if (vereador.totalMaterias > 0 || vereador.totalSessoes > 0) {
                DetailSectionHeader("Produção legislativa")
                if (vereador.totalMaterias > 0) {
                    DetailField("Matérias", "${vereador.totalMaterias}")
                }
                if (vereador.totalSessoes > 0) {
                    DetailField("Sessões presentes", "${vereador.totalSessoes}")
                }
            }
            if (vereador.sessoesPresentes.isNotEmpty()) {
                DetailSectionHeader("Sessões recentes")
                vereador.sessoesPresentes.forEach { sessao ->
                    if (sessao.url.isNotBlank()) {
                        DetailLinkAction(
                            label = sessao.titulo,
                            url = sessao.url,
                            baseUrl = CAMARA_PORTAL_BASE,
                            actionText = sessao.titulo,
                            usePdfIcon = false,
                        )
                    } else {
                        DetailField(sessao.titulo, sessao.data.ifBlank { "—" })
                    }
                }
            }
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
    DetailScaffold(
        title = truncateToolbarTitle(m?.titulo ?: "Matéria"),
        onBack = onBack,
        shareTitle = m?.titulo ?: "Matéria",
        shareText = m?.let { ShareTexts.materia(it) },
    ) {
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
fun SecretariaDetailScreen(
    viewModel: TransparenciaViewModel,
    id: String,
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit = {},
) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(id) { viewModel.loadDetail(DetailEntity.Secretaria, id) }
    val s = state.payload?.secretaria
    DetailScaffold(
        title = s?.nome ?: "Secretaria",
        onBack = onBack,
        shareTitle = s?.nome ?: "Secretaria",
        shareText = s?.let { ShareTexts.secretaria(it) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Secretaria, id) }
        s?.let { secretaria ->
            if (secretaria.secretario.isNotBlank()) {
                val gestor = if (secretaria.cargoGestor.isNotBlank()) {
                    "${secretaria.secretario} · ${secretaria.cargoGestor}"
                } else {
                    secretaria.secretario
                }
                DetailField("Secretário(a) atual", gestor)
            }
            val resumo = secretaria.resumoFinanceiro
            if (resumo.totalGastos.isNotBlank()
                || resumo.totalContratos > 0
                || resumo.totalLicitacoes > 0
            ) {
                DetailSectionHeader("Resumo financeiro")
                if (resumo.totalGastos.isNotBlank()) DetailField("Total em contratos", resumo.totalGastos)
                if (resumo.totalContratos > 0) DetailField("Contratos", "${resumo.totalContratos}")
                if (resumo.totalLicitacoes > 0) DetailField("Licitações vinculadas", "${resumo.totalLicitacoes}")
            }
            if (secretaria.projetosAndamento.isNotEmpty()) {
                DetailSectionHeader("Projetos em andamento")
                secretaria.projetosAndamento.forEach { projeto ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (projeto.url.isNotBlank() || projeto.numero.isNotBlank()) {
                                    Modifier.clickable { onNavigate(routeFromExternalUrl(projeto.url)) }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                projeto.titulo.ifBlank { projeto.numero },
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = AppColors.TextPrimary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    projeto.tipo.takeIf { it.isNotBlank() },
                                    projeto.situacao.takeIf { it.isNotBlank() },
                                    projeto.valor.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary,
                            )
                        }
                    }
                }
            }
            if (secretaria.licitacoes.isNotEmpty()) {
                DetailSectionHeader("Licitações")
                secretaria.licitacoes.forEach { lic ->
                    val info = lic.displayInfo()
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(routeFromLicitacao(lic)) },
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(info.titulo, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (info.descricao.isNotBlank()) {
                                Text(info.descricao, fontSize = 11.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
            if (secretaria.contratos.isNotEmpty()) {
                DetailSectionHeader("Contratos")
                secretaria.contratos.forEach { contrato ->
                    val info = contrato.normalized().displayInfo()
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(routeFromContrato(contrato)) },
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(info.titulo, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (contrato.valor.isNotBlank()) {
                                Text(contrato.valor, fontSize = 12.sp, color = AppColors.Green700, fontWeight = FontWeight.SemiBold)
                            }
                            if (info.descricao.isNotBlank()) {
                                Text(info.descricao, fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            DetailSectionHeader("Contato")
            ContatoSection(secretaria.contato)
            if (secretaria.url.isNotBlank()) {
                DetailPortalLink(secretaria.url, PREFEITURA_PORTAL_BASE)
            }
        }
    }
}

@Composable
fun PublicacaoDetailScreen(viewModel: TransparenciaViewModel, id: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(id) { viewModel.loadDetail(DetailEntity.Publicacao, id) }
    val p = state.payload?.publicacao
    DetailScaffold(
        title = p?.titulo?.ifBlank { "Publicação" } ?: "Publicação",
        onBack = onBack,
        shareTitle = p?.titulo ?: "Publicação",
        shareText = p?.let { ShareTexts.publicacao(it) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Publicacao, id) }
        p?.let { pub ->
            if (pub.tipo.isNotBlank()) DetailField("Tipo", pub.tipo)
            if (pub.data.isNotBlank()) DetailField("Data", pub.data)
            if (pub.resumo.isNotBlank()) {
                DetailSectionHeader("Descrição")
                DetailBodyText(pub.resumo)
            }
            DetailCamposExtras(pub.camposExtras)
            val anexos = pub.anexos.ifEmpty {
                val doc = pub.linkArquivo.ifBlank { pub.url }
                if (doc.isNotBlank() && isPdfLink(doc)) {
                    listOf(DetalheAnexo(titulo = "Documento", url = doc, extensao = "PDF"))
                } else {
                    emptyList()
                }
            }
            DetailAnexos(anexos, PREFEITURA_PORTAL_BASE)
            if (pub.url.isNotBlank()) DetailPortalLink(pub.url, PREFEITURA_PORTAL_BASE)
        }
    }
}

@Composable
fun DocumentoCamaraDetailScreen(viewModel: TransparenciaViewModel, pageId: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(pageId) { viewModel.loadDetail(DetailEntity.DocumentoCamara, pageId) }
    val doc = state.payload?.documentoCamara
    DetailScaffold(
        title = truncateToolbarTitle(doc?.titulo?.ifBlank { "Documento" } ?: "Documento"),
        onBack = onBack,
        shareTitle = doc?.titulo ?: "Documento Câmara",
        shareText = doc?.url?.let { ShareTexts.paginaPortal(doc.titulo, pageId) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.DocumentoCamara, pageId) }
        doc?.let { d ->
            if (d.categoria.isNotBlank()) DetailField("Tipo", d.categoria.replaceFirstChar { it.uppercase() })
            if (d.data.isNotBlank()) DetailField("Data", d.data)
            if (d.resumo.isNotBlank()) {
                DetailSectionHeader("Resumo")
                DetailBodyText(d.resumo)
            }
            DetailCamposExtras(d.camposExtras)
            DetailAnexos(d.anexos, CAMARA_PORTAL_BASE)
            if (d.url.isNotBlank()) DetailPortalLink(d.url, CAMARA_PORTAL_BASE)
        }
    }
}

@Composable
fun PaginaPortalDetailScreen(viewModel: TransparenciaViewModel, pageId: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(pageId) { viewModel.loadDetail(DetailEntity.PaginaPortal, pageId) }
    val page = state.payload?.paginaPortal
    val toolbarTitle = truncateToolbarTitle(page?.titulo?.ifBlank { "Transparência" } ?: "Transparência")
    DetailScaffold(
        title = toolbarTitle,
        onBack = onBack,
        shareTitle = page?.titulo ?: "Transparência",
        shareText = page?.titulo?.let { ShareTexts.paginaPortal(it, pageId) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.PaginaPortal, pageId) }
        page?.let { pg ->
            if (pg.categoria.isNotBlank()) DetailField("Categoria", pg.categoria.replaceFirstChar { it.uppercase() })
            if (pg.aviso.isNotBlank()) {
                DataStatusBanner(error = pg.aviso)
            }
            if (pg.resumo.isNotBlank()) {
                DetailSectionHeader("Resumo")
                DetailBodyText(pg.resumo)
            }
            DetailCamposExtras(pg.camposExtras)
            DetailAnexos(pg.anexos, portalBaseUrl(pg.origem))
            if (pg.url.isNotBlank()) DetailPortalLink(pg.url, portalBaseUrl(pg.origem))
        }
    }
}

@Composable
fun ContratoDetailScreen(viewModel: TransparenciaViewModel, numero: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(numero) { viewModel.loadDetail(DetailEntity.Contrato, numero) }
    val c = state.payload?.contrato?.normalized()
    val info = c?.displayInfo()
    DetailScaffold(
        title = info?.titulo ?: "Contrato $numero",
        onBack = onBack,
        shareTitle = info?.titulo ?: "Contrato",
        shareText = c?.let { ShareTexts.contrato(it) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Contrato, numero) }
        c?.let {
            if (it.valor.isNotBlank()) DetailField("Valor", it.valor)
            if (it.objeto.isNotBlank()) DetailField("Descrição", it.objeto)
            if (it.empresa.isNotBlank()) DetailField("Empresa", it.empresa)
            if (it.cnpjCredor.isNotBlank()) DetailField("CNPJ/CPF", it.cnpjCredor)
            if (it.secretaria.isNotBlank()) DetailField("Secretaria", it.secretaria)
            DetailField("Número", it.numero.replace("CONTRATO ORIGINAL", "", ignoreCase = true).trim().ifBlank { "—" })
            if (it.modalidade.isNotBlank()) DetailField("Modalidade", it.modalidade)
            if (it.dataPublicacao.isNotBlank()) DetailField("Publicação", it.dataPublicacao)
            if (it.vigenciaInicio.isNotBlank() || it.vigenciaFim.isNotBlank()) {
                DetailField(
                    "Vigência",
                    listOf(it.vigenciaInicio, it.vigenciaFim).filter { p -> p.isNotBlank() }.joinToString(" — "),
                )
            } else if (it.data.isNotBlank()) {
                DetailField("Vigência", it.data)
            }
            if (it.vigenciaStatus.isNotBlank()) DetailField("Situação", it.vigenciaStatus)
            DetailCamposExtras(it.camposExtras)
            val anexos = it.anexos.ifEmpty {
                val docUrl = it.pdfUrl.ifBlank { it.url }
                if (docUrl.isNotBlank()) listOf(DetalheAnexo(titulo = "Documento original", url = docUrl, extensao = "PDF"))
                else emptyList()
            }
            DetailAnexos(anexos, PREFEITURA_PORTAL_BASE)
        }
    }
}

@Composable
fun LicitacaoDetailScreen(viewModel: TransparenciaViewModel, numero: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(numero) { viewModel.loadDetail(DetailEntity.Licitacao, numero) }
    val l = state.payload?.licitacao
    val info = l?.displayInfo()
    DetailScaffold(
        title = info?.titulo?.take(48) ?: "Licitação $numero",
        onBack = onBack,
        shareTitle = info?.titulo ?: "Licitação",
        shareText = l?.let { ShareTexts.licitacao(it) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Licitacao, numero) }
        l?.let {
            val display = it.displayInfo()
            if (display.descricao.isNotBlank()) DetailField("Modalidade / processo", display.descricao)
            DetailField("Número", it.numero.ifBlank { "—" })
            DetailField("Situação", display.situacao)
            if (display.meta.isNotBlank()) DetailField("Abertura", display.meta)
            if (it.horaAbertura.isNotBlank()) DetailField("Horário", it.horaAbertura)
            if (it.valorEstimado.isNotBlank()) DetailField("Valor estimado", it.valorEstimado)
            if (it.tipoJulgamento.isNotBlank()) DetailField("Tipo de julgamento", it.tipoJulgamento)
            if (it.plataformaEletronica.isNotBlank()) DetailField("Plataforma eletrônica", it.plataformaEletronica)
            if (it.objeto.isNotBlank() && it.objeto != display.titulo) DetailField("Objeto", it.objeto)
            DetailCamposExtras(it.camposExtras)
            DetailAndamentos(it.andamentos)
            DetailAnexos(it.anexos, PREFEITURA_PORTAL_BASE)
            if (it.anexos.isEmpty() && it.url.isNotBlank()) {
                DetailPortalLink(it.url, PREFEITURA_PORTAL_BASE)
            }
        }
    }
}

@Composable
fun SessaoDetailScreen(viewModel: TransparenciaViewModel, id: String, onBack: () -> Unit) {
    val state by viewModel.detailState.collectAsState()
    LaunchedEffect(id) { viewModel.loadDetail(DetailEntity.Sessao, id) }
    val s = state.payload?.sessao
    DetailScaffold(
        title = truncateToolbarTitle(s?.titulo ?: "Sessão"),
        onBack = onBack,
        shareTitle = s?.titulo ?: "Sessão",
        shareText = s?.let { ShareTexts.sessao(it, id) },
    ) {
        DetailLoadingOrError(state) { viewModel.loadDetail(DetailEntity.Sessao, id) }
        s?.let {
            if (it.data.isNotBlank()) DetailField("Data", it.data)
            SessionVideoPlayer(
                embedUrl = it.videoEmbedUrl,
                watchUrl = it.videoUrl(),
            )
            if (it.resumo.isNotBlank()) {
                DetailSectionHeader("Resumo")
                DetailBodyText(it.resumo)
            }
            DetailCamposExtras(it.camposExtras)
            DetailAnexos(it.anexos, CAMARA_PORTAL_BASE)
            if (it.anexos.isEmpty() && it.url.isNotBlank()) DetailPortalLink(it.url, CAMARA_PORTAL_BASE)
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

@Composable
fun ObraDetailScreen(obras: List<Obra>, id: String, onBack: () -> Unit) {
    val obra = obras.find { it.id == id || it.titulo == id }
    DetailScaffold(
        title = truncateToolbarTitle(obra?.titulo ?: "Obra"),
        onBack = onBack,
    ) {
        if (obra == null) {
            EmptyState("Obra não encontrada na listagem atual.")
            return@DetailScaffold
        }
        if (obra.secretaria.isNotBlank()) DetailField("Secretaria", obra.secretaria)
        if (obra.situacao.isNotBlank()) DetailField("Situação", obra.situacao)
        if (obra.valor.isNotBlank()) DetailField("Valor", obra.valor)
        if (obra.data.isNotBlank()) DetailField("Data", obra.data)
        if (obra.descricao.isNotBlank()) {
            DetailSectionHeader("Descrição")
            DetailBodyText(obra.descricao)
        }
        val docUrl = obra.url
        if (docUrl.isNotBlank()) {
            DetailLinkAction(
                label = "Documento / portal",
                url = docUrl,
                baseUrl = PREFEITURA_PORTAL_BASE,
                actionText = "Abrir no portal oficial",
                usePdfIcon = docUrl.contains(".pdf", ignoreCase = true),
            )
        }
    }
}
