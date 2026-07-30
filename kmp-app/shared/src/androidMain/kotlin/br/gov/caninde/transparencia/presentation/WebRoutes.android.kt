package br.gov.caninde.transparencia.presentation

import java.net.URLDecoder
import java.net.URLEncoder

actual fun encodeUriSegment(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())

actual fun decodeUriSegment(value: String): String =
    URLDecoder.decode(value, Charsets.UTF_8.name())

actual fun currentWebLocation(): String = "/"

actual fun updateWebPath(path: String, replace: Boolean) {}

actual fun webHistoryBack() {}

actual fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit = { }
