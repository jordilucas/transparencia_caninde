package br.gov.caninde.transparencia.data

data class WebSocketEndpoint(
    val scheme: String,
    val host: String,
    val port: Int,
) {
    val url: String get() = "$scheme://$host:$port"

    companion object {
        val DEFAULT = WebSocketEndpoint(scheme = "ws", host = "10.0.2.2", port = 8080)
    }
}
