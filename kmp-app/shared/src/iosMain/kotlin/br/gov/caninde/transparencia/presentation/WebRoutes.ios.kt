package br.gov.caninde.transparencia.presentation

actual fun installWebHistoryListener(onPathChange: (String) -> Unit): () -> Unit = { }

actual fun updateWebPath(path: String) {}

actual fun currentWebPath(): String = "/"
