package br.gov.caninde.transparencia.data

data class WebSocketEndpoint(
    val scheme: String,
    val host: String,
    val port: Int,
    val authToken: String = "",
) {
    val url: String
        get() {
            val base = "$scheme://$host:$port"
            return if (authToken.isNotBlank()) "$base?token=$authToken" else base
        }

    companion object {
        val DEFAULT = WebSocketEndpoint(scheme = "ws", host = "10.0.2.2", port = 8080)
    }
}
