package br.gov.caninde.transparencia.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.data.WebSocketEndpoint
import br.gov.caninde.transparencia.data.createAppModule
import br.gov.caninde.transparencia.presentation.TransparenciaApp
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import kotlinx.browser.document

private fun webSocketEndpointForBrowser(): WebSocketEndpoint {
    val hostname = kotlinx.browser.window.location.hostname
    return when (hostname) {
        "localhost", "127.0.0.1" -> WebSocketEndpoint(
            scheme = "ws",
            host = "localhost",
            port = 8080,
        )
        else -> WebSocketEndpoint(
            scheme = "wss",
            host = "transparencia-caninde.onrender.com",
            port = 443,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(createAppModule(webSocketEndpointForBrowser()))
    }

    ComposeViewport(viewportContainer = document.body!!) {
        KoinContext {
            val viewModel: TransparenciaViewModel = koinInject()
            TransparenciaApp(viewModel)
        }
    }
}
