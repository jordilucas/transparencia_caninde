package br.gov.caninde.transparencia.domain

/**
 * Interpreta valor vindo do scraping (número, wa.me, api.whatsapp.com).
 * URLs de compartilhamento sem telefone (AddToAny, wa.me/?text=) retornam null.
 */
data class ParsedWhatsapp(
    val digits: String,
    val displayLabel: String,
    val openUrl: String,
)

fun parseWhatsapp(raw: String): ParsedWhatsapp? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.contains("addtoany", ignoreCase = true)) return null

    val fromWaMe = Regex("""wa\.me/(\d{8,15})""", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)
    val fromApi = Regex("""[?&]phone=(\d{8,15})""", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)
    val fromTel = Regex("""tel:([+\d\s()-]+)""", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)

    val digits = (fromWaMe ?: fromApi ?: fromTel)
        ?.filter { it.isDigit() }
        ?: trimmed.filter { it.isDigit() }.takeIf { it.length in 10..15 }

    if (digits == null || digits.length !in 10..15) {
        if (trimmed.contains("wa.me", ignoreCase = true) ||
            trimmed.contains("whatsapp", ignoreCase = true)
        ) {
            return null
        }
        return null
    }

    return ParsedWhatsapp(
        digits = digits,
        displayLabel = formatPhoneDisplay(digits),
        openUrl = "https://wa.me/$digits",
    )
}

fun formatPhoneDisplay(digits: String): String {
    val d = digits.removePrefix("55")
    return when {
        d.length == 11 -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        d.length == 10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
        digits.length > 11 && digits.startsWith("55") -> "+55 ${formatPhoneDisplay(digits.removePrefix("55"))}"
        else -> digits
    }
}
