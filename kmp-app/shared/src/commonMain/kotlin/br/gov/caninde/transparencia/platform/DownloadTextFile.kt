package br.gov.caninde.transparencia.platform

expect fun downloadTextFile(fileName: String, content: String, mimeType: String = "text/csv;charset=utf-8")
