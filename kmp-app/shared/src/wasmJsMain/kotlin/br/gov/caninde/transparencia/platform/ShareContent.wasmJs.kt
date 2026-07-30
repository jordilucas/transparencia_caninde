package br.gov.caninde.transparencia.platform

import kotlin.js.JsName
external fun transparenciaShareContentJs(title: String, text: String)

@JsName("transparenciaHideLoading")
external fun transparenciaHideLoadingJs()

actual fun shareContent(title: String, text: String) {
    transparenciaShareContentJs(title, text)
}

actual fun hideAppLoadingScreen() {
    transparenciaHideLoadingJs()
}
