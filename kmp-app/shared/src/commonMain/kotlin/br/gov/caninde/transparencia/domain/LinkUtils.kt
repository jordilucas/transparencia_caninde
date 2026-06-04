package br.gov.caninde.transparencia.domain

const val CAMARA_PORTAL_BASE = "https://www.cmcaninde.ce.gov.br"
const val PREFEITURA_PORTAL_BASE = "https://www.caninde.ce.gov.br"

fun resolveAbsoluteUrl(href: String, base: String = CAMARA_PORTAL_BASE): String {
    val trimmed = href.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    val baseClean = base.trimEnd('/')
    val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    return "$baseClean$path"
}

fun isPdfLink(url: String): Boolean {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return path.endsWith(".pdf") || ".pdf" in path
}

fun pdfLinkLabel(url: String): String {
    val name = url.substringBefore('?').substringAfterLast('/')
    return if (name.endsWith(".pdf", ignoreCase = true) && name.length < 80) name else "Abrir documento PDF"
}
