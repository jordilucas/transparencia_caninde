package br.gov.caninde.transparencia.data

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun createHttpClient(): HttpClient

fun createConfiguredClient(engine: HttpClient): HttpClient {
    return HttpClient {
        install(WebSockets) {
            pingInterval = 20_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }
}

fun createAppModule(endpoint: WebSocketEndpoint = WebSocketEndpoint.DEFAULT): Module = module {
    single { endpoint }
    single { createHttpClient() }
    single { TransparenciaRepository(get(), get()) }
    factory { TransparenciaViewModel(get()) }
}

/** @deprecated Use [createAppModule] com endpoint explícito no Android. */
val appModule: Module get() = createAppModule()
