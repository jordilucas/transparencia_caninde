package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CamaraFiltersTest {

    @Test
    fun materiaFilter_porTipo() {
        val materia = Materia(titulo = "PL 1", tipo = "Projeto de Lei")
        assertTrue(materia.matchesMateriaFilter("Projeto de Lei"))
        assertFalse(materia.matchesMateriaFilter("Requerimento"))
        assertTrue(materia.matchesMateriaFilter(MATERIA_FILTER_TODAS))
    }

    @Test
    fun parlamentar_matchesMembroMesa() {
        val p = Parlamentar(nome = "Maria Silva", nomeCompleto = "Maria da Silva")
        val membro = MembroMesa(nome = "Maria da Silva", cargo = "Presidente")
        assertTrue(p.matchesMembroMesa(membro))
    }

    @Test
    fun sessao_videoUrl() {
        val sessao = Sessao(url = "https://www.cmcaninde.ce.gov.br/video/sessao-1/")
        assertTrue(sessao.isVideoSession())
        assertTrue(sessao.videoUrl().contains("/video/"))
    }
}
