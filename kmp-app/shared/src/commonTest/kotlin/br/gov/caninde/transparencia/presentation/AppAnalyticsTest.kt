package br.gov.caninde.transparencia.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class AppAnalyticsTest {

    @Test
    fun analyticsScreenName_rotasPrincipais() {
        assertEquals("prefeitura", AppRoute.Main(Screen.Prefeitura).analyticsScreenName())
        assertEquals("busca", AppRoute.Main(Screen.Busca).analyticsScreenName())
        assertEquals("detalhe_contrato", AppRoute.Contrato("1").analyticsScreenName())
    }
}
