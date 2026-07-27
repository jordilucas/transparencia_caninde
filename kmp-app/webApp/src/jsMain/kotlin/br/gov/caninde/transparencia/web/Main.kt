package br.gov.caninde.transparencia.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.data.WebSocketEndpoint
import br.gov.caninde.transparencia.data.createAppModule
import br.gov.caninde.transparencia.presentation.TransparenciaApp
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

private fun webSocketEndpointForBrowser(): WebSocketEndpoint {
    val hostname = window.location.hostname
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
private fun launchApp() {
    ComposeViewport(viewportContainer = document.body!!) {
        KoinContext {
            val viewModel: TransparenciaViewModel = koinInject()
            TransparenciaApp(viewModel)
        }
    }
    window.setTimeout({
        window.dispatchEvent(org.w3c.dom.events.Event("resize"))
    }, 0)
}

fun main() {
    startKoin {
        modules(createAppModule(webSocketEndpointForBrowser()))
    }

    if (document.readyState.toString() == "complete") {
        launchApp()
    } else {
        window.onload = { launchApp() }
    }
}
