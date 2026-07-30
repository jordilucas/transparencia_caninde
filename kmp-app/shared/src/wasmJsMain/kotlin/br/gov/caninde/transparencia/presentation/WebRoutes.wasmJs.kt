package br.gov.caninde.transparencia.presentation

import kotlinx.browser.window
import org.w3c.dom.events.Event

private external fun encodeURIComponent(value: String): String

private external fun decodeURIComponent(value: String): String

actual fun encodeUriSegment(value: String): String = encodeURIComponent(value)

actual fun decodeUriSegment(value: String): String = runCatching {
    decodeURIComponent(value)
}.getOrDefault(value)

actual fun currentWebLocation(): String = readWebLocation(
    pathname = window.location.pathname,
    hash = window.location.hash,
)

actual fun updateWebPath(path: String, replace: Boolean) {
    val normalized = path.trim().ifBlank { "/" }
    val hash = if (normalized == "/") "#/" else "#$normalized"
    val url = window.location.pathname + window.location.search + hash
    if (replace) {
        window.history.replaceState(null, "", url)
    } else {
        window.history.pushState(null, "", url)
    }
}

actual fun webHistoryBack() {
    window.history.back()
}

actual fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit {
    val handler: (Event) -> Unit = {
        onPathChange(currentWebLocation())
    }
    window.addEventListener("popstate", handler)
    window.addEventListener("hashchange", handler)
    return {
        window.removeEventListener("popstate", handler)
        window.removeEventListener("hashchange", handler)
    }
}
