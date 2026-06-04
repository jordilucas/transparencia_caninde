package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.Contrato
import br.gov.caninde.transparencia.domain.WsPayload
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
