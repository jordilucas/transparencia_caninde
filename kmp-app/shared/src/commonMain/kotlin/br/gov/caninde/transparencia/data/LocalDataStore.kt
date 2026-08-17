package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.CamaraUiState
import br.gov.caninde.transparencia.domain.PrefeituraUiState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect object LocalDataStore {
    fun savePrefeitura(json: String)
    fun loadPrefeitura(): String?
    fun saveCamara(json: String)
    fun loadCamara(): String?
    fun saveRecentSearches(json: String)
    fun loadRecentSearches(): String?
}

object LocalCache {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun loadPrefeitura(): PrefeituraUiState? = runCatching {
        val raw = LocalDataStore.loadPrefeitura() ?: return null
        json.decodeFromString<PrefeituraUiState>(raw).copy(isLoading = false)
    }.getOrNull()

    fun loadCamara(): CamaraUiState? = runCatching {
        val raw = LocalDataStore.loadCamara() ?: return null
        json.decodeFromString<CamaraUiState>(raw).copy(isLoading = false)
    }.getOrNull()

    fun savePrefeitura(state: PrefeituraUiState) {
        if (state.contratos.isEmpty() && state.obras.isEmpty() && state.lrf.isEmpty()) return
        runCatching {
            LocalDataStore.savePrefeitura(json.encodeToString(state.copy(isLoading = false, error = null)))
        }
    }

    fun saveCamara(state: CamaraUiState) {
        if (state.parlamentares.isEmpty() && state.sessoes.isEmpty()) return
        runCatching {
            LocalDataStore.saveCamara(json.encodeToString(state.copy(isLoading = false, error = null)))
        }
    }
}
