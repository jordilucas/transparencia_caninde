package br.gov.caninde.transparencia.domain

private val ACCENT_REPLACEMENTS = mapOf(
    'á' to 'a', 'à' to 'a', 'ã' to 'a', 'â' to 'a', 'ä' to 'a',
    'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
    'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
    'ó' to 'o', 'ò' to 'o', 'õ' to 'o', 'ô' to 'o', 'ö' to 'o',
    'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
    'ç' to 'c', 'ñ' to 'n',
)

fun normalizeSearchText(text: String): String = buildString(text.length) {
    text.lowercase().forEach { char ->
        append(ACCENT_REPLACEMENTS[char] ?: char)
    }
}

fun String.matchesSearch(query: String): Boolean {
    val q = normalizeSearchText(query.trim())
    if (q.isEmpty()) return true
    return normalizeSearchText(this).contains(q)
}

fun matchesAnySearch(query: String, vararg fields: String): Boolean {
    val q = normalizeSearchText(query.trim())
    if (q.isEmpty()) return true
    return fields.any { field -> normalizeSearchText(field).contains(q) }
}

enum class SearchScope(val label: String) {
    Tudo("Tudo"),
    Prefeitura("Prefeitura"),
    Camara("Câmara"),
}

enum class ContratoListFilter(val label: String) {
    Todos("Todos"),
    Vigentes("Vigentes"),
}

enum class LicitacaoListFilter(val label: String) {
    Todas("Todas"),
    Abertas("Abertas"),
}

fun Contrato.matchesListFilter(filter: ContratoListFilter): Boolean = when (filter) {
    ContratoListFilter.Todos -> true
    ContratoListFilter.Vigentes -> {
        val status = vigenciaStatus.lowercase()
        status.contains("vigente") || (status.isBlank() && vigenciaFim.isBlank())
    }
}

fun Licitacao.matchesListFilter(filter: LicitacaoListFilter): Boolean = when (filter) {
    LicitacaoListFilter.Todas -> true
    LicitacaoListFilter.Abertas -> {
        val sit = situacao.lowercase()
        sit.contains("aberta") || sit.contains("andamento") || sit.contains("public")
    }
}
