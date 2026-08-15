package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.PickedMedia
import br.gov.caninde.transparencia.domain.ReclamacaoAguaUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReclamacaoAguaViewModel(
    private val repository: ReclamacaoAguaRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(
        ReclamacaoAguaUiState(
            firebaseConfigured = FirebaseConfig.isConfigured,
            supabaseConfigured = SupabaseConfig.isConfigured,
        ),
    )
    val uiState: StateFlow<ReclamacaoAguaUiState> = _uiState.asStateFlow()

    fun onEnderecoChange(value: String) {
        _uiState.update { it.copy(endereco = value, submitSuccess = false, submitError = null) }
    }

    fun onSetorChange(value: String) {
        _uiState.update { it.copy(setor = value, submitSuccess = false, submitError = null) }
    }

    fun onDiasSemAguaChange(value: String) {
        _uiState.update { it.copy(diasSemAgua = value.filter { ch -> ch.isDigit() }, submitSuccess = false, submitError = null) }
    }

    fun onMediaPicked(media: PickedMedia?) {
        _uiState.update { it.copy(pickedMedia = media, submitSuccess = false, submitError = null) }
    }

    fun clearSubmitStatus() {
        _uiState.update { it.copy(submitSuccess = false, submitError = null) }
    }

    fun enviarReclamacao() {
        val state = _uiState.value
        if (!state.firebaseConfigured) {
            _uiState.update {
                it.copy(submitError = "Configure o Firebase em FirebaseConfig.kt antes de enviar.")
            }
            return
        }
        if (!state.supabaseConfigured) {
            _uiState.update {
                it.copy(submitError = "Configure o Supabase em SupabaseConfig.kt para enviar fotos/vídeos.")
            }
            return
        }

        val endereco = state.endereco.trim()
        if (endereco.length < 6) {
            _uiState.update { it.copy(submitError = "Informe um endereço completo (bairro, rua ou referência).") }
            return
        }

        val dias = state.diasSemAgua.toIntOrNull()
        if (dias == null || dias < 0 || dias > 365) {
            _uiState.update { it.copy(submitError = "Informe quantos dias sem água (0 a 365).") }
            return
        }

        if (state.pickedMedia == null) {
            _uiState.update { it.copy(submitError = "Anexe uma foto ou vídeo como comprovante.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, submitError = null, submitSuccess = false) }
        scope.launch {
            val result = repository.enviarReclamacao(
                endereco = endereco,
                setor = state.setor,
                diasSemAgua = dias,
                media = state.pickedMedia,
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitSuccess = true,
                            endereco = "",
                            diasSemAgua = "",
                            pickedMedia = null,
                        )
                    }
                    carregarDashboard(force = true)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = error.message ?: "Não foi possível enviar a reclamação.",
                        )
                    }
                },
            )
        }
    }

    fun carregarDashboard(force: Boolean = false) {
        val state = _uiState.value
        if (!state.firebaseConfigured) {
            _uiState.update {
                it.copy(
                    dashboardError = "Configure o Firebase em FirebaseConfig.kt para ver o painel.",
                    dashboardLoading = false,
                )
            }
            return
        }
        if (state.dashboardLoading && !force) return

        _uiState.update { it.copy(dashboardLoading = true, dashboardError = null) }
        scope.launch {
            val result = repository.listarReclamacoes()
            result.fold(
                onSuccess = { lista ->
                    _uiState.update {
                        it.copy(
                            dashboardLoading = false,
                            reclamacoes = lista,
                            stats = repository.calcularStats(lista),
                            dashboardError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            dashboardLoading = false,
                            dashboardError = error.message ?: "Não foi possível carregar o painel.",
                        )
                    }
                },
            )
        }
    }
}
