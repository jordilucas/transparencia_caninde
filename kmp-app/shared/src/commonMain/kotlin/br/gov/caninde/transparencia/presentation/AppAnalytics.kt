package br.gov.caninde.transparencia.presentation

import br.gov.caninde.transparencia.platform.logAnalyticsEvent
import br.gov.caninde.transparencia.platform.logAnalyticsScreen

fun AppRoute.analyticsScreenName(): String = when (this) {
    is AppRoute.Main -> when (screen) {
        Screen.Prefeitura -> "prefeitura"
        Screen.Camara -> "camara"
        Screen.Graficos -> "graficos"
        Screen.Busca -> "busca"
        Screen.Agua -> "agua"
        Screen.Sobre -> "sobre"
    }
    is AppRoute.Vereador -> "detalhe_vereador"
    is AppRoute.Materia -> "detalhe_materia"
    is AppRoute.Secretaria -> "detalhe_secretaria"
    is AppRoute.Contrato -> "detalhe_contrato"
    is AppRoute.Licitacao -> "detalhe_licitacao"
    is AppRoute.Sessao -> "detalhe_sessao"
    is AppRoute.Publicacao -> "detalhe_publicacao"
    is AppRoute.PaginaPortal -> "detalhe_pagina_portal"
    is AppRoute.DocumentoCamara -> "detalhe_documento_camara"
    AppRoute.Gestores -> "detalhe_gestores"
    is AppRoute.Institucional -> if (camara) "institucional_camara" else "institucional_prefeitura"
}

object AppAnalytics {
    fun logScreen(route: AppRoute) {
        logAnalyticsScreen(route.analyticsScreenName())
    }

    fun logSearch(queryLength: Int, resultsCount: Int, scope: String) {
        logAnalyticsEvent(
            "search",
            mapOf(
                "query_length" to queryLength.toString(),
                "results_count" to resultsCount.toString(),
                "scope" to scope,
            ),
        )
    }

    fun logShare(contentType: String) {
        logAnalyticsEvent("share", mapOf("content_type" to contentType))
    }

    fun logConnectionError() {
        logAnalyticsEvent("connection_error")
    }

    fun logRetryConnection() {
        logAnalyticsEvent("retry_connection")
    }
}
