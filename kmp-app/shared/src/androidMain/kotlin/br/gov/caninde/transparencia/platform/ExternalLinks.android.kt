package br.gov.caninde.transparencia.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

private var appContext: Context? = null

fun initExternalLinks(context: Context) {
    appContext = context.applicationContext
}

actual fun openExternalUrl(url: String) {
    val ctx = appContext ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}
