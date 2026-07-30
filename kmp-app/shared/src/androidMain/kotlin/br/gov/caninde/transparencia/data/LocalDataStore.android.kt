package br.gov.caninde.transparencia.data

import android.content.Context

private var appContext: Context? = null

fun initLocalDataStore(context: Context) {
    appContext = context.applicationContext
}

actual object LocalDataStore {
    private const val PREFS = "transparencia_cache"
    private const val KEY_PREF = "prefeitura"
    private const val KEY_CAM = "camara"

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun savePrefeitura(json: String) {
        prefs()?.edit()?.putString(KEY_PREF, json)?.apply()
    }

    actual fun loadPrefeitura(): String? = prefs()?.getString(KEY_PREF, null)

    actual fun saveCamara(json: String) {
        prefs()?.edit()?.putString(KEY_CAM, json)?.apply()
    }

    actual fun loadCamara(): String? = prefs()?.getString(KEY_CAM, null)
}
