package br.gov.caninde.transparencia.platform

import kotlinx.browser.window

actual fun openExternalUrl(url: String) {
    if (url.isBlank()) return
    window.open(url, "_blank")
}
