package br.gov.caninde.transparencia.presentation

actual fun encodeUriSegment(value: String): String = value

actual fun decodeUriSegment(value: String): String = value

actual fun currentWebLocation(): String = "/"

actual fun updateWebPath(path: String, replace: Boolean) {}

actual fun webHistoryBack() {}

actual fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit = { }
