package br.gov.caninde.transparencia.platform

import kotlin.js.JsName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@JsName("transparenciaLogScreen")
external fun transparenciaLogScreenJs(screenName: String)

@JsName("transparenciaLogEvent")
external fun transparenciaLogEventJs(name: String, paramsJson: String)

actual fun logAnalyticsScreen(screenName: String) {
    if (screenName.isNotBlank()) {
        transparenciaLogScreenJs(screenName)
    }
}

actual fun logAnalyticsEvent(name: String, params: Map<String, String>) {
    if (name.isBlank()) return
    val json = if (params.isEmpty()) "{}" else Json.encodeToString(params)
    transparenciaLogEventJs(name, json)
}
