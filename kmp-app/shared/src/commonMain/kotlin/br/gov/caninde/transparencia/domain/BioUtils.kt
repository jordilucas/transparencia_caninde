package br.gov.caninde.transparencia.domain

fun sanitizeBiography(raw: String): String {
    if (raw.isBlank()) return ""
    val skipPrefixes = listOf(
        "parlamentar:", "cargo:", "e-mail", "email:", "telefone:", "whatsapp",
        "endereço", "horário", "de segunda", "addtoany", "compartilhar",
    )
    return raw
        .replace(Regex("[ \t]+"), " ")
        .lines()
        .map { it.trim() }
        .filter { it.length >= 20 }
        .filterNot { line -> skipPrefixes.any { line.contains(it, ignoreCase = true) } }
        .distinct()
        .joinToString("\n\n")
        .trim()
}

fun truncateToolbarTitle(text: String, maxChars: Int = 26): String {
    val t = text.trim()
    if (t.length <= maxChars) return t
    return t.take(maxChars).trimEnd() + "…"
}
