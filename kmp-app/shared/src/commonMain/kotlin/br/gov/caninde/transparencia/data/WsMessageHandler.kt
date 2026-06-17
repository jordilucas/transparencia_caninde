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
            "REFRESHING" -> {
                val source = msg.payload?.source ?: "all"
                current.copy(
                    prefeitura = if (source == "all" || source == "prefeitura") {
                        current.prefeitura.copy(isLoading = true)
                    } else {
                        current.prefeitura
                    },
                    camara = if (source == "all" || source == "camara") {
                        current.camara.copy(isLoading = true)
                    } else {
                        current.camara
                    },
                )
            }
            "PREFEITURA_DATA" -> current.copy(
                prefeitura = msg.payload?.let { toPrefeituraUiState(it, msg.timestamp, current.prefeitura) }
                    ?: current.prefeitura,
            )
            "CAMARA_DATA" -> current.copy(
                camara = msg.payload?.let { toCamaraUiState(it, msg.timestamp, current.camara) }
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

    fun toPrefeituraUiState(
        p: WsPayload,
        timestamp: String,
        previous: PrefeituraUiState = PrefeituraUiState(),
    ) = PrefeituraUiState(
        isLoading = false,
        contratos = DataMerge.mergeContratos(previous.contratos, p.contratos ?: emptyList()),
        licitacoes = DataMerge.mergeLicitacoes(previous.licitacoes, p.licitacoes ?: emptyList()),
        gestores = if ((p.gestores ?: emptyList()).isNotEmpty()) {
            p.gestores ?: emptyList()
        } else {
            previous.gestores
        },
        diariosOficiais = DataMerge.mergeDiarios(previous.diariosOficiais, p.diariosOficiais ?: emptyList()),
        publicacoes = DataMerge.mergePublicacoes(previous.publicacoes, p.publicacoes ?: emptyList()),
        secretarias = DataMerge.mergeSecretarias(previous.secretarias, p.secretarias ?: emptyList()),
        linksTransparencia = if ((p.linksTransparencia ?: emptyList()).isNotEmpty()) {
            p.linksTransparencia ?: emptyList()
        } else {
            previous.linksTransparencia
        },
        graficos = p.graficos ?: previous.graficos,
        resumo = DataMerge.mergeResumoPrefeitura(
            previous.resumo,
            p.resumo ?: ResumoPrefeitura(),
        ),
        lastUpdated = pickNewerTimestamp(previous.lastUpdated, p.scrapedAt ?: timestamp),
        error = p.error,
    )

    fun toCamaraUiState(
        p: WsPayload,
        timestamp: String,
        previous: CamaraUiState = CamaraUiState(),
    ) = CamaraUiState(
        isLoading = false,
        parlamentares = DataMerge.mergeParlamentares(previous.parlamentares, p.parlamentares ?: emptyList()),
        sessoes = DataMerge.mergeSessoes(previous.sessoes, p.sessoes ?: emptyList()),
        materias = DataMerge.mergeMaterias(previous.materias, p.materias ?: emptyList()),
        mesaDiretora = if ((p.mesaDiretora ?: emptyList()).isNotEmpty()) {
            p.mesaDiretora ?: emptyList()
        } else {
            previous.mesaDiretora
        },
        linksTransparencia = if ((p.linksTransparencia ?: emptyList()).isNotEmpty()) {
            p.linksTransparencia ?: emptyList()
        } else {
            previous.linksTransparencia
        },
        graficos = p.graficos ?: previous.graficos,
        resumo = p.resumoCamara ?: ResumoCamara(
            totalParlamentares = (p.parlamentares ?: previous.parlamentares).size,
            totalSessoes2025 = (p.sessoes ?: previous.sessoes).size,
            totalMaterias = (p.materias ?: previous.materias).size,
        ),
        lastUpdated = pickNewerTimestamp(previous.lastUpdated, p.scrapedAt ?: timestamp),
        error = p.error,
    )

    private fun pickNewerTimestamp(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        return if (incoming >= existing) incoming else existing
    }

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
