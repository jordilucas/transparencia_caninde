package br.gov.caninde.transparencia.domain

data class ContratoDisplayInfo(
    val titulo: String,
    val descricao: String,
    val valor: String,
    val meta: String,
    val situacao: String,
)

private val descricaoInicio = Regex(
    pattern = """(?:NOVO )?CONTRATAÇÃO|AQUISIÇÃO|LOCAÇÃO|REGISTRO|CREDENCIAMENTO|REFORMA|MANUNTENÇÃO|EXERCUÇÃO""",
    option = RegexOption.IGNORE_CASE,
)

private val cnpjSuffix = Regex("""\s+\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}$""")
private val dataValorColado = Regex("""^(\d{2}/\d{2}/\d{4})([\d.,]+)$""")

private fun splitSecretariaObjeto(text: String): Pair<String, String> {
    val t = text.trim()
    if (t.isEmpty()) return "" to ""
    val match = descricaoInicio.find(t) ?: return "" to t
    if (match.range.first <= 0) return "" to t
    return t.substring(0, match.range.first).trim() to t.substring(match.range.first).trim()
}

private fun splitEmpresaCnpj(text: String): Pair<String, String> {
    val t = text.trim()
    val m = cnpjSuffix.find(t) ?: return t to ""
    return t.substring(0, m.range.first).trim() to m.value.trim()
}

private fun splitDataValor(text: String): Pair<String, String> {
    val t = text.trim()
    if (t.contains("R$")) return "" to t
    val m = dataValorColado.matchEntire(t) ?: return t to ""
    val valorRaw = m.groupValues[2]
    val valor = formatValorBRL(valorRaw)
    return m.groupValues[1] to valor
}

private fun formatValorBRL(raw: String): String {
    if (raw.isBlank()) return ""
    val n = raw.replace(".", "").replace(",", ".").toDoubleOrNull() ?: return raw
    val cents = kotlin.math.round(n * 100).toLong()
    val reais = cents / 100
    val frac = (cents % 100).toInt()
    return "R$ ${reais.toString().reversed().chunked(3).joinToString(".").reversed()},${frac.toString().padStart(2, '0')}"
}

private fun normalizeContrato(c: Contrato): Contrato {
    if (c.secretaria.isNotBlank() && !cnpjSuffix.containsMatchIn(c.objeto) && c.valor.contains("R$")) {
        return c
    }

    var secretaria = c.secretaria
    var objeto = c.objeto
    var valor = c.valor
    var empresa = c.empresa
    var data = c.data
    var cnpj = c.cnpjCredor

    if (secretaria.isBlank() && objeto.isNotBlank()) {
        val (sec, desc) = splitSecretariaObjeto(objeto)
        if (sec.isNotBlank()) {
            secretaria = sec
            objeto = desc
        }
    }

    if (secretaria.isBlank() && valor.isNotBlank() && descricaoInicio.containsMatchIn(valor)) {
        val (sec, desc) = splitSecretariaObjeto(valor)
        if (sec.isNotBlank()) {
            secretaria = sec
            objeto = desc.ifBlank { objeto }
            valor = ""
        }
    }

    if (objeto.isNotBlank() && cnpjSuffix.containsMatchIn(objeto)) {
        val (emp, cnpjParsed) = splitEmpresaCnpj(objeto)
        if (empresa.isBlank() || empresa.any { it.isDigit() }) {
            empresa = emp
            cnpj = cnpj.ifBlank { cnpjParsed }
        }
        objeto = ""
    }

    if (valor.isNotBlank() && !valor.contains("R$") && descricaoInicio.containsMatchIn(valor)) {
        val (sec, desc) = splitSecretariaObjeto(valor)
        if (sec.isNotBlank()) {
            secretaria = secretaria.ifBlank { sec }
            objeto = objeto.ifBlank { desc }
            valor = ""
        }
    }

    if (empresa.isNotBlank() && dataValorColado.matches(empresa.trim())) {
        val (d, v) = splitDataValor(empresa)
        data = data.ifBlank { d }
        valor = valor.ifBlank { v }
        empresa = ""
    }

    if (valor.isNotBlank() && !valor.contains("R$")) {
        val (d, v) = splitDataValor(valor)
        if (v.isNotBlank()) {
            valor = v
            if (d.isNotBlank() && data.isBlank()) data = d
        }
    }

    return c.copy(
        secretaria = secretaria,
        objeto = objeto,
        valor = valor,
        empresa = empresa,
        data = data.replace("VIGENTE", "Vigente", ignoreCase = true),
        cnpjCredor = cnpj,
    )
}

fun Contrato.displayInfo(): ContratoDisplayInfo {
    val n = normalizeContrato(this)
    val numeroLimpo = n.numero.replace("CONTRATO ORIGINAL", "", ignoreCase = true).trim()

    val titulo = n.secretaria.ifBlank {
        n.empresa.ifBlank { if (numeroLimpo.isNotBlank()) "Contrato $numeroLimpo" else "Contrato" }
    }

    val descricao = n.objeto.ifBlank {
        if (titulo != n.empresa) n.empresa else ""
    }

    val meta = buildList {
        if (n.empresa.isNotBlank() && n.empresa != titulo && n.empresa != descricao) add(n.empresa)
        if (numeroLimpo.isNotBlank()) add(numeroLimpo)
        if (n.data.isNotBlank()) add(n.data)
    }.joinToString(" · ")

    val situacao = when {
        n.data.contains("Vigente", ignoreCase = true) -> "Vigente"
        else -> "Ativo"
    }

    return ContratoDisplayInfo(
        titulo = titulo,
        descricao = descricao,
        valor = n.valor,
        meta = meta,
        situacao = situacao,
    )
}

fun Contrato.normalized(): Contrato = normalizeContrato(this)
