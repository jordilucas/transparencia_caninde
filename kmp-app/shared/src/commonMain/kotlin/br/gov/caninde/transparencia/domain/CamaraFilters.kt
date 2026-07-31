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

fun Sessao.isVideoSession(): Boolean =
    url.contains("/video/", ignoreCase = true) || videoEmbedUrl.isNotBlank()

fun Sessao.videoUrl(): String = when {
    url.contains("/video/", ignoreCase = true) && url.isNotBlank() -> url
    videoEmbedUrl.isNotBlank() -> youtubeWatchUrl(videoEmbedUrl).ifBlank { videoEmbedUrl }
    else -> ""
}

fun youtubeWatchUrl(embedOrWatchUrl: String): String {
    val url = embedOrWatchUrl.trim()
    if (url.isBlank()) return ""
    val embedId = Regex("youtube\\.com/embed/([^?&/]+)", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)
    if (embedId != null) return "https://www.youtube.com/watch?v=$embedId"
    val watchId = Regex("[?&]v=([^&]+)", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)
    if (watchId != null) return "https://www.youtube.com/watch?v=$watchId"
    val shortId = Regex("youtu\\.be/([^?&/]+)", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)
    if (shortId != null) return "https://www.youtube.com/watch?v=$shortId"
    return url
}

fun documentoCamaraRouteId(doc: DocumentoCamara): String = encodePortalPageId(doc.url)
