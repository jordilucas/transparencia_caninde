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

    fun onStart() {
        repository.connect(scope)
    }

    fun onStop() {
        repository.disconnect()
    }

    fun refreshPrefeitura() {
        scope.launch { repository.requestRefresh("prefeitura") }
    }

    fun refreshCamara() {
        scope.launch { repository.requestRefresh("camara") }
    }

    fun refreshAll() {
        scope.launch { repository.requestRefresh("all") }
    }

    fun dispose() {
        repository.disconnect()
        scope.cancel()
    }
}
