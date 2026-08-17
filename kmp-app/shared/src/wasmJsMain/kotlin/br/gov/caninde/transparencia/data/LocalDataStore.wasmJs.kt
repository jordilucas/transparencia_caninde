package br.gov.caninde.transparencia.data

import kotlin.js.JsName

@JsName("transparenciaCacheSave")
external fun transparenciaCacheSaveJs(key: String, value: String)

@JsName("transparenciaCacheLoad")
external fun transparenciaCacheLoadJs(key: String): String?

actual object LocalDataStore {
    private const val KEY_PREF = "prefeitura"
    private const val KEY_CAM = "camara"

    actual fun savePrefeitura(json: String) {
        transparenciaCacheSaveJs(KEY_PREF, json)
    }

    actual fun loadPrefeitura(): String? = transparenciaCacheLoadJs(KEY_PREF)

    actual fun saveCamara(json: String) {
        transparenciaCacheSaveJs(KEY_CAM, json)
    }

    actual fun loadCamara(): String? = transparenciaCacheLoadJs(KEY_CAM)

    private const val KEY_RECENT = "recent_searches"

    actual fun saveRecentSearches(json: String) {
        transparenciaCacheSaveJs(KEY_RECENT, json)
    }

    actual fun loadRecentSearches(): String? = transparenciaCacheLoadJs(KEY_RECENT)
}
