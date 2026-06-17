package br.gov.caninde.transparencia.domain

data class LicitacaoDisplayInfo(
    val titulo: String,
    val descricao: String,
    val meta: String,
    val situacao: String,
)

fun Licitacao.displayInfo(): LicitacaoDisplayInfo {
    val numeroLimpo = numero.trim()
    val titulo = objeto.ifBlank {
        listOfNotNull(
            modalidade.takeIf { it.isNotBlank() },
            numeroLimpo.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { "Licitação" }
    }

    val descricao = buildList {
        if (modalidade.isNotBlank() && modalidade != titulo) add(modalidade)
        if (numeroLimpo.isNotBlank() && !titulo.contains(numeroLimpo)) add("Processo $numeroLimpo")
    }.joinToString(" · ")

    val meta = dataAbertura.takeIf { it.isNotBlank() } ?: ""

    return LicitacaoDisplayInfo(
        titulo = titulo,
        descricao = descricao,
        meta = meta,
        situacao = situacao.ifBlank { "Em andamento" },
    )
}

fun formatGestoresResumo(gestores: List<Gestor>): String {
    return gestores
        .take(2)
        .joinToString(" · ") { gestor ->
            val primeiroNome = gestor.nome.split(" ").firstOrNull().orEmpty()
            val cargoCurto = when {
                gestor.cargo.contains("Vice", ignoreCase = true) -> "Vice"
                gestor.cargo.contains("Prefeito", ignoreCase = true) -> "Prefeito"
                else -> gestor.cargo
            }
            if (primeiroNome.isNotBlank() && cargoCurto.isNotBlank()) "$primeiroNome ($cargoCurto)"
            else gestor.nome
        }
}
