package br.gov.caninde.transparencia.domain

data class ReclamacaoAgua(
    val id: String,
    val endereco: String,
    val setor: String,
    val diasSemAgua: Int,
    val mediaUrl: String?,
    val mediaType: String?,
    val criadoEmMillis: Long,
)

data class ReclamacaoAguaStats(
    val total: Int = 0,
    val setor1: Int = 0,
    val setor2: Int = 0,
    val mediaDiasSemAgua: Double = 0.0,
)

data class ReclamacaoAguaUiState(
    val endereco: String = "",
    val setor: String = "1",
    val diasSemAgua: String = "",
    val pickedMedia: PickedMedia? = null,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,
    val dashboardLoading: Boolean = false,
    val dashboardError: String? = null,
    val reclamacoes: List<ReclamacaoAgua> = emptyList(),
    val stats: ReclamacaoAguaStats = ReclamacaoAguaStats(),
    val firebaseConfigured: Boolean = true,
    val supabaseConfigured: Boolean = true,
)

data class PickedMedia(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedMedia) return false
        return bytes.contentEquals(other.bytes) &&
            fileName == other.fileName &&
            mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
