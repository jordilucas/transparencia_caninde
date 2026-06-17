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
        const val CONNECT_TIMEOUT_MS = 20_000L
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _prefeituraState = MutableStateFlow(PrefeituraUiState())
    val prefeituraState: StateFlow<PrefeituraUiState> = _prefeituraState.asStateFlow()

    private val _camaraState = MutableStateFlow(CamaraUiState())
    val camaraState: StateFlow<CamaraUiState> = _camaraState.asStateFlow()

    private val _detailState = MutableStateFlow(DetailUiState())
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    private val detailCache = mutableMapOf<String, DetailUiState>()
    private var wsSession: DefaultClientWebSocketSession? = null
    private var connectJob: Job? = null
    private var ownerScope: CoroutineScope? = null
    @Volatile private var resetReconnectAttempts = false

    fun connect(scope: CoroutineScope) {
        ownerScope = scope
        connectJob?.cancel()
        connectJob = scope.launch {
            var attempt = 0
            while (isActive && attempt < MAX_RECONNECT_ATTEMPTS) {
                if (resetReconnectAttempts) {
                    attempt = 0
                    resetReconnectAttempts = false
                }
                try {
                    _connectionState.value = if (attempt == 0) {
                        ConnectionState.Connecting
                    } else {
                        ConnectionState.Reconnecting
                    }

                    client.webSocket(urlString = endpoint.url) {
                        val session = this
                        wsSession = session
                        attempt = 0
                        _connectionState.value = ConnectionState.Connected

                        sendFrame("""{"type":"REQUEST_PREFEITURA"}""")
                        sendFrame("""{"type":"REQUEST_CAMARA"}""")

                        val pingJob = launch {
                            while (isActive) {
                                delay(30_000)
                                sendFrame("""{"type":"PING"}""")
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
                    _connectionState.value = ConnectionState.Error
                    attempt++
                    if (attempt < MAX_RECONNECT_ATTEMPTS) {
                        delay(RECONNECT_DELAY_MS * attempt.coerceAtMost(3))
                    }
                }
            }
            if (isActive) {
                _connectionState.value = ConnectionState.Error
            }
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        wsSession = null
        _connectionState.value = ConnectionState.Connecting
    }

    fun isConnected(): Boolean =
        _connectionState.value is ConnectionState.Connected && wsSession != null

    fun forceReconnect() {
        val scope = ownerScope ?: return
        resetReconnectAttempts = true
        connectJob?.cancel()
        wsSession = null
        connect(scope)
    }

    suspend fun refreshSource(source: String) {
        setLoadingForSource(source, loading = true)

        if (!isConnected()) {
            forceReconnect()
            val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                connectionState.first { it is ConnectionState.Connected }
            } != null
            if (!connected) {
                setLoadingForSource(source, loading = false)
                setConnectionErrorForSource(source)
                return
            }
        }

        val sent = sendFrame("""{"type":"REQUEST_REFRESH","source":"$source"}""")
        if (!sent) {
            forceReconnect()
            setLoadingForSource(source, loading = false)
            setConnectionErrorForSource(source)
        }
    }

    suspend fun loadDetail(entity: DetailEntity, id: String) {
        val cacheKey = "${entity.name}:$id"
        detailCache[cacheKey]?.let {
            _detailState.value = it
            return
        }
        _detailState.value = DetailUiState(isLoading = true, entity = entity, entityId = id, payload = null, error = null)
        if (!sendFrame(messageHandler.buildRequestDetail(entity, id))) {
            _detailState.value = DetailUiState(
                isLoading = false,
                entity = entity,
                entityId = id,
                error = "Sem conexão com o servidor",
            )
        }
    }

    private fun setLoadingForSource(source: String, loading: Boolean) {
        when (source) {
            "prefeitura" -> _prefeituraState.update { it.copy(isLoading = loading) }
            "camara" -> _camaraState.update { it.copy(isLoading = loading) }
            else -> {
                _prefeituraState.update { it.copy(isLoading = loading) }
                _camaraState.update { it.copy(isLoading = loading) }
            }
        }
    }

    private fun setConnectionErrorForSource(source: String) {
        val msg = "Sem conexão com o servidor"
        when (source) {
            "prefeitura" -> _prefeituraState.update { it.copy(error = msg) }
            "camara" -> _camaraState.update { it.copy(error = msg) }
            else -> {
                _prefeituraState.update { it.copy(error = msg) }
                _camaraState.update { it.copy(error = msg) }
            }
        }
    }

    private fun processMessage(raw: String) {
        try {
            val reduced = messageHandler.reduce(
                WsHandlerState(_prefeituraState.value, _camaraState.value, _detailState.value),
                raw,
            )
            _prefeituraState.value = reduced.prefeitura
            _camaraState.value = reduced.camara
            _detailState.value = reduced.detail
            val msg = messageHandler.parse(raw)
            if (msg.type == "DETAIL_DATA" && msg.payload != null && reduced.detail.error.isNullOrBlank()) {
                val key = "${msg.payload.entity}:${msg.payload.entityId}"
                detailCache[key] = reduced.detail
            }
        } catch (e: Exception) {
            println("[WS] erro ao parsear mensagem: ${e.message}")
        }
    }

    private suspend fun sendFrame(msg: String): Boolean {
        val session = wsSession ?: return false
        return try {
            session.send(Frame.Text(msg))
            true
        } catch (e: Exception) {
            println("[WS] erro ao enviar: ${e.message}")
            wsSession = null
            if (_connectionState.value is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Error
            }
            false
        }
    }
}
