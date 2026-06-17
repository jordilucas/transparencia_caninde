package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DataMergeTest {

    @Test
    fun mergeContratosPrefereDataMaisRecente() {
        val merged = DataMerge.mergeContratos(
            listOf(Contrato(numero = "1", data = "01/01/2025", valor = "R$ 10")),
            listOf(Contrato(numero = "1", data = "15/06/2025", valor = "")),
        )
        assertEquals("15/06/2025", merged.single().data)
        assertEquals("R$ 10", merged.single().valor)
    }

    @Test
    fun mergeContratosUneNumerosDistintos() {
        val merged = DataMerge.mergeContratos(
            listOf(Contrato(numero = "A", data = "01/01/2025")),
            listOf(Contrato(numero = "B", data = "10/06/2025")),
        )
        assertEquals(2, merged.size)
        assertEquals("B", merged.first().numero)
    }

    @Test
    fun mergeDiariosOrdenaPorDataNoTexto() {
        val merged = DataMerge.mergeDiarios(
            listOf("Diário — 01/01/2025"),
            listOf("Diário — 20/06/2025"),
        )
        assertEquals(2, merged.size)
        assert(merged.first().contains("20/06/2025"))
    }
}
