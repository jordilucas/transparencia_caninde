package br.gov.caninde.transparencia.platform

/** Compartilha texto (Web Share API, intent Android ou cópia). */
expect fun shareContent(title: String, text: String)

/** Remove splash de carregamento HTML (somente web). */
expect fun hideAppLoadingScreen()
