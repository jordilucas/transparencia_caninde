package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TransparenciaRepository(
    private val client: HttpClient,
    private val endpoint: WebSocketEndpoint,
    private val messageHandler: WsMessageHandler = WsMessageHandler(),
) {

    companion object {
        const val RECONNECT_DELAY_MS = 5_000L
        const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _prefeituraState = MutableStateFlow(PrefeituraUiState())
    val prefeituraState: StateFlow<PrefeituraUiState> = _prefeituraState.asStateFlow()

    private val _camaraState = MutableStateFlow(CamaraUiState())
    val camaraState: StateFlow<CamaraUiState> = _camaraState.asStateFlow()

    private var wsSession: DefaultClientWebSocketSession? = null
    private var connectJob: Job? = null

    fun connect(scope: CoroutineScope) {
        connectJob?.cancel()
        connectJob = scope.launch {
            var attempt = 0
            while (isActive && attempt < MAX_RECONNECT_ATTEMPTS) {
                try {
                    _connectionState.value = if (attempt == 0)
                        ConnectionState.Connecting
                    else
                        ConnectionState.Reconnecting

                    client.webSocket(urlString = endpoint.url) {
                        val session = this
                        wsSession = session
                        attempt = 0
                        _connectionState.value = ConnectionState.Connected

                        sendMessage("""{"type":"REQUEST_PREFEITURA"}""")
                        sendMessage("""{"type":"REQUEST_CAMARA"}""")

                        val pingJob = launch {
                            while (isActive) {
                                delay(30_000)
                                sendMessage("""{"type":"PING"}""")
                            }
                        }

                        try {
                            for (frame in session.incoming) {
                                if (frame is Frame.Text) {
                                    processMessage(frame.readText())
                                }
                            }
                        } finally {
                            pingJob.cancel()
                            wsSession = null
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Erro desconhecido")
                    attempt++
                    if (attempt < MAX_RECONNECT_ATTEMPTS) {
                        delay(RECONNECT_DELAY_MS * attempt.coerceAtMost(3))
                    }
                }
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        wsSession = null
        _connectionState.value = ConnectionState.Connecting
    }

    suspend fun requestRefresh(source: String = "all") {
        sendMessage("""{"type":"REQUEST_REFRESH","source":"$source"}""")
    }

    private fun processMessage(raw: String) {
        try {
            val reduced = messageHandler.reduce(
                WsHandlerState(_prefeituraState.value, _camaraState.value),
                raw,
            )
            _prefeituraState.value = reduced.prefeitura
            _camaraState.value = reduced.camara
        } catch (e: Exception) {
            println("[WS] erro ao parsear mensagem: ${e.message}")
        }
    }

    private suspend fun sendMessage(msg: String) {
        try {
            wsSession?.send(Frame.Text(msg))
        } catch (e: Exception) {
            println("[WS] erro ao enviar: ${e.message}")
        }
    }
}
