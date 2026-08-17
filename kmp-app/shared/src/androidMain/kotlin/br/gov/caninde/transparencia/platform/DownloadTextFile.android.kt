package br.gov.caninde.transparencia.platform

actual fun downloadTextFile(fileName: String, content: String, mimeType: String) {
    shareContent(fileName.removeSuffix(".csv"), content)
}
