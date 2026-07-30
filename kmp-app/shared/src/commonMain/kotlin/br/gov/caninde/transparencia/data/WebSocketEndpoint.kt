package br.gov.caninde.transparencia.data

data class WebSocketEndpoint(
    val scheme: String,
    val host: String,
    val port: Int,
    val authToken: String = "",
) {
    val url: String
        get() {
            val base = "$scheme://$host${portSuffix(scheme, port)}"
            return if (authToken.isNotBlank()) "$base?token=$authToken" else base
        }

    val healthCheckUrl: String
        get() {
            val httpScheme = when (scheme) {
                "wss" -> "https"
                "ws" -> "http"
                else -> "http"
            }
            return "$httpScheme://$host${portSuffix(httpScheme, port)}/health"
        }

    companion object {
        val DEFAULT = WebSocketEndpoint(scheme = "ws", host = "10.0.2.2", port = 8080)

        private fun portSuffix(scheme: String, port: Int): String = when {
            scheme == "https" && port == 443 -> ""
            scheme == "http" && port == 80 -> ""
            scheme == "wss" && port == 443 -> ""
            scheme == "ws" && port == 80 -> ""
            else -> ":$port"
        }
    }
}
