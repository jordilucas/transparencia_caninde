package br.gov.caninde.transparencia.platform

import kotlin.js.JsName

@JsName("transparenciaDownloadText")
external fun transparenciaDownloadTextJs(fileName: String, content: String, mimeType: String)

actual fun downloadTextFile(fileName: String, content: String, mimeType: String) {
    transparenciaDownloadTextJs(fileName, content, mimeType)
}
