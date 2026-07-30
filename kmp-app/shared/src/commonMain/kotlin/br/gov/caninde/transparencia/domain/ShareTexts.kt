package br.gov.caninde.transparencia.domain

object ShareTexts {
    const val SITE_URL = "https://transparenciacaninde.com.br"

    fun contrato(c: Contrato): String {
        val info = c.normalized().displayInfo()
        return buildString {
            append("Contrato: ${info.titulo}")
            if (c.valor.isNotBlank()) append(" — ${c.valor}")
            append("\n\nTransparência Canindé\n$SITE_URL")
        }
    }

    fun licitacao(l: Licitacao): String {
        val info = l.displayInfo()
        return buildString {
            append("Licitação: ${info.titulo}")
            if (l.situacao.isNotBlank()) append(" (${l.situacao})")
            append("\n\nTransparência Canindé\n$SITE_URL")
        }
    }

    fun publicacao(p: Publicacao): String = buildString {
        append("Publicação: ${p.titulo.ifBlank { p.tipo }}")
        if (p.data.isNotBlank()) append(" — ${p.data}")
        append("\n\nTransparência Canindé\n$SITE_URL")
    }

    fun vereador(p: Parlamentar): String = buildString {
        append("Vereador(a): ${p.nomeCompleto.ifBlank { p.nome }}")
        if (p.partido.isNotBlank()) append(" — ${p.partido}")
        append("\n\nTransparência Canindé\n$SITE_URL")
    }

    fun materia(m: Materia): String = buildString {
        append("Matéria: ${m.titulo}")
        if (m.tipo.isNotBlank()) append(" (${m.tipo})")
        append("\n\nTransparência Canindé\n$SITE_URL")
    }

    fun secretaria(s: Secretaria): String = buildString {
        append("Secretaria: ${s.nome}")
        if (s.secretario.isNotBlank()) append(" — ${s.secretario}")
        append("\n\nTransparência Canindé\n$SITE_URL")
    }

    fun sessao(s: Sessao): String = buildString {
        append("Sessão: ${s.titulo.ifBlank { s.data }}")
        append("\n\nTransparência Canindé\n$SITE_URL")
    }

    fun paginaPortal(titulo: String): String =
        "$titulo\n\nTransparência Canindé\n$SITE_URL"
}
