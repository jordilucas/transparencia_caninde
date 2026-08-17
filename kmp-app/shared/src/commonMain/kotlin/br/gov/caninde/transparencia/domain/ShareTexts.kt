package br.gov.caninde.transparencia.domain

import br.gov.caninde.transparencia.presentation.AppRoute
import br.gov.caninde.transparencia.presentation.Screen
import br.gov.caninde.transparencia.presentation.contratoDetailId
import br.gov.caninde.transparencia.presentation.licitacaoDetailId
import br.gov.caninde.transparencia.presentation.materiaSlug
import br.gov.caninde.transparencia.presentation.parlamentarSlug
import br.gov.caninde.transparencia.presentation.sessaoRouteId
import br.gov.caninde.transparencia.presentation.shareUrl

object ShareTexts {
    const val SITE_URL = "https://transparenciacaninde.com.br"

    fun contrato(c: Contrato): String {
        val info = c.normalized().displayInfo()
        val link = AppRoute.Contrato(contratoDetailId(c)).shareUrl()
        return buildString {
            append("Contrato: ${info.titulo}")
            if (c.valor.isNotBlank()) append(" — ${c.valor}")
            append("\n\n$link")
        }
    }

    fun licitacao(l: Licitacao): String {
        val info = l.displayInfo()
        val link = AppRoute.Licitacao(licitacaoDetailId(l)).shareUrl()
        return buildString {
            append("Licitação: ${info.titulo}")
            if (l.situacao.isNotBlank()) append(" (${l.situacao})")
            append("\n\n$link")
        }
    }

    fun publicacao(p: Publicacao): String {
        val id = p.id.ifBlank { p.url.substringAfter("id=").substringBefore('&') }
        val link = if (id.isNotBlank()) AppRoute.Publicacao(id).shareUrl() else SITE_URL
        return buildString {
            append("Publicação: ${p.titulo.ifBlank { p.tipo }}")
            if (p.data.isNotBlank()) append(" — ${p.data}")
            append("\n\n$link")
        }
    }

    fun vereador(p: Parlamentar): String {
        val slug = parlamentarSlug(p)
        val link = if (slug.isNotBlank()) AppRoute.Vereador(slug).shareUrl() else SITE_URL
        return buildString {
            append("Vereador(a): ${p.nomeCompleto.ifBlank { p.nome }}")
            if (p.partido.isNotBlank()) append(" — ${p.partido}")
            append("\n\n$link")
        }
    }

    fun materia(m: Materia): String {
        val slug = materiaSlug(m)
        val link = if (slug.isNotBlank()) AppRoute.Materia(slug).shareUrl() else SITE_URL
        return buildString {
            append("Matéria: ${m.titulo}")
            if (m.tipo.isNotBlank()) append(" (${m.tipo})")
            append("\n\n$link")
        }
    }

    fun secretaria(s: Secretaria): String {
        val link = if (s.id.isNotBlank()) AppRoute.Secretaria(s.id).shareUrl() else SITE_URL
        return buildString {
            append("Secretaria: ${s.nome}")
            if (s.secretario.isNotBlank()) append(" — ${s.secretario}")
            append("\n\n$link")
        }
    }

    fun sessao(s: Sessao, routeId: String = s.slug): String {
        val link = if (routeId.isNotBlank()) AppRoute.Sessao(routeId).shareUrl() else SITE_URL
        return buildString {
            append("Sessão: ${s.titulo.ifBlank { s.data }}")
            append("\n\n$link")
        }
    }

    fun paginaPortal(titulo: String, pageId: String = ""): String {
        val link = if (pageId.isNotBlank()) AppRoute.PaginaPortal(pageId).shareUrl() else SITE_URL
        return "$titulo\n\n$link"
    }

    fun reclamacaoAgua(): String {
        val link = AppRoute.Main(Screen.Agua).shareUrl()
        return buildString {
            append("💧 Falta de água em Canindé?\n\n")
            append("Registre sua reclamação de forma anônima:\n")
            append("• Endereço / bairro\n")
            append("• Setor 1 ou 2 (rodízio SAAE)\n")
            append("• Dias sem água\n")
            append("• Foto ou vídeo como comprovante\n\n")
            append("Acompanhe todas no painel público.\n\n")
            append(link)
        }
    }
}
