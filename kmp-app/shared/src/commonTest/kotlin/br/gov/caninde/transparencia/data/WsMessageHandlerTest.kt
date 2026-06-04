package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WsMessageHandlerTest {

    private val handler = WsMessageHandler()

    @Test
    fun parsePrefeituraDataAtualizaEstado() {
        val raw = """
            {
              "type": "PREFEITURA_DATA",
              "timestamp": "2025-06-04T12:00:00Z",
              "payload": {
                "contratos": [{"numero": "001/2025", "objeto": "Teste", "valor": "R$ 1", "empresa": "X", "data": "01/01/2025"}],
                "scrapedAt": "2025-06-04T12:00:00Z"
              }
            }
        """.trimIndent()

        val reduced = handler.reduce(WsHandlerState(), raw)
        assertEquals(1, reduced.prefeitura.contratos.size)
        assertEquals("001/2025", reduced.prefeitura.contratos.first().numero)
        assertNull(reduced.prefeitura.error)
    }

    @Test
    fun parseCamaraDataSemListasInventadas() {
        val raw = """
            {
              "type": "CAMARA_DATA",
              "timestamp": "2025-06-04T12:00:00Z",
              "payload": {
                "parlamentares": [{"nome": "Karlinda Coelho", "partido": "REP", "cargo": "Presidente", "foto": ""}],
                "error": "Alguns dados não foram carregados: sessões."
              }
            }
        """.trimIndent()

        val reduced = handler.reduce(WsHandlerState(), raw)
        assertEquals(1, reduced.camara.parlamentares.size)
        assertEquals(0, reduced.camara.sessoes.size)
        assertEquals("Alguns dados não foram carregados: sessões.", reduced.camara.error)
    }

    @Test
    fun mensagemErrorPropagaParaAmbosEstados() {
        val raw = """
            {"type":"ERROR","payload":{"message":"Falha no scraping"},"timestamp":"2025-06-04T12:00:00Z"}
        """.trimIndent()

        val reduced = handler.reduce(WsHandlerState(), raw)
        assertEquals("Falha no scraping", reduced.prefeitura.error)
        assertEquals("Falha no scraping", reduced.camara.error)
    }

    @Test
    fun parseDetailDataAtualizaDetailState() {
        val raw = """
            {
              "type": "DETAIL_DATA",
              "timestamp": "2025-06-04T12:00:00Z",
              "payload": {
                "entity": "vereador",
                "entityId": "karlinda-coelho",
                "parlamentar": {
                  "nome": "Karlinda Coelho",
                  "slug": "karlinda-coelho",
                  "contato": { "email": "v@cmcaninde.ce.gov.br" }
                }
              }
            }
        """.trimIndent()

        val reduced = handler.reduce(
            WsHandlerState(detail = DetailUiState(isLoading = true, entity = DetailEntity.Vereador, entityId = "karlinda-coelho")),
            raw,
        )
        assertEquals(false, reduced.detail.isLoading)
        assertEquals("Karlinda Coelho", reduced.detail.payload?.parlamentar?.nome)
        assertEquals("v@cmcaninde.ce.gov.br", reduced.detail.payload?.parlamentar?.contato?.email)
    }

    @Test
    fun buildRequestDetailFormataJson() {
        val json = handler.buildRequestDetail(DetailEntity.Secretaria, "5")
        assert(json.contains("REQUEST_DETAIL"))
        assert(json.contains("secretaria"))
        assert(json.contains("\"id\":\"5\""))
    }

    @Test
    fun toPrefeituraUiStateMapeiaGraficos() {
        val state = handler.toPrefeituraUiState(
            WsPayload(
                graficos = br.gov.caninde.transparencia.domain.GraficosPayload(
                    prefeitura = listOf(
                        br.gov.caninde.transparencia.domain.ChartSeries(
                            titulo = "Licitações",
                            labels = listOf("Aberta"),
                            valores = listOf(2),
                        ),
                    ),
                ),
            ),
            "2025-06-04T12:00:00Z",
        )
        assertEquals(1, state.graficos?.prefeitura?.size)
    }

    @Test
    fun toPrefeituraUiStateMapeiaCampos() {
        val state = handler.toPrefeituraUiState(
            WsPayload(
                contratos = listOf(Contrato(numero = "99")),
                error = "timeout",
            ),
            "2025-06-04T12:00:00Z",
        )
        assertEquals("99", state.contratos.first().numero)
        assertEquals("timeout", state.error)
    }
}
