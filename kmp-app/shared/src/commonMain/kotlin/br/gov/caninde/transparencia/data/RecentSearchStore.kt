package br.gov.caninde.transparencia.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RecentSearchStore {
    private const val MAX = 8
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<String> = runCatching {
        val raw = LocalDataStore.loadRecentSearches() ?: return emptyList()
        json.decodeFromString<List<String>>(raw)
    }.getOrDefault(emptyList())

    fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val updated = (listOf(trimmed) + load().filter { !it.equals(trimmed, ignoreCase = true) })
            .take(MAX)
        runCatching {
            LocalDataStore.saveRecentSearches(json.encodeToString(updated))
        }
    }
}
