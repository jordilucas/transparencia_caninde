package br.gov.caninde.transparencia.platform

import android.content.Context
import android.content.Intent
import br.gov.caninde.transparencia.domain.ShareTexts

private var shareContext: Context? = null

fun initShareContent(context: Context) {
    shareContext = context.applicationContext
}

actual fun shareContent(title: String, text: String) {
    val ctx = shareContext ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(chooser) }
}

actual fun hideAppLoadingScreen() {}
