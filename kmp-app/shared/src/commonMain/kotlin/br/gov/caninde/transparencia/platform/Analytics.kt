package br.gov.caninde.transparencia.platform

expect fun logAnalyticsScreen(screenName: String)

expect fun logAnalyticsEvent(name: String, params: Map<String, String> = emptyMap())
