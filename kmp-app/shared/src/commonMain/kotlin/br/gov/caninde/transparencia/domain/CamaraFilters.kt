package br.gov.caninde.transparencia.domain

const val MATERIA_FILTER_TODAS = "Todas"
const val CAMARA_LIST_PAGE_SIZE = 15

fun materiaFilterOptions(materias: List<Materia>): List<String> {
    val tipos = materias
        .map { it.tipo.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }
    return listOf(MATERIA_FILTER_TODAS) + tipos
}

fun Materia.matchesMateriaFilter(filter: String): Boolean =
    filter == MATERIA_FILTER_TODAS || tipo.equals(filter, ignoreCase = true)

fun Parlamentar.matchesMembroMesa(membro: MembroMesa): Boolean {
    val alvo = membro.nome.trim()
    if (alvo.isBlank()) return false
    if (nome.equals(alvo, ignoreCase = true)) return true
    if (nomeCompleto.equals(alvo, ignoreCase = true)) return true
    val primeiro = alvo.split(" ").firstOrNull()?.takeIf { it.length > 2 } ?: return false
    return nome.contains(primeiro, ignoreCase = true) || nomeCompleto.contains(primeiro, ignoreCase = true)
}

fun Sessao.isVideoSession(): Boolean = url.contains("/video/", ignoreCase = true)

fun Sessao.videoUrl(): String = url.takeIf { isVideoSession() && it.isNotBlank() }.orEmpty()
