package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchIndexTest {

    private val prefeitura = PrefeituraUiState(
        isLoading = false,
        contratos = listOf(Contrato(numero = "001/2025", objeto = "Pavimentacao urbana", empresa = "Construtora ABC")),
        licitacoes = listOf(Licitacao(numero = "02/2025", objeto = "Material escolar")),
    )

    private val camara = CamaraUiState(
        isLoading = false,
        parlamentares = listOf(Parlamentar(nome = "Maria Silva", partido = "PT")),
        materias = listOf(Materia(titulo = "Requerimento educacao", tipo = "Requerimento")),
        documentosTransparencia = listOf(
            DocumentoCamara(titulo = "Edital pregão 01", categoria = "licitacao", data = "01/07/2026"),
        ),
    )

    @Test
    fun search_encontraContratoPorEmpresa() {
        val hits = SearchIndex.search(prefeitura, camara, "construtora", SearchScope.Tudo)
        assertTrue(hits.any { it is SearchHit.ContratoHit })
    }

    @Test
    fun search_ignoraAcentos() {
        val hits = SearchIndex.search(prefeitura, camara, "educacao", SearchScope.Tudo)
        assertTrue(hits.any { it is SearchHit.MateriaHit })
    }

    @Test
    fun search_filtraPorTipoDocumento() {
        val hits = SearchIndex.search(
            prefeitura,
            camara,
            "edital",
            SearchScope.Camara,
            SearchEntityFilter.Documentos,
        )
        assertEquals(1, hits.size)
        assertTrue(hits.first() is SearchHit.DocumentoCamaraHit)
    }
}
