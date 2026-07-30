package br.gov.caninde.transparencia.presentation

import kotlin.js.JsName
import kotlinx.browser.window

private object WebHistoryBridge {
    var onPop: (() -> Unit)? = null
}

@JsName("transparenciaHistoryPop")
fun transparenciaHistoryPop() {
    WebHistoryBridge.onPop?.invoke()
}

actual fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit {
    WebHistoryBridge.onPop = { onPathChange(currentWebPath()) }
    return { WebHistoryBridge.onPop = null }
}

actual fun updateWebPath(path: String) {
    val target = if (path.isBlank() || path == "/") "/" else path
    if (window.location.pathname != target) {
        window.history.pushState(null, "", target)
    }
}

actual fun currentWebPath(): String = window.location.pathname.ifBlank { "/" }
