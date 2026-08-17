package br.gov.caninde.transparencia.domain

import br.gov.caninde.transparencia.data.currentTimeMillis

fun parseIsoTimestampMillis(iso: String): Long? {
    val match = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})""").find(iso.trim()) ?: return null
    val (year, month, day, hour, minute, second) = match.destructured
    return utcEpochMillis(
        year.toInt(),
        month.toInt(),
        day.toInt(),
        hour.toInt(),
        minute.toInt(),
        second.toInt(),
    )
}

private fun utcEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
    var y = year
    var m = month
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m - 3) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097L + doe - 719468L
    return days * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1_000L
}

fun formatRelativeUpdated(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    val millis = parseIsoTimestampMillis(isoTimestamp) ?: return ""
    val now = currentTimeMillis()
    val diffMs = (now - millis).coerceAtLeast(0)
    val diffMin = diffMs / 60_000L
    val diffHours = diffMs / 3_600_000L
    val diffDays = diffMs / 86_400_000L
    return when {
        diffMin < 1 -> "agora há pouco"
        diffMin < 60 -> "há $diffMin min"
        diffHours < 24 -> "há $diffHours h"
        diffDays == 1L -> "há 1 dia"
        diffDays < 7 -> "há $diffDays dias"
        else -> ""
    }
}

fun exercicioYearOptions(anchor: Int = currentCalendarYear()): List<Int> =
    (0..2).map { anchor - it }

fun currentCalendarYear(): Int {
    val days = currentTimeMillis() / 86_400_000L
    return utcDateFromDays(days).first
}

private fun utcDateFromDays(daysSinceEpoch: Long): Triple<Int, Int, Int> {
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
