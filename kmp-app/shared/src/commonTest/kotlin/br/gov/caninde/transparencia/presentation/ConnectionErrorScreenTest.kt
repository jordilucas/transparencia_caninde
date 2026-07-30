package br.gov.caninde.transparencia.presentation

import br.gov.caninde.transparencia.domain.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionErrorScreenTest {

    private val emptyPref = PrefeituraUiState()
    private val emptyCam = CamaraUiState()

    @Test
    fun mostraErro_quandoOfflineSemCache() {
        assertTrue(
            shouldShowConnectionErrorScreen(
                ConnectionState.Error,
                emptyPref,
                emptyCam,
                onSobreScreen = false,
            ),
        )
    }

    @Test
    fun naoMostraErro_naPaginaSobre() {
        assertFalse(
            shouldShowConnectionErrorScreen(
                ConnectionState.Error,
                emptyPref,
                emptyCam,
                onSobreScreen = true,
            ),
        )
    }

    @Test
    fun naoMostraErro_comDadosEmCache() {
        val prefeitura = PrefeituraUiState(contratos = listOf(Contrato(numero = "1")))
        assertFalse(
            shouldShowConnectionErrorScreen(
                ConnectionState.Error,
                prefeitura,
                emptyCam,
                onSobreScreen = false,
            ),
        )
    }

    @Test
    fun mostraErro_duranteReconexaoSemCache() {
        assertTrue(
            shouldShowConnectionErrorScreen(
                ConnectionState.Reconnecting,
                emptyPref,
                emptyCam,
                onSobreScreen = false,
            ),
        )
    }

    @Test
    fun naoMostraErro_quandoConectado() {
        assertFalse(
            shouldShowConnectionErrorScreen(
                ConnectionState.Connected,
                emptyPref,
                emptyCam,
                onSobreScreen = false,
            ),
        )
    }
}
