package br.gov.caninde.transparencia.data

import br.gov.caninde.transparencia.domain.PickedMedia
import br.gov.caninde.transparencia.domain.ReclamacaoAgua
import br.gov.caninde.transparencia.domain.ReclamacaoAguaStats
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random

class ReclamacaoAguaRepository(
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedIdToken: String? = null
    private var tokenExpiresAtMillis: Long = 0L

    suspend fun enviarReclamacao(
        endereco: String,
        setor: String,
        diasSemAgua: Int,
        media: PickedMedia?,
    ): Result<Unit> = runCatching {
        ensureBackendConfigured(requireMedia = media != null)

        val docId = generateDocId()
        var mediaUrl: String? = null
        var mediaType: String? = null

        if (media != null) {
            val upload = uploadMediaSupabase(docId, media)
            mediaUrl = upload.url
            mediaType = upload.mediaType
        }

        val idToken = ensureFirebaseIdToken()
        val body = buildJsonObject {
            putJsonObject("fields") {
                putJsonObject("endereco") { put("stringValue", endereco.trim()) }
                putJsonObject("setor") { put("stringValue", setor) }
                putJsonObject("diasSemAgua") { put("integerValue", diasSemAgua.toString()) }
                putJsonObject("criadoEmMillis") {
                    put("integerValue", currentTimeMillis().toString())
                }
                if (mediaUrl != null) {
                    putJsonObject("mediaUrl") { put("stringValue", mediaUrl) }
                }
                if (mediaType != null) {
                    putJsonObject("mediaType") { put("stringValue", mediaType) }
                }
            }
        }

        val response = httpClient.post(firestoreDocUrl(docId)) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $idToken")
            setBody(body.toString())
        }

        if (!response.status.isSuccess()) {
            error(parseFirebaseError(response))
        }
    }

    suspend fun listarReclamacoes(): Result<List<ReclamacaoAgua>> = runCatching {
        require(FirebaseConfig.isConfigured) {
            "Firebase não configurado. Edite FirebaseConfig.kt."
        }

        val idToken = ensureFirebaseIdToken()
        val response = httpClient.get(firestoreCollectionUrl()) {
            header(HttpHeaders.Authorization, "Bearer $idToken")
            parameter("pageSize", "200")
        }

        if (!response.status.isSuccess()) {
            error(parseFirebaseError(response))
        }

        val payload = json.decodeFromString<FirestoreListResponse>(response.bodyAsText())
        payload.documents.orEmpty()
            .mapNotNull { it.toReclamacao() }
            .sortedByDescending { it.criadoEmMillis }
    }

    fun calcularStats(reclamacoes: List<ReclamacaoAgua>): ReclamacaoAguaStats {
        if (reclamacoes.isEmpty()) return ReclamacaoAguaStats()
        val setor1 = reclamacoes.count { it.setor == "1" }
        val setor2 = reclamacoes.count { it.setor == "2" }
        val mediaDias = reclamacoes.map { it.diasSemAgua }.average()
        return ReclamacaoAguaStats(
            total = reclamacoes.size,
            setor1 = setor1,
            setor2 = setor2,
            mediaDiasSemAgua = mediaDias,
        )
    }

    private fun ensureBackendConfigured(requireMedia: Boolean) {
        require(FirebaseConfig.isConfigured) {
            "Firebase não configurado. Edite FirebaseConfig.kt."
        }
        if (requireMedia) {
            require(SupabaseConfig.isConfigured) {
                "Supabase não configurado. Edite SupabaseConfig.kt (URL e ANON_KEY) para enviar fotos/vídeos."
            }
        }
    }

    private suspend fun ensureFirebaseIdToken(): String {
        val now = currentTimeMillis()
        val cached = cachedIdToken
        if (cached != null && now < tokenExpiresAtMillis - 60_000) {
            return cached
        }

        val response = httpClient.post(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FirebaseConfig.API_KEY}",
        ) {
            contentType(ContentType.Application.Json)
            setBody("""{"returnSecureToken":true}""")
        }

        if (!response.status.isSuccess()) {
            error(parseFirebaseError(response))
        }

        val auth = json.decodeFromString<FirebaseAuthResponse>(response.bodyAsText())
        cachedIdToken = auth.idToken ?: error("Token Firebase indisponível.")
        val expiresInSec = auth.expiresIn?.toLongOrNull() ?: 3600L
        tokenExpiresAtMillis = now + expiresInSec * 1000
        return cachedIdToken!!
    }

    private suspend fun uploadMediaSupabase(
        docId: String,
        media: PickedMedia,
    ): UploadResult {
        require(media.bytes.size <= MAX_MEDIA_BYTES) {
            "Arquivo muito grande. Máximo: ${MAX_MEDIA_BYTES / (1024 * 1024)} MB."
        }

        val safeName = sanitizeFileName(media.fileName, media.mimeType)
        val objectPath = "reclamacoes/$docId/$safeName"
        val response = httpClient.post(SupabaseConfig.uploadUrl(objectPath)) {
            header(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.ANON_KEY}")
            header("apikey", SupabaseConfig.ANON_KEY)
            header("x-upsert", "false")
            contentType(ContentType.parse(media.mimeType))
            setBody(media.bytes)
        }

        if (!response.status.isSuccess()) {
            error(parseSupabaseError(response))
        }

        return UploadResult(
            url = SupabaseConfig.objectUrl(objectPath),
            mediaType = media.mimeType,
        )
    }

    private fun sanitizeFileName(rawName: String, mimeType: String): String {
        val base = rawName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .take(40)
            .ifBlank { if (mimeType.startsWith("video/")) "video" else "foto" }
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("quicktime") -> "mov"
            mimeType.startsWith("video/") -> "mp4"
            else -> "jpg"
        }
        return "${base}_${Random.nextInt(1000, 9999)}.$ext"
    }

    private fun firestoreCollectionUrl(): String =
        "https://firestore.googleapis.com/v1/projects/${FirebaseConfig.PROJECT_ID}/databases/(default)/documents/reclamacoes"

    private fun firestoreDocUrl(docId: String): String =
        "${firestoreCollectionUrl()}?documentId=$docId"

    private fun generateDocId(): String {
        val millis = currentTimeMillis()
        val suffix = Random.nextInt(1000, 9999)
        return "rec_${millis}_$suffix"
    }

    private suspend fun parseFirebaseError(response: HttpResponse): String {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching {
            json.decodeFromString<FirebaseErrorResponse>(body)
        }.getOrNull()
        return parsed?.error?.message ?: "Erro Firebase (${response.status.value})"
    }

    private suspend fun parseSupabaseError(response: HttpResponse): String {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching {
            json.decodeFromString<SupabaseErrorResponse>(body)
        }.getOrNull()
        return parsed?.message ?: parsed?.error ?: "Erro Supabase (${response.status.value})"
    }

    private fun FirestoreDocument.toReclamacao(): ReclamacaoAgua? {
        val fields = fields ?: return null
        val endereco = fields.string("endereco") ?: return null
        val setor = fields.string("setor") ?: return null
        val dias = fields.int("diasSemAgua") ?: return null
        val criadoEm = fields.long("criadoEmMillis") ?: 0L
        val id = name.substringAfterLast('/')
        return ReclamacaoAgua(
            id = id,
            endereco = endereco,
            setor = setor,
            diasSemAgua = dias,
            mediaUrl = fields.string("mediaUrl"),
            mediaType = fields.string("mediaType"),
            criadoEmMillis = criadoEm,
        )
    }

    private fun Map<String, FirestoreValue>.string(key: String): String? =
        this[key]?.stringValue

    private fun Map<String, FirestoreValue>.int(key: String): Int? =
        this[key]?.integerValue?.toIntOrNull()

    private fun Map<String, FirestoreValue>.long(key: String): Long? =
        this[key]?.integerValue?.toLongOrNull()

    private data class UploadResult(val url: String, val mediaType: String)

    companion object {
        private const val MAX_MEDIA_BYTES = 50 * 1024 * 1024
    }
}

@Serializable
private data class FirebaseAuthResponse(
    @SerialName("idToken") val idToken: String? = null,
    @SerialName("expiresIn") val expiresIn: String? = null,
)

@Serializable
private data class FirebaseErrorResponse(
    val error: FirebaseErrorBody? = null,
)

@Serializable
private data class FirebaseErrorBody(
    val message: String? = null,
)

@Serializable
private data class FirestoreListResponse(
    val documents: List<FirestoreDocument>? = null,
)

@Serializable
private data class FirestoreDocument(
    val name: String = "",
    val fields: Map<String, FirestoreValue>? = null,
)

@Serializable
private data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
)

@Serializable
private data class SupabaseErrorResponse(
    val message: String? = null,
    val error: String? = null,
)

expect fun currentTimeMillis(): Long
