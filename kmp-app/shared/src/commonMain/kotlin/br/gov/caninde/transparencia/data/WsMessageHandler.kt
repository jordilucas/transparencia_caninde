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
            "DETAIL_DATA" -> current.copy(
                detail = toDetailUiState(msg.payload, current.detail),
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
        publicacoes = p.publicacoes ?: emptyList(),
        secretarias = p.secretarias ?: emptyList(),
        linksTransparencia = p.linksTransparencia ?: emptyList(),
        graficos = p.graficos,
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
        linksTransparencia = p.linksTransparencia ?: emptyList(),
        graficos = p.graficos,
        resumo = p.resumoCamara ?: ResumoCamara(
            totalParlamentares = p.parlamentares?.size ?: 0,
            totalSessoes2025 = p.sessoes?.size ?: 0,
            totalMaterias = p.materias?.size ?: 0,
        ),
        lastUpdated = p.scrapedAt ?: timestamp,
        error = p.error,
    )

    fun toDetailUiState(p: WsPayload?, previous: DetailUiState): DetailUiState {
        if (p == null) return previous.copy(isLoading = false, error = "Resposta vazia")
        val err = p.error?.takeIf { it.isNotBlank() }
        return previous.copy(
            isLoading = false,
            entityId = p.entityId ?: previous.entityId,
            payload = p,
            error = err,
        )
    }

    fun entityToWs(entity: DetailEntity): String = when (entity) {
        DetailEntity.Vereador -> "vereador"
        DetailEntity.Materia -> "materia"
        DetailEntity.Secretaria -> "secretaria"
        DetailEntity.Contrato -> "contrato"
        DetailEntity.Licitacao -> "licitacao"
        DetailEntity.Sessao -> "sessao"
        DetailEntity.Gestores -> "gestores"
        DetailEntity.InstitucionalCamara -> "institucional"
        DetailEntity.InstitucionalPrefeitura -> "institucional"
    }

    fun buildRequestDetail(entity: DetailEntity, id: String): String {
        val wsEntity = entityToWs(entity)
        val resolvedId = when (entity) {
            DetailEntity.InstitucionalCamara -> "camara"
            DetailEntity.InstitucionalPrefeitura -> "prefeitura"
            DetailEntity.Gestores -> "all"
            else -> id
        }
        return """{"type":"REQUEST_DETAIL","payload":{"entity":"$wsEntity","id":"$resolvedId"}}"""
    }
}

data class WsHandlerState(
    val prefeitura: PrefeituraUiState = PrefeituraUiState(),
    val camara: CamaraUiState = CamaraUiState(),
    val detail: DetailUiState = DetailUiState(),
)
