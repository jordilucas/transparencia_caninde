package br.gov.caninde.transparencia.data

actual object LocalDataStore {
    actual fun savePrefeitura(json: String) {}
    actual fun loadPrefeitura(): String? = null
    actual fun saveCamara(json: String) {}
    actual fun loadCamara(): String? = null
}
