package br.gov.caninde.transparencia.presentation

const val SITE_BASE_URL = "https://transparenciacaninde.com.br"

fun AppRoute.toWebPath(): String = when (this) {
    is AppRoute.Main -> when (screen) {
        Screen.Prefeitura -> "/"
        Screen.Folha -> "/folha"
        Screen.Camara -> "/camara"
        Screen.Graficos -> "/graficos"
        Screen.Busca -> "/busca"
        Screen.Agua -> "/saae"
        Screen.Sobre -> "/sobre"
    }
    is AppRoute.Vereador -> "/vereador/${encodeUriSegment(slug)}"
    is AppRoute.Materia -> "/materia/${encodeUriSegment(slug)}"
    is AppRoute.Secretaria -> "/secretaria/${encodeUriSegment(id)}"
    is AppRoute.Contrato -> "/contrato/${encodeUriSegment(numero)}"
    is AppRoute.Licitacao -> "/licitacao/${encodeUriSegment(numero)}"
    is AppRoute.Sessao -> "/sessao/${encodeUriSegment(id)}"
    is AppRoute.Publicacao -> "/publicacao/${encodeUriSegment(id)}"
    is AppRoute.Obra -> "/obra/${encodeUriSegment(id)}"
    is AppRoute.Lrf -> "/lrf/${encodeUriSegment(id)}"
    is AppRoute.PaginaPortal -> "/pagina/${encodeUriSegment(pageId)}"
    is AppRoute.DocumentoCamara -> "/documento-camara/${encodeUriSegment(pageId)}"
    AppRoute.Gestores -> "/gestores"
    is AppRoute.Institucional -> if (camara) "/institucional/camara" else "/institucional/prefeitura"
}

fun AppRoute.shareUrl(): String = "$SITE_BASE_URL#${toWebPath()}"

fun parseWebPath(path: String): AppRoute? {
    val normalized = path.trim().let { raw ->
        when {
            raw.contains('#') -> raw.substringAfter('#')
            else -> raw
        }
    }.ifBlank { "/" }
    val segments = normalized.trim('/').split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return AppRoute.Main(Screen.Prefeitura)
    return when (segments.first()) {
        "camara" -> AppRoute.Main(Screen.Camara)
        "folha" -> AppRoute.Main(Screen.Folha)
        "graficos" -> AppRoute.Main(Screen.Graficos)
        "busca" -> AppRoute.Main(Screen.Busca)
        "agua", "saae" -> AppRoute.Main(Screen.Agua)
        "sobre" -> AppRoute.Main(Screen.Sobre)
        "vereador" -> segments.getOrNull(1)?.let { AppRoute.Vereador(decodeUriSegment(it)) }
        "materia" -> segments.getOrNull(1)?.let { AppRoute.Materia(decodeUriSegment(it)) }
        "secretaria" -> segments.getOrNull(1)?.let { AppRoute.Secretaria(decodeUriSegment(it)) }
        "contrato" -> segments.getOrNull(1)?.let { AppRoute.Contrato(decodeUriSegment(it)) }
        "licitacao" -> segments.getOrNull(1)?.let { AppRoute.Licitacao(decodeUriSegment(it)) }
        "sessao" -> segments.getOrNull(1)?.let { AppRoute.Sessao(decodeUriSegment(it)) }
        "publicacao" -> segments.getOrNull(1)?.let { AppRoute.Publicacao(decodeUriSegment(it)) }
        "obra" -> segments.getOrNull(1)?.let { AppRoute.Obra(decodeUriSegment(it)) }
        "lrf" -> segments.getOrNull(1)?.let { AppRoute.Lrf(decodeUriSegment(it)) }
        "pagina" -> segments.getOrNull(1)?.let { AppRoute.PaginaPortal(decodeUriSegment(it)) }
        "documento-camara" -> segments.getOrNull(1)?.let { AppRoute.DocumentoCamara(decodeUriSegment(it)) }
        "gestores" -> AppRoute.Gestores
        "institucional" -> when (segments.getOrNull(1)) {
            "camara" -> AppRoute.Institucional(true)
            else -> AppRoute.Institucional(false)
        }
        else -> null
    }
}

fun readWebLocation(pathname: String, hash: String): String {
    val hashPath = hash.removePrefix("#").trim()
    if (hashPath.isNotEmpty() && hashPath != "/") {
        return if (hashPath.startsWith("/")) hashPath else "/$hashPath"
    }
    val path = pathname.trim().ifBlank { "/" }
    if (path != "/" && !path.contains('.')) return path
    return "/"
}

expect fun encodeUriSegment(value: String): String

expect fun decodeUriSegment(value: String): String

expect fun currentWebLocation(): String

expect fun updateWebPath(path: String, replace: Boolean)

expect fun webHistoryBack()

expect fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit
