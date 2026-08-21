package br.gov.caninde.transparencia.domain

const val SEARCH_PAGE_SIZE = 8

enum class SearchEntityFilter(val label: String) {
    Todos("Todos"),
    Contratos("Contratos"),
    Licitacoes("Licitações"),
    Vereadores("Vereadores"),
    Sessoes("Sessões"),
    Materias("Matérias"),
    Publicacoes("Publicações"),
    Documentos("Documentos"),
}

sealed class SearchHit {
    abstract val title: String
    abstract val subtitle: String
    abstract val section: String

    data class ContratoHit(val item: Contrato) : SearchHit() {
        override val title get() = item.objeto.ifBlank { item.numero }
        override val subtitle get() = listOfNotNull(item.numero, item.empresa).joinToString(" · ")
        override val section = "Contratos"
    }

    data class LicitacaoHit(val item: Licitacao) : SearchHit() {
        override val title get() = item.objeto.ifBlank { item.numero }
        override val subtitle get() = listOfNotNull(item.numero, item.modalidade).joinToString(" · ")
        override val section = "Licitações"
    }

    data class SecretariaHit(val item: Secretaria) : SearchHit() {
        override val title get() = item.nome
        override val subtitle get() = item.secretario
        override val section = "Secretarias"
    }

    data class PublicacaoHit(val item: Publicacao) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = listOfNotNull(item.tipo, item.data).joinToString(" · ")
        override val section = "Publicações"
    }

    data class ObraHit(val item: Obra) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = listOfNotNull(item.secretaria, item.valor).joinToString(" · ")
        override val section = "Obras"
    }

    data class LrfHit(val item: LrfDocumento) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = item.tipo
        override val section = "LRF"
    }

    data class GestorHit(val item: Gestor) : SearchHit() {
        override val title get() = item.nome
        override val subtitle get() = item.cargo
        override val section = "Gestores"
    }

    data class ParlamentarHit(val item: Parlamentar) : SearchHit() {
        override val title get() = item.nome
        override val subtitle get() = listOfNotNull(item.partido, item.cargo).joinToString(" · ")
        override val section = "Vereadores"
    }

    data class MateriaHit(val item: Materia) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = item.tipo
        override val section = "Matérias"
    }

    data class SessaoHit(val index: Int, val item: Sessao) : SearchHit() {
        override val title get() = item.titulo.ifBlank { "Sessão ${index + 1}" }
        override val subtitle get() = item.data
        override val section = "Sessões"
    }

    data class MesaHit(val item: MembroMesa) : SearchHit() {
        override val title get() = item.nome
        override val subtitle get() = item.cargo
        override val section = "Mesa diretora"
    }

    data class LinkCamaraHit(val item: LinkExterno) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = item.categoria.replaceFirstChar { it.uppercase() }
        override val section = "Transparência Câmara"
    }

    data class LinkPrefeituraHit(val item: LinkExterno) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = listOfNotNull(
            item.secao.takeIf { it.isNotBlank() },
            item.categoria.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() },
        ).joinToString(" · ")
        override val section = "Transparência Prefeitura"
    }

    data class DocumentoCamaraHit(val item: DocumentoCamara) : SearchHit() {
        override val title get() = item.titulo
        override val subtitle get() = listOfNotNull(
            item.categoria.takeIf { it.isNotBlank() },
            item.data.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        override val section = "Documentos Câmara"
    }
}

object SearchIndex {

    fun search(
        prefeitura: PrefeituraUiState,
        camara: CamaraUiState,
        query: String,
        scope: SearchScope,
        entityFilter: SearchEntityFilter = SearchEntityFilter.Todos,
    ): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val showPref = scope == SearchScope.Tudo || scope == SearchScope.Prefeitura
        val showCam = scope == SearchScope.Tudo || scope == SearchScope.Camara
        val hits = mutableListOf<SearchHit>()

        fun allow(filter: SearchEntityFilter) =
            entityFilter == SearchEntityFilter.Todos || entityFilter == filter

        if (showPref && allow(SearchEntityFilter.Contratos)) {
            prefeitura.contratos.filter {
                matchesAnySearch(q, it.objeto, it.empresa, it.numero, it.secretaria)
            }.forEach { hits.add(SearchHit.ContratoHit(it)) }
        }
        if (showPref && allow(SearchEntityFilter.Licitacoes)) {
            prefeitura.licitacoes.filter {
                matchesAnySearch(q, it.objeto, it.numero, it.modalidade, it.situacao)
            }.forEach { hits.add(SearchHit.LicitacaoHit(it)) }
        }
        if (showPref && allow(SearchEntityFilter.Publicacoes)) {
            prefeitura.publicacoes.filter {
                matchesAnySearch(q, it.titulo, it.tipo, it.data)
            }.forEach { hits.add(SearchHit.PublicacaoHit(it)) }
        }
        if (showPref && entityFilter == SearchEntityFilter.Todos) {
            prefeitura.secretarias.filter {
                matchesAnySearch(q, it.nome, it.secretario)
            }.forEach { hits.add(SearchHit.SecretariaHit(it)) }
            prefeitura.obras.filter {
                matchesAnySearch(q, it.titulo, it.descricao, it.secretaria, it.situacao)
            }.forEach { hits.add(SearchHit.ObraHit(it)) }
            prefeitura.lrf.filter {
                matchesAnySearch(q, it.titulo, it.tipo, it.exercicio)
            }.forEach { hits.add(SearchHit.LrfHit(it)) }
            prefeitura.gestores.filter {
                matchesAnySearch(q, it.nome, it.cargo)
            }.forEach { hits.add(SearchHit.GestorHit(it)) }
            prefeitura.linksTransparencia.filter {
                matchesAnySearch(q, it.titulo, it.categoria, it.secao, it.url)
            }.forEach { hits.add(SearchHit.LinkPrefeituraHit(it)) }
            prefeitura.resumoFinanceiro?.linksPortal.orEmpty().filter { portalLink ->
                prefeitura.linksTransparencia.none { it.url.equals(portalLink.url, ignoreCase = true) }
            }.filter {
                matchesAnySearch(q, it.titulo, it.categoria, it.secao, it.url)
            }.forEach { hits.add(SearchHit.LinkPrefeituraHit(it)) }
        }
        if (showCam && allow(SearchEntityFilter.Vereadores)) {
            camara.parlamentares.filter {
                matchesAnySearch(q, it.nome, it.nomeCompleto, it.partido, it.cargo)
            }.forEach { hits.add(SearchHit.ParlamentarHit(it)) }
        }
        if (showCam && allow(SearchEntityFilter.Materias)) {
            camara.materias.filter {
                matchesAnySearch(q, it.titulo, it.tipo, it.autor)
            }.forEach { hits.add(SearchHit.MateriaHit(it)) }
        }
        if (showCam && allow(SearchEntityFilter.Sessoes)) {
            camara.sessoes.withIndex().filter { (_, s) ->
                matchesAnySearch(q, s.titulo, s.data, s.resumo)
            }.forEach { (idx, s) -> hits.add(SearchHit.SessaoHit(idx, s)) }
        }
        if (showCam && entityFilter == SearchEntityFilter.Todos) {
            camara.mesaDiretora.filter {
                matchesAnySearch(q, it.nome, it.cargo)
            }.forEach { hits.add(SearchHit.MesaHit(it)) }
            camara.linksTransparencia.filter {
                matchesAnySearch(q, it.titulo, it.categoria, it.url)
            }.forEach { hits.add(SearchHit.LinkCamaraHit(it)) }
        }
        if (showCam && allow(SearchEntityFilter.Documentos)) {
            camara.documentosTransparencia.filter {
                matchesAnySearch(q, it.titulo, it.categoria, it.data)
            }.forEach { hits.add(SearchHit.DocumentoCamaraHit(it)) }
        }

        return hits
    }

    fun grouped(hits: List<SearchHit>): List<Pair<String, List<SearchHit>>> =
        hits.groupBy { it.section }.entries.map { it.key to it.value }
}
