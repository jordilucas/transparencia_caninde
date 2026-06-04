package br.gov.caninde.transparencia.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
