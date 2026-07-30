package br.gov.caninde.transparencia.presentation

import br.gov.caninde.transparencia.domain.encodePortalPageId

fun AppRoute.toWebPath(): String = when (this) {
    is AppRoute.Main -> when (screen) {
        Screen.Prefeitura -> "/"
        Screen.Camara -> "/camara"
        Screen.Graficos -> "/graficos"
        Screen.Busca -> "/busca"
        Screen.Sobre -> "/sobre"
    }
    is AppRoute.Vereador -> "/vereador/${encodePath(slug)}"
    is AppRoute.Materia -> "/materia/${encodePath(slug)}"
    is AppRoute.Secretaria -> "/secretaria/${encodePath(id)}"
    is AppRoute.Contrato -> "/contrato/${encodePath(numero)}"
    is AppRoute.Licitacao -> "/licitacao/${encodePath(numero)}"
    is AppRoute.Sessao -> "/sessao/${encodePath(id)}"
    is AppRoute.Publicacao -> "/publicacao/${encodePath(id)}"
    is AppRoute.PaginaPortal -> "/pagina/${encodePath(pageId)}"
    AppRoute.Gestores -> "/gestores"
    is AppRoute.Institucional -> if (camara) "/institucional/camara" else "/institucional/prefeitura"
}

fun parseWebPath(path: String): AppRoute? {
    val segments = path.trim('/').split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return AppRoute.Main(Screen.Prefeitura)
    return when (segments.first()) {
        "camara" -> AppRoute.Main(Screen.Camara)
        "graficos" -> AppRoute.Main(Screen.Graficos)
        "busca" -> AppRoute.Main(Screen.Busca)
        "sobre" -> AppRoute.Main(Screen.Sobre)
        "vereador" -> segments.getOrNull(1)?.let { AppRoute.Vereador(decodePath(it)) }
        "materia" -> segments.getOrNull(1)?.let { AppRoute.Materia(decodePath(it)) }
        "secretaria" -> segments.getOrNull(1)?.let { AppRoute.Secretaria(decodePath(it)) }
        "contrato" -> segments.getOrNull(1)?.let { AppRoute.Contrato(decodePath(it)) }
        "licitacao" -> segments.getOrNull(1)?.let { AppRoute.Licitacao(decodePath(it)) }
        "sessao" -> segments.getOrNull(1)?.let { AppRoute.Sessao(decodePath(it)) }
        "publicacao" -> segments.getOrNull(1)?.let { AppRoute.Publicacao(decodePath(it)) }
        "pagina" -> segments.getOrNull(1)?.let { AppRoute.PaginaPortal(decodePath(it)) }
        "gestores" -> AppRoute.Gestores
        "institucional" -> when (segments.getOrNull(1)) {
            "camara" -> AppRoute.Institucional(true)
            else -> AppRoute.Institucional(false)
        }
        else -> null
    }
}

private fun encodePath(value: String): String =
    value.replace("%", "%25").replace("/", "%2F")

private fun decodePath(value: String): String =
    value.replace("%2F", "/").replace("%25", "%")

expect fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit

expect fun updateWebPath(path: String)

expect fun currentWebPath(): String
