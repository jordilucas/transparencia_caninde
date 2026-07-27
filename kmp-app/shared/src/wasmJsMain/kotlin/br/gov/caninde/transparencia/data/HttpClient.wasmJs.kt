package br.gov.caninde.transparencia.data

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient = HttpClient(Js) {
    install(WebSockets) {
        pingInterval = 20_000.milliseconds
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}
