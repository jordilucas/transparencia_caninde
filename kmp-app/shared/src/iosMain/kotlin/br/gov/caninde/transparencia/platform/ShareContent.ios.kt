package br.gov.caninde.transparencia.platform

import br.gov.caninde.transparencia.domain.ShareTexts

actual fun shareContent(title: String, text: String) {
    openExternalUrl(ShareTexts.SITE_URL)
}

actual fun hideAppLoadingScreen() {}
