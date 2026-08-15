package br.gov.caninde.transparencia.data

/**
 * Projeto Supabase: jimdiiwbybfhazxeyusk
 * Dashboard: https://supabase.com/dashboard/project/jimdiiwbybfhazxeyusk
 */
object SupabaseConfig {
    const val URL = "https://jimdiiwbybfhazxeyusk.supabase.co"
    const val ANON_KEY = "sb_publishable_6gvJq5h8lI1D0K1Bjx9gDw_mdx3Ypo2"
    const val BUCKET = "reclamacoes-agua"

    val isConfigured: Boolean
        get() = URL.startsWith("https://") &&
            URL.contains("supabase.co") &&
            ANON_KEY != "CONFIGURE_ME" &&
            ANON_KEY.isNotBlank()

    fun objectUrl(objectPath: String): String =
        "${URL.trimEnd('/')}/storage/v1/object/public/$BUCKET/$objectPath"

    fun uploadUrl(objectPath: String): String =
        "${URL.trimEnd('/')}/storage/v1/object/$BUCKET/$objectPath"
}
