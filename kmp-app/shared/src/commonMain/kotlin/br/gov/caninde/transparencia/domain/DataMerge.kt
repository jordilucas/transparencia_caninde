package br.gov.caninde.transparencia.domain

/**
 * Mescla listas do cliente mantendo sempre o registro mais recente/completo,
 * independente da fonte (JSON, HTML ou broadcast anterior).
 */
object DataMerge {

    fun mergeContratos(existing: List<Contrato>, incoming: List<Contrato>): List<Contrato> =
        mergeByKey(existing, incoming, ::contratoKey, ::contratoRecency) { a, b -> mergeContrato(a, b) }

    fun mergeLicitacoes(existing: List<Licitacao>, incoming: List<Licitacao>): List<Licitacao> =
        mergeByKey(existing, incoming, ::licitacaoKey, ::licitacaoRecency) { a, b -> mergeLicitacao(a, b) }

    fun mergeSecretarias(existing: List<Secretaria>, incoming: List<Secretaria>): List<Secretaria> =
        mergeByKey(existing, incoming, ::secretariaKey, ::secretariaRecency) { a, b -> mergeSecretaria(a, b) }

    fun mergePublicacoes(existing: List<Publicacao>, incoming: List<Publicacao>): List<Publicacao> =
        mergeByKey(existing, incoming, ::publicacaoKey, ::publicacaoRecency) { a, b -> mergePublicacao(a, b) }

    fun mergeSessoes(existing: List<Sessao>, incoming: List<Sessao>): List<Sessao> =
        mergeByKey(existing, incoming, ::sessaoKey, ::sessaoRecency) { a, b -> mergeSessao(a, b) }

    fun mergeMaterias(existing: List<Materia>, incoming: List<Materia>): List<Materia> =
        mergeByKey(existing, incoming, ::materiaKey, ::materiaRecency) { a, b -> mergeMateria(a, b) }

    fun mergeParlamentares(existing: List<Parlamentar>, incoming: List<Parlamentar>): List<Parlamentar> =
        mergeByKey(existing, incoming, ::parlamentarKey, ::parlamentarRecency) { a, b -> mergeParlamentar(a, b) }

    fun mergeDiarios(existing: List<String>, incoming: List<String>): List<String> {
        val all = (existing + incoming).distinctBy { it.trim().lowercase() }
        return all.sortedByDescending { parseBrazilianDate(extractDateFromText(it)) }
    }

    fun mergeResumoPrefeitura(existing: ResumoPrefeitura, incoming: ResumoPrefeitura): ResumoPrefeitura {
        val fontes = (existing.fontesUtilizadas + incoming.fontesUtilizadas).distinct()
        return incoming.copy(
            totalContratos = maxOf(existing.totalContratos, incoming.totalContratos),
            totalLicitacoes = maxOf(existing.totalLicitacoes, incoming.totalLicitacoes),
            totalPublicacoes = maxOf(existing.totalPublicacoes, incoming.totalPublicacoes),
            fontesUtilizadas = fontes,
        )
    }

    private fun <T> mergeByKey(
        existing: List<T>,
        incoming: List<T>,
        keyFn: (T) -> String,
        recencyFn: (T) -> Long,
        mergeFn: (T, T) -> T,
    ): List<T> {
        val map = linkedMapOf<String, T>()
        for (item in existing + incoming) {
            val key = keyFn(item)
            if (key.isBlank()) continue
            val prev = map[key]
            map[key] = when {
                prev == null -> item
                recencyFn(item) > recencyFn(prev) -> mergeFn(item, prev)
                recencyFn(item) < recencyFn(prev) -> mergeFn(prev, item)
                else -> mergeFn(item, prev)
            }
        }
        return map.values.sortedByDescending { recencyFn(it) }
    }

    private fun contratoKey(c: Contrato): String {
        val n = c.numero.trim().lowercase()
        if (n.isNotBlank()) return "n:$n"
        return if (c.id.isNotBlank()) "id:${c.id}" else ""
    }

    private fun contratoRecency(c: Contrato): Long = parseBrazilianDate(c.data)

    private fun mergeContrato(preferred: Contrato, fallback: Contrato) = preferred.copy(
        id = preferred.id.ifBlank { fallback.id },
        objeto = preferred.objeto.ifBlank { fallback.objeto },
        valor = preferred.valor.ifBlank { fallback.valor },
        empresa = preferred.empresa.ifBlank { fallback.empresa },
        data = preferred.data.ifBlank { fallback.data },
        url = preferred.url.ifBlank { fallback.url },
        cnpjCredor = preferred.cnpjCredor.ifBlank { fallback.cnpjCredor },
        secretaria = preferred.secretaria.ifBlank { fallback.secretaria },
        modalidade = preferred.modalidade.ifBlank { fallback.modalidade },
        pdfUrl = preferred.pdfUrl.ifBlank { fallback.pdfUrl },
    )

    private fun licitacaoKey(l: Licitacao): String {
        val n = l.numero.trim().lowercase()
        if (n.isNotBlank()) return "n:$n"
        return if (l.id.isNotBlank()) "id:${l.id}" else ""
    }

    private fun licitacaoRecency(l: Licitacao): Long = parseBrazilianDate(l.dataAbertura)

    private fun mergeLicitacao(preferred: Licitacao, fallback: Licitacao) = preferred.copy(
        id = preferred.id.ifBlank { fallback.id },
        modalidade = preferred.modalidade.ifBlank { fallback.modalidade },
        objeto = preferred.objeto.ifBlank { fallback.objeto },
        situacao = preferred.situacao.ifBlank { fallback.situacao },
        url = preferred.url.ifBlank { fallback.url },
        dataAbertura = preferred.dataAbertura.ifBlank { fallback.dataAbertura },
    )

    private fun secretariaKey(s: Secretaria): String {
        if (s.id.isNotBlank()) return "id:${s.id}"
        val nome = s.nome.trim().lowercase()
        return if (nome.isNotBlank()) "nome:$nome" else ""
    }

    private fun secretariaRecency(s: Secretaria): Long {
        var score = 0L
        if (s.secretario.isNotBlank()) score += 100_000
        if (s.contato.email.isNotBlank()) score += 10_000
        if (s.contato.telefone.isNotBlank()) score += 1_000
        if (s.contato.horarioFuncionamento.isNotBlank()) score += 100
        return score
    }

    private fun mergeSecretaria(preferred: Secretaria, fallback: Secretaria) = preferred.copy(
        nome = preferred.nome.ifBlank { fallback.nome },
        secretario = preferred.secretario.ifBlank { fallback.secretario },
        url = preferred.url.ifBlank { fallback.url },
        contato = mergeContato(preferred.contato, fallback.contato),
    )

    private fun mergeContato(preferred: Contato, fallback: Contato) = preferred.copy(
        email = preferred.email.ifBlank { fallback.email },
        telefone = preferred.telefone.ifBlank { fallback.telefone },
        whatsapp = preferred.whatsapp.ifBlank { fallback.whatsapp },
        endereco = preferred.endereco.ifBlank { fallback.endereco },
        horarioFuncionamento = preferred.horarioFuncionamento.ifBlank { fallback.horarioFuncionamento },
    )

    private fun publicacaoKey(p: Publicacao): String {
        if (p.id.isNotBlank()) return "id:${p.id}"
        val t = p.titulo.trim().lowercase().take(80)
        return if (t.isNotBlank()) "t:$t|${p.data.trim()}" else ""
    }

    private fun publicacaoRecency(p: Publicacao): Long = parseBrazilianDate(p.data)

    private fun mergePublicacao(preferred: Publicacao, fallback: Publicacao) = preferred.copy(
        titulo = preferred.titulo.ifBlank { fallback.titulo },
        tipo = preferred.tipo.ifBlank { fallback.tipo },
        data = preferred.data.ifBlank { fallback.data },
        url = preferred.url.ifBlank { fallback.url },
    )

    private fun sessaoKey(s: Sessao): String {
        val t = s.titulo.trim().lowercase()
        return if (t.isNotBlank()) "t:$t|${s.data.trim()}" else ""
    }

    private fun sessaoRecency(s: Sessao): Long = parseBrazilianDate(s.data)

    private fun mergeSessao(preferred: Sessao, fallback: Sessao) = preferred.copy(
        titulo = preferred.titulo.ifBlank { fallback.titulo },
        data = preferred.data.ifBlank { fallback.data },
        url = preferred.url.ifBlank { fallback.url },
        resumo = preferred.resumo.ifBlank { fallback.resumo },
    )

    private fun materiaKey(m: Materia): String {
        if (m.slug.isNotBlank()) return "slug:${m.slug}"
        val t = m.titulo.trim().lowercase()
        return if (t.isNotBlank()) "t:$t" else ""
    }

    private fun materiaRecency(m: Materia): Long = parseBrazilianDate(m.dataPublicacao)

    private fun mergeMateria(preferred: Materia, fallback: Materia) = preferred.copy(
        titulo = preferred.titulo.ifBlank { fallback.titulo },
        tipo = preferred.tipo.ifBlank { fallback.tipo },
        slug = preferred.slug.ifBlank { fallback.slug },
        url = preferred.url.ifBlank { fallback.url },
        autor = preferred.autor.ifBlank { fallback.autor },
        dataPublicacao = preferred.dataPublicacao.ifBlank { fallback.dataPublicacao },
        pdfUrl = preferred.pdfUrl.ifBlank { fallback.pdfUrl },
        resumo = preferred.resumo.ifBlank { fallback.resumo },
    )

    private fun parlamentarKey(p: Parlamentar): String {
        if (p.slug.isNotBlank()) return "slug:${p.slug}"
        return p.nome.trim().lowercase().let { if (it.isNotBlank()) "nome:$it" else "" }
    }

    private fun parlamentarRecency(p: Parlamentar): Long {
        var score = 0L
        if (p.biografia.isNotBlank()) score += 10_000
        if (p.contato.email.isNotBlank()) score += 1_000
        if (p.contato.whatsapp.isNotBlank()) score += 100
        if (p.foto.isNotBlank()) score += 10
        return score
    }

    private fun mergeParlamentar(preferred: Parlamentar, fallback: Parlamentar) = preferred.copy(
        nome = preferred.nome.ifBlank { fallback.nome },
        nomeCompleto = preferred.nomeCompleto.ifBlank { fallback.nomeCompleto },
        partido = preferred.partido.ifBlank { fallback.partido },
        cargo = preferred.cargo.ifBlank { fallback.cargo },
        foto = preferred.foto.ifBlank { fallback.foto },
        slug = preferred.slug.ifBlank { fallback.slug },
        profileUrl = preferred.profileUrl.ifBlank { fallback.profileUrl },
        biografia = preferred.biografia.ifBlank { fallback.biografia },
        contato = mergeContato(preferred.contato, fallback.contato),
    )

    private fun extractDateFromText(text: String): String {
        val m = Regex("""(\d{1,2}/\d{1,2}/\d{4})""").find(text)
        return m?.groupValues?.get(1).orEmpty()
    }

    private fun parseBrazilianDate(str: String): Long {
        if (str.isBlank()) return 0L
        val m = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""").find(str.trim()) ?: return 0L
        val day = m.groupValues[1].toIntOrNull() ?: return 0L
        val month = m.groupValues[2].toIntOrNull() ?: return 0L
        val year = m.groupValues[3].toIntOrNull() ?: return 0L
        if (month !in 1..12 || day !in 1..31 || year < 1900) return 0L
        return year * 10_000L + month * 100L + day
    }
}
