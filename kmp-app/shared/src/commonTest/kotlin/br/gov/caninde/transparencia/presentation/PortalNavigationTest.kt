package br.gov.caninde.transparencia.presentation

import br.gov.caninde.transparencia.domain.Sessao
import kotlin.test.Test
import kotlin.test.assertEquals

class PortalNavigationTest {

    @Test
    fun sessaoRouteId_prefereSlug() {
        val s = Sessao(slug = "sessao-ordinaria-2026", url = "https://example/video/outro/")
        assertEquals("sessao-ordinaria-2026", sessaoRouteId(s, 3))
    }

    @Test
    fun sessaoRouteId_extraiDaUrlVideo() {
        val s = Sessao(
            slug = "",
            url = "https://www.cmcaninde.ce.gov.br/video/sessao-extraordinaria/",
        )
        assertEquals("sessao-extraordinaria", sessaoRouteId(s, 0))
    }

    @Test
    fun sessaoRouteId_fallbackIndice() {
        val s = Sessao(slug = "", url = "")
        assertEquals("5", sessaoRouteId(s, 5))
    }
}
