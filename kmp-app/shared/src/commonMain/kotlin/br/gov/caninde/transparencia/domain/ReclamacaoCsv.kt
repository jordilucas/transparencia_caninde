package br.gov.caninde.transparencia.domain

fun reclamacoesAguaToCsv(reclamacoes: List<ReclamacaoAgua>): String {
    val header = "id,endereco,setor,dias_sem_agua,data_registro,media_url\n"
    val rows = reclamacoes.joinToString("\n") { item ->
        listOf(
            csvEscape(item.id),
            csvEscape(item.endereco),
            csvEscape(item.setor),
            item.diasSemAgua.toString(),
            csvEscape(formatCsvDate(item.criadoEmMillis)),
            csvEscape(item.mediaUrl.orEmpty()),
        ).joinToString(",")
    }
    return header + rows + if (rows.isNotEmpty()) "\n" else ""
}

private fun csvEscape(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
    val escaped = value.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}

private fun formatCsvDate(millis: Long): String {
    if (millis <= 0L) return ""
    val totalSeconds = millis / 1000L
    val days = totalSeconds / 86_400L
    val rem = totalSeconds % 86_400L
    val hours = rem / 3600L
    val minutes = (rem % 3600L) / 60L
    val seconds = rem % 60L
    val (year, month, day) = daysToUtcDate(days)
    return buildString {
        append(year.toString().padStart(4, '0'))
        append('-')
        append(month.toString().padStart(2, '0'))
        append('-')
        append(day.toString().padStart(2, '0'))
        append('T')
        append(hours.toString().padStart(2, '0'))
        append(':')
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(seconds.toString().padStart(2, '0'))
        append('Z')
    }
}

private fun daysToUtcDate(daysSinceEpoch: Long): Triple<Int, Int, Int> {
    var z = daysSinceEpoch + 719_468L
    val era = if (z >= 0L) z / 146_097L else (z - 146_096L) / 146_097L
    val doe = (z - era * 146_097L).toInt()
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400L
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100 + yoe / 400)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + if (mp < 10) 3 else -9
    val year = y + if (m <= 2) 1 else 0
    return Triple(year.toInt(), m, d)
}
