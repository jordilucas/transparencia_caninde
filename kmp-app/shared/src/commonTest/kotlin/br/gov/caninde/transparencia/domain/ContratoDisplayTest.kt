package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContratoDisplayTest {

    @Test
    fun displayInfoUsaSecretariaComoTitulo() {
        val info = Contrato(
            secretaria = "Secretaria Municipal da Saúde",
            objeto = "Aquisição de medicamentos",
            valor = "R$ 10.000,00",
            empresa = "Empresa X LTDA",
            numero = "001/2026",
            data = "01/06/2026",
        ).displayInfo()

        assertEquals("Secretaria Municipal da Saúde", info.titulo)
        assertEquals("Aquisição de medicamentos", info.descricao)
        assertEquals("R$ 10.000,00", info.valor)
    }

    @Test
    fun normalizaLinhaHtmlComCamposColados() {
        val info = Contrato(
            numero = "202606120002    CONTRATO ORIGINAL",
            objeto = "RDO ROBENYLSON FURTADO NOGUEIRA 49.627.786/0001-60",
            valor = "Secretaria de Segurança Pública e Trânsito NOVO CONTRATAÇÃO DE EMPRESA",
            empresa = "12/06/202665.000,00",
            data = "12/06/2026  12/06/2027VIGENTE",
        ).displayInfo()

        assertEquals("Secretaria de Segurança Pública e Trânsito", info.titulo)
        assertTrue(info.descricao.contains("CONTRATAÇÃO"))
        assertTrue(info.valor.contains("65.000"))
        assertEquals("Vigente", info.situacao)
    }
}
