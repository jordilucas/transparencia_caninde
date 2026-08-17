package br.gov.caninde.transparencia.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRoutesTest {

    @Test
    fun parseWebPath_rotasPrincipais() {
        assertEquals(AppRoute.Main(Screen.Prefeitura), parseWebPath("/"))
        assertEquals(AppRoute.Main(Screen.Camara), parseWebPath("/camara"))
        assertEquals(AppRoute.Main(Screen.Folha), parseWebPath("/folha"))
        assertEquals(AppRoute.Main(Screen.Agua), parseWebPath("/agua"))
        assertEquals(AppRoute.Contrato("1073"), parseWebPath("/contrato/1073"))
        assertEquals(AppRoute.Vereador("joao-silva"), parseWebPath("/vereador/joao-silva"))
        assertEquals(AppRoute.Lrf("rreo-2025"), parseWebPath("/lrf/rreo-2025"))
    }

    @Test
    fun readWebLocation_prefereHash() {
        assertEquals("/contrato/1073", readWebLocation("/", "#/contrato/1073"))
        assertEquals("/camara", readWebLocation("/camara", ""))
    }

    @Test
    fun shareUrl_usaHash() {
        val url = AppRoute.Contrato("1073").shareUrl()
        assertTrue(url.contains("#/contrato/"))
        assertTrue(url.contains("1073"))
    }

    @Test
    fun toWebPath_detalheContrato() {
        assertEquals("/contrato/1073", AppRoute.Contrato("1073").toWebPath())
    }
}
