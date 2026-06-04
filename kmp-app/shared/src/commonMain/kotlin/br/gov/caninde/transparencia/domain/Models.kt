package br.gov.caninde.transparencia.domain

import kotlinx.serialization.Serializable

// ─── Mensagens WebSocket ──────────────────────────────────────────────────────

@Serializable
data class WsMessage(
    val type: String,
    val payload: WsPayload? = null,
    val timestamp: String = ""
)

@Serializable
data class WsPayload(
    // Prefeitura
    val municipio: String? = null,
    val estado: String? = null,
    val fonte: String? = null,
    val contratos: List<Contrato>? = null,
    val licitacoes: List<Licitacao>? = null,
    val diariosOficiais: List<String>? = null,
    val secretarias: List<String>? = null,
    val resumo: ResumoPrefeitura? = null,
    val scrapedAt: String? = null,
    val error: String? = null,
    // Câmara
    val parlamentares: List<Parlamentar>? = null,
    val sessoes: List<Sessao>? = null,
    val materias: List<Materia>? = null,
    val mesaDiretora: List<MembroMesa>? = null,
    val resumoCamara: ResumoCamara? = null,
    // Status
    val version: String? = null,
    val sources: List<String>? = null,
    val intervals: Intervals? = null,
    val lastUpdated: LastUpdated? = null,
    val source: String? = null,
    val message: String? = null,
)

// ─── Prefeitura ───────────────────────────────────────────────────────────────

@Serializable
data class Contrato(
    val numero: String = "",
    val objeto: String = "",
    val valor: String = "",
    val empresa: String = "",
    val data: String = ""
)

@Serializable
data class Licitacao(
    val numero: String = "",
    val modalidade: String = "",
    val objeto: String = "",
    val situacao: String = ""
)

@Serializable
data class ResumoPrefeitura(
    val totalContratos: Int = 0,
    val totalLicitacoes: Int = 0,
    val exercicio: Int = 2025
)

// ─── Câmara ───────────────────────────────────────────────────────────────────

@Serializable
data class Parlamentar(
    val nome: String = "",
    val partido: String = "",
    val cargo: String = "",
    val foto: String = ""
)

@Serializable
data class Sessao(
    val titulo: String = "",
    val data: String = ""
)

@Serializable
data class Materia(
    val titulo: String = "",
    val tipo: String = ""
)

@Serializable
data class MembroMesa(
    val nome: String = "",
    val cargo: String = ""
)

@Serializable
data class ResumoCamara(
    val totalParlamentares: Int = 0,
    val totalSessoes2025: Int = 0,
    val totalMaterias: Int = 0
)

// ─── Auxiliares ───────────────────────────────────────────────────────────────

@Serializable
data class Intervals(
    val prefeitura: Long = 60000,
    val camara: Long = 90000
)

@Serializable
data class LastUpdated(
    val prefeitura: String? = null,
    val camara: String? = null
)

// ─── Estado da UI ─────────────────────────────────────────────────────────────

sealed class ConnectionState {
    object Connecting : ConnectionState()
    object Connected  : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class PrefeituraUiState(
    val isLoading: Boolean = true,
    val contratos: List<Contrato> = emptyList(),
    val licitacoes: List<Licitacao> = emptyList(),
    val diariosOficiais: List<String> = emptyList(),
    val secretarias: List<String> = emptyList(),
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
    val resumo: ResumoCamara = ResumoCamara(),
    val lastUpdated: String = "",
    val error: String? = null,
)
