package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.*
import kotlinx.serialization.json.Json

/**
 * Lógica pura de parse e atualização de estado a partir de mensagens WS (testável sem rede).
 */
class WsMessageHandler(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    },
) {
    fun parse(raw: String): WsMessage = json.decodeFromString(raw)

    fun reduce(
        current: WsHandlerState,
        raw: String,
    ): WsHandlerState {
        val msg = parse(raw)
        return reduce(current, msg)
    }

    fun reduce(current: WsHandlerState, msg: WsMessage): WsHandlerState {
        return when (msg.type) {
            "PREFEITURA_DATA" -> current.copy(
                prefeitura = msg.payload?.let { toPrefeituraUiState(it, msg.timestamp) }
                    ?: current.prefeitura,
            )
            "CAMARA_DATA" -> current.copy(
                camara = msg.payload?.let { toCamaraUiState(it, msg.timestamp) }
                    ?: current.camara,
            )
            "ERROR" -> {
                val errorMsg = msg.payload?.message ?: "Erro no servidor"
                current.copy(
                    prefeitura = current.prefeitura.copy(error = errorMsg),
                    camara = current.camara.copy(error = errorMsg),
                )
            }
            else -> current
        }
    }

    fun toPrefeituraUiState(p: WsPayload, timestamp: String) = PrefeituraUiState(
        isLoading = false,
        contratos = p.contratos ?: emptyList(),
        licitacoes = p.licitacoes ?: emptyList(),
        diariosOficiais = p.diariosOficiais ?: emptyList(),
        secretarias = p.secretarias ?: emptyList(),
        resumo = p.resumo ?: ResumoPrefeitura(),
        lastUpdated = p.scrapedAt ?: timestamp,
        error = p.error,
    )

    fun toCamaraUiState(p: WsPayload, timestamp: String) = CamaraUiState(
        isLoading = false,
        parlamentares = p.parlamentares ?: emptyList(),
        sessoes = p.sessoes ?: emptyList(),
        materias = p.materias ?: emptyList(),
        mesaDiretora = p.mesaDiretora ?: emptyList(),
        resumo = ResumoCamara(
            totalParlamentares = p.parlamentares?.size ?: 0,
            totalSessoes2025 = p.sessoes?.size ?: 0,
            totalMaterias = p.materias?.size ?: 0,
        ),
        lastUpdated = p.scrapedAt ?: timestamp,
        error = p.error,
    )
}

data class WsHandlerState(
    val prefeitura: PrefeituraUiState = PrefeituraUiState(),
    val camara: CamaraUiState = CamaraUiState(),
)
