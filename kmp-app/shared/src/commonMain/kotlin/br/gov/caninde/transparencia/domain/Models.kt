package br.gov.caninde.transparencia.domain

import kotlinx.serialization.Serializable

// ─── Mensagens WebSocket ──────────────────────────────────────────────────────

@Serializable
data class WsMessage(
    val type: String,
    val payload: WsPayload? = null,
    val timestamp: String = "",
)

@Serializable
data class WsPayload(
    val municipio: String? = null,
    val estado: String? = null,
    val fonte: String? = null,
    val contratos: List<Contrato>? = null,
    val licitacoes: List<Licitacao>? = null,
    val diariosOficiais: List<String>? = null,
    val secretarias: List<Secretaria>? = null,
    val publicacoes: List<Publicacao>? = null,
    val linksTransparencia: List<LinkExterno>? = null,
    val resumo: ResumoPrefeitura? = null,
    val resumoCamara: ResumoCamara? = null,
    val scrapedAt: String? = null,
    val error: String? = null,
    val parlamentares: List<Parlamentar>? = null,
    val sessoes: List<Sessao>? = null,
    val materias: List<Materia>? = null,
    val mesaDiretora: List<MembroMesa>? = null,
    val graficos: GraficosPayload? = null,
    val entity: String? = null,
    val entityId: String? = null,
    val gestores: List<Gestor>? = null,
    val institucional: Institucional? = null,
    val parlamentar: Parlamentar? = null,
    val materia: Materia? = null,
    val secretaria: Secretaria? = null,
    val contrato: Contrato? = null,
    val licitacao: Licitacao? = null,
    val sessao: Sessao? = null,
    val version: String? = null,
    val sources: List<String>? = null,
    val intervals: Intervals? = null,
    val lastUpdated: LastUpdated? = null,
    val source: String? = null,
    val message: String? = null,
)

@Serializable
data class Contato(
    val email: String = "",
    val telefone: String = "",
    val whatsapp: String = "",
    val endereco: String = "",
    val horarioFuncionamento: String = "",
)

@Serializable
data class Institucional(
    val orgao: String = "",
    val endereco: String = "",
    val contato: Contato = Contato(),
    val siteUrl: String = "",
)

@Serializable
data class Gestor(
    val nome: String = "",
    val cargo: String = "",
    val foto: String = "",
    val contato: Contato = Contato(),
)

@Serializable
data class Contrato(
    val id: String = "",
    val numero: String = "",
    val objeto: String = "",
    val valor: String = "",
    val empresa: String = "",
    val data: String = "",
    val url: String = "",
    val cnpjCredor: String = "",
    val secretaria: String = "",
    val modalidade: String = "",
    val pdfUrl: String = "",
)

@Serializable
data class Licitacao(
    val id: String = "",
    val numero: String = "",
    val modalidade: String = "",
    val objeto: String = "",
    val situacao: String = "",
    val url: String = "",
    val dataAbertura: String = "",
)

@Serializable
data class Publicacao(
    val id: String = "",
    val titulo: String = "",
    val tipo: String = "",
    val data: String = "",
    val url: String = "",
)

@Serializable
data class LinkExterno(
    val titulo: String = "",
    val url: String = "",
    val categoria: String = "",
)

@Serializable
data class Secretaria(
    val id: String = "",
    val nome: String = "",
    val secretario: String = "",
    val url: String = "",
    val contato: Contato = Contato(),
)

@Serializable
data class ResumoPrefeitura(
    val totalContratos: Int = 0,
    val totalLicitacoes: Int = 0,
    val totalPublicacoes: Int = 0,
    val exercicio: Int = 2025,
    val fontesUtilizadas: List<String> = emptyList(),
)

@Serializable
data class Parlamentar(
    val nome: String = "",
    val nomeCompleto: String = "",
    val partido: String = "",
    val cargo: String = "",
    val foto: String = "",
    val slug: String = "",
    val profileUrl: String = "",
    val contato: Contato = Contato(),
    val biografia: String = "",
)

@Serializable
data class Sessao(
    val titulo: String = "",
    val data: String = "",
    val url: String = "",
    val resumo: String = "",
)

@Serializable
data class Materia(
    val titulo: String = "",
    val tipo: String = "",
    val slug: String = "",
    val url: String = "",
    val autor: String = "",
    val dataPublicacao: String = "",
    val pdfUrl: String = "",
    val resumo: String = "",
)

@Serializable
data class MembroMesa(
    val nome: String = "",
    val cargo: String = "",
)

@Serializable
data class ResumoCamara(
    val totalParlamentares: Int = 0,
    val totalSessoes2025: Int = 0,
    val totalMaterias: Int = 0,
)

@Serializable
data class ChartSeries(
    val titulo: String = "",
    val labels: List<String> = emptyList(),
    val valores: List<Int> = emptyList(),
)

@Serializable
data class GraficosPayload(
    val prefeitura: List<ChartSeries> = emptyList(),
    val camara: List<ChartSeries> = emptyList(),
)

@Serializable
data class Intervals(
    val prefeitura: Long = 60000,
    val camara: Long = 90000,
)

@Serializable
data class LastUpdated(
    val prefeitura: String? = null,
    val camara: String? = null,
)

// ─── Detalhe (UI) ───────────────────────────────────────────────────────────

enum class DetailEntity {
    Vereador, Materia, Secretaria, Contrato, Licitacao, Sessao, Gestores, InstitucionalCamara, InstitucionalPrefeitura,
}

data class DetailUiState(
    val isLoading: Boolean = false,
    val entity: DetailEntity? = null,
    val entityId: String = "",
    val payload: WsPayload? = null,
    val error: String? = null,
)

sealed class ConnectionState {
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
    object Error : ConnectionState()
}

data class PrefeituraUiState(
    val isLoading: Boolean = true,
    val contratos: List<Contrato> = emptyList(),
    val licitacoes: List<Licitacao> = emptyList(),
    val diariosOficiais: List<String> = emptyList(),
    val publicacoes: List<Publicacao> = emptyList(),
    val secretarias: List<Secretaria> = emptyList(),
    val linksTransparencia: List<LinkExterno> = emptyList(),
    val graficos: GraficosPayload? = null,
    val resumo: ResumoPrefeitura = ResumoPrefeitura(),
    val lastUpdated: String = "",
    val error: String? = null,
)

data class CamaraUiState(
    val isLoading: Boolean = true,
    val parlamentares: List<Parlamentar> = emptyList(),
    val sessoes: List<Sessao> = emptyList(),
    val materias: List<Materia> = emptyList(),
    val mesaDiretora: List<MembroMesa> = emptyList(),
    val linksTransparencia: List<LinkExterno> = emptyList(),
    val graficos: GraficosPayload? = null,
    val resumo: ResumoCamara = ResumoCamara(),
    val lastUpdated: String = "",
    val error: String? = null,
)
