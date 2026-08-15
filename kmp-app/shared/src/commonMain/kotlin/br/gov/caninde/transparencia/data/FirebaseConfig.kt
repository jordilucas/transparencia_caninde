package br.gov.caninde.transparencia.data

/**
 * Projeto Firebase: reclamacao-agua-caninde
 * Console: https://console.firebase.google.com/project/reclamacao-agua-caninde/overview
 */
object FirebaseConfig {
    const val API_KEY = "AIzaSyCKM4Ljn5o4khWyGAn7u-7c3EHqLmzwLjE"
    const val PROJECT_ID = "reclamacao-agua-caninde"

    val isConfigured: Boolean
        get() = API_KEY.isNotBlank() && PROJECT_ID.isNotBlank()
}
