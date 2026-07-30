package br.gov.caninde.transparencia.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRoutesTest {

    @Test
    fun parseWebPath_rotasPrincipais() {
        assertEquals(AppRoute.Main(Screen.Prefeitura), parseWebPath("/"))
        assertEquals(AppRoute.Main(Screen.Camara), parseWebPath("/camara"))
        assertEquals(AppRoute.Contrato("1073"), parseWebPath("/contrato/1073"))
        assertEquals(AppRoute.Vereador("joao-silva"), parseWebPath("/vereador/joao-silva"))
    }

    @Test
    fun toWebPath_detalheContrato() {
        assertTrue(AppRoute.Contrato("1073").toWebPath().contains("1073"))
    }
}
