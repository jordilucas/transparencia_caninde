package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchUtilsTest {

    @Test
    fun normalizeSearchText_removeAcentos() {
        assertTrue(normalizeSearchText("Educação").contains("educacao"))
        assertTrue(normalizeSearchText("Licitação").contains("licitacao"))
    }

    @Test
    fun matchesSearch_ignoraAcentos() {
        assertTrue("Secretaria de Educação".matchesSearch("educacao"))
        assertTrue("João Silva".matchesSearch("joao"))
        assertFalse("Contrato de obras".matchesSearch("licitacao"))
    }

    @Test
    fun matchesAnySearch_variosCampos() {
        assertTrue(matchesAnySearch("silva", "Objeto longo", "Empresa Silva LTDA"))
        assertFalse(matchesAnySearch("xyz", "Objeto", "Empresa"))
    }

    @Test
    fun contratoFilter_vigente() {
        val vigente = Contrato(vigenciaStatus = "Vigente")
        val encerrado = Contrato(vigenciaStatus = "Encerrado")
        assertTrue(vigente.matchesListFilter(ContratoListFilter.Vigentes))
        assertFalse(encerrado.matchesListFilter(ContratoListFilter.Vigentes))
        assertTrue(encerrado.matchesListFilter(ContratoListFilter.Todos))
    }

    @Test
    fun licitacaoFilter_aberta() {
        val aberta = Licitacao(situacao = "Aberta")
        val homologada = Licitacao(situacao = "Homologada")
        assertTrue(aberta.matchesListFilter(LicitacaoListFilter.Abertas))
        assertFalse(homologada.matchesListFilter(LicitacaoListFilter.Abertas))
    }
}
