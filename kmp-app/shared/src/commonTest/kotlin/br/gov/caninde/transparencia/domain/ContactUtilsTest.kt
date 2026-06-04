package br.gov.caninde.transparencia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactUtilsTest {

    @Test
    fun parseWaMeUrl() {
        val p = parseWhatsapp("https://wa.me/5585987654321")
        assertEquals("5585987654321", p?.digits)
        assertEquals("https://wa.me/5585987654321", p?.openUrl)
    }

    @Test
    fun parsePlainDigits() {
        val p = parseWhatsapp("85987654321")
        assertEquals("85987654321", p?.digits)
    }

    @Test
    fun rejectShareUrlWithoutPhone() {
        assertNull(parseWhatsapp("https://wa.me/?text=compartilhar"))
        assertNull(parseWhatsapp("https://www.addtoany.com/share"))
    }
}

class BioUtilsTest {

    @Test
    fun sanitizeBiographyRemoveMetadados() {
        val raw = """
            Parlamentar: João Silva
            Cargo: Vereador - PT
            Texto real da biografia com mais de vinte caracteres.
        """.trimIndent()
        val out = sanitizeBiography(raw)
        assert(out.contains("Texto real"))
        assert(!out.contains("Parlamentar:"))
    }
}
