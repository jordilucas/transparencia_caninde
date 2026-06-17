package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class TransparenciaViewModel(
    private val repository: TransparenciaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val prefeituraState: StateFlow<PrefeituraUiState> = repository.prefeituraState
    val camaraState: StateFlow<CamaraUiState> = repository.camaraState
    val detailState: StateFlow<DetailUiState> = repository.detailState

    fun onStart() {
        repository.connect(scope)
    }

    fun onStop() {
        repository.disconnect()
    }

    fun refreshPrefeitura() {
        scope.launch { repository.refreshSource("prefeitura") }
    }

    fun refreshCamara() {
        scope.launch { repository.refreshSource("camara") }
    }

    fun refreshAll() {
        scope.launch { repository.refreshSource("all") }
    }

    fun reconnect() {
        repository.forceReconnect()
    }

    fun loadDetail(entity: DetailEntity, id: String) {
        scope.launch { repository.loadDetail(entity, id) }
    }

    fun dispose() {
        repository.disconnect()
        scope.cancel()
    }
}
