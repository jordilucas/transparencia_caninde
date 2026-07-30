package br.gov.caninde.transparencia.presentation

import br.gov.caninde.transparencia.domain.*

private val contratoIdRegex = Regex("contratos\\.php\\?id=([^&]+)", RegexOption.IGNORE_CASE)
private val licitacaoIdRegex = Regex("licitacaolista\\.php\\?id=([^&]+)", RegexOption.IGNORE_CASE)
private val publicacaoIdRegex = Regex("publicacoes\\.php\\?id=([^&]+)", RegexOption.IGNORE_CASE)
private val materiaSlugRegex = Regex("/materia/([^/?#]+)", RegexOption.IGNORE_CASE)
private val sessaoSlugRegex = Regex("/sessao/([^/?#]+)", RegexOption.IGNORE_CASE)

fun routeFromExternalUrl(url: String): AppRoute {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return AppRoute.PaginaPortal("")

    val absolute = when {
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.contains("cmcaninde", ignoreCase = true) ->
            resolveAbsoluteUrl(trimmed, CAMARA_PORTAL_BASE)
        else -> resolveAbsoluteUrl(trimmed, PREFEITURA_PORTAL_BASE)
    }

    contratoIdRegex.find(absolute)?.groupValues?.get(1)?.let { return AppRoute.Contrato(it) }
    licitacaoIdRegex.find(absolute)?.groupValues?.get(1)?.let { return AppRoute.Licitacao(it) }
    publicacaoIdRegex.find(absolute)?.groupValues?.get(1)?.let { return AppRoute.Publicacao(it) }
    materiaSlugRegex.find(absolute)?.groupValues?.get(1)?.let { return AppRoute.Materia(it) }
    sessaoSlugRegex.find(absolute)?.groupValues?.get(1)?.let { return AppRoute.Sessao(it) }

    return AppRoute.PaginaPortal(encodePortalPageId(absolute))
}

fun routeFromLink(link: LinkExterno): AppRoute = routeFromExternalUrl(link.url)

fun routeFromPublicacao(publicacao: Publicacao): AppRoute {
    if (publicacao.id.isNotBlank()) return AppRoute.Publicacao(publicacao.id)
    return routeFromExternalUrl(publicacao.url)
}

fun routeFromContrato(contrato: Contrato): AppRoute {
    val id = contratoDetailId(contrato)
    if (id.isNotBlank()) return AppRoute.Contrato(id)
    return routeFromExternalUrl(contrato.url)
}

fun routeFromLicitacao(licitacao: Licitacao): AppRoute {
    val id = licitacaoDetailId(licitacao)
    if (id.isNotBlank()) return AppRoute.Licitacao(id)
    return routeFromExternalUrl(licitacao.url)
}

fun contratoDetailId(contrato: Contrato): String {
    if (contrato.id.isNotBlank()) return contrato.id
    contratoIdRegex.find(contrato.url)?.groupValues?.get(1)?.let { return it }
    return contrato.numero
}

fun licitacaoDetailId(licitacao: Licitacao): String {
    if (licitacao.id.isNotBlank()) return licitacao.id
    licitacaoIdRegex.find(licitacao.url)?.groupValues?.get(1)?.let { return it }
    return licitacao.numero
}

private val vereadorSlugRegex = Regex("/vereadores/([^/?#]+)", RegexOption.IGNORE_CASE)

fun parlamentarSlug(parlamentar: Parlamentar): String {
    if (parlamentar.slug.isNotBlank()) return parlamentar.slug
    vereadorSlugRegex.find(parlamentar.profileUrl)?.groupValues?.get(1)?.let { return it }
    return ""
}

fun materiaSlug(materia: Materia): String {
    if (materia.slug.isNotBlank()) return materia.slug
    materiaSlugRegex.find(materia.url)?.groupValues?.get(1)?.let { return it }
    return ""
}

fun sessaoRouteId(sessao: Sessao, index: Int): String =
    sessao.slug.ifBlank { index.toString() }

fun portalBaseUrl(origem: String): String = when (origem) {
    "camara" -> CAMARA_PORTAL_BASE
    "prefeitura" -> PREFEITURA_PORTAL_BASE
    else -> PREFEITURA_PORTAL_BASE
}
