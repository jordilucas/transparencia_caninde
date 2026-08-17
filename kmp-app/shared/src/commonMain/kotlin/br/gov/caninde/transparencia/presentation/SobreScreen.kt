package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.*
import br.gov.caninde.transparencia.platform.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SobreScreen(
    prefeituraState: PrefeituraUiState,
    camaraState: CamaraUiState,
) {
    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Box(Modifier.fillMaxWidth().background(AppColors.Navy800)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "Sobre o portal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Blue100,
                )
                Text(
                    "Transparência Canindé · ${DataSourcesInfo.MUNICIPIO}",
                    fontSize = 11.sp,
                    color = AppColors.Blue300,
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SobreCard {
                    SobreParagraph(
                        "Este portal agrega e exibe dados públicos da Prefeitura e da Câmara Municipal de Canindé (CE). " +
                            "Nenhuma informação é inventada: tudo o que você vê vem dos sites oficiais listados abaixo, " +
                            "atualizados periodicamente pelo nosso servidor.",
                    )
                    SobreParagraph(
                        "Os dados são reais, publicados pelos próprios órgãos públicos para consulta da população. " +
                            "Este projeto facilita o acesso, mas a fonte de verdade continua sendo sempre o portal oficial de origem.",
                    )
                }
            }

            item {
                SobreSectionTitle("Novidades")
                SobreCard {
                    DataSourcesInfo.changelog.forEach { entry ->
                        Text(
                            "${entry.titulo} · ${entry.data}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Navy800,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        entry.itens.forEach { item ->
                            Text(
                                "• $item",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item {
                SobreSectionTitle("Como capturamos os dados")
                SobreCard {
                    SobreParagraph(
                        "Um servidor consulta as URLs abaixo em intervalos regulares, normaliza o conteúdo em JSON " +
                            "e envia ao aplicativo via WebSocket. Detalhes (biografias, PDFs, páginas longas) são buscados " +
                            "somente quando você abre o item.",
                    )
                    if (prefeituraState.lastUpdated.isNotBlank() || camaraState.lastUpdated.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        if (prefeituraState.lastUpdated.isNotBlank()) {
                            SobreMeta("Prefeitura atualizada", prefeituraState.lastUpdated)
                        }
                        if (camaraState.lastUpdated.isNotBlank()) {
                            SobreMeta("Câmara atualizada", camaraState.lastUpdated)
                        }
                    }
                    val fontesAtivas = (prefeituraState.resumo.fontesUtilizadas + camaraState.resumo.fontesUtilizadas)
                        .distinct()
                        .filter { it.isNotBlank() }
                    if (fontesAtivas.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        SobreMeta(
                            "Fontes ativas na última sincronização",
                            fontesAtivas.joinToString(", "),
                        )
                    }
                }
            }

            item {
                SobreSectionTitle("Prefeitura — exportação JSON")
                SobreCard {
                    SobreParagraph("Endpoint principal de dados abertos (parâmetro a=ano do exercício):")
                    DataSourcesInfo.prefeituraJson.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Prefeitura — páginas HTML")
                SobreCard {
                    DataSourcesInfo.prefeituraHtml.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Prefeitura — detalhes sob demanda")
                SobreCard {
                    SobreParagraph("Consultadas ao abrir um item específico no app:")
                    DataSourcesInfo.prefeituraDetalhe.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Câmara — API WordPress (REST)")
                SobreCard {
                    DataSourcesInfo.camaraWp.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Câmara — páginas HTML")
                SobreCard {
                    DataSourcesInfo.camaraHtml.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Câmara — detalhes sob demanda")
                SobreCard {
                    DataSourcesInfo.camaraDetalhe.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Governo Transparente (links oficiais)")
                SobreCard {
                    SobreParagraph(
                        "Receitas, despesas e convênios detalhados ficam no painel do Governo Transparente. " +
                            "O app direciona para esses endereços; não replica consultas interativas desse sistema.",
                    )
                    DataSourcesInfo.governoTransparente.forEach { fonte ->
                        SobreFonteRow(fonte)
                    }
                }
            }

            item {
                SobreSectionTitle("Base legal")
                SobreCard {
                    SobreParagraph(
                        "A captura e divulgação destes dados encontra amparo na legislação brasileira de transparência " +
                            "e acesso à informação pública. Trata-se de informações já disponibilizadas pelos órgãos " +
                            "municipais para consulta de qualquer cidadão.",
                    )
                    DataSourcesInfo.baseLegal.forEach { lei ->
                        SobreLeiRow(lei)
                    }
                }
            }

            item {
                SobreSectionTitle("Privacidade")
                SobreCard {
                    SobreParagraph(
                        "Este portal exibe dados institucionais e documentos públicos. Não vendemos nem comercializamos " +
                            "informações pessoais. Dados de contato de secretarias e gestores são os mesmos já publicados " +
                            "nos sites oficiais. Contatos pessoais de vereadores não são exibidos neste app — consulte o " +
                            "site da Câmara, nos termos da Lei nº 13.709/2018 (LGPD) aplicável à administração pública.",
                    )
                    Spacer(Modifier.height(8.dp))
                    SobreParagraph(
                        "Não utilizamos cookies de rastreamento nem ferramentas de analytics (Google Analytics, etc.). " +
                            "O portal não registra telas visitadas, buscas ou compartilhamentos. Apenas o cache local do " +
                            "navegador guarda dados públicos para uso offline.",
                    )
                }
            }

            item {
                SobreSectionTitle("Aviso")
                SobreCard {
                    SobreParagraph(
                        "Embora busquemos manter os dados atualizados, pode haver atraso em relação ao portal de origem. " +
                            "Em caso de divergência, prevalece sempre a informação no site oficial. " +
                            "Este é um projeto independente de facilitação do acesso à transparência municipal.",
                    )
                    Spacer(Modifier.height(4.dp))
                    SobreMeta("Site", DataSourcesInfo.SITE_URL)
                    SobreMeta("Município (IBGE)", "${DataSourcesInfo.MUNICIPIO} · ${DataSourcesInfo.IBGE}")
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun SobreSectionTitle(title: String) {
    Text(
        title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.Navy800,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun SobreCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SobreParagraph(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = AppColors.TextSecondary,
    )
}

@Composable
private fun SobreMeta(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, color = AppColors.TextTertiary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = AppColors.TextPrimary)
    }
}

@Composable
private fun SobreFonteRow(fonte: FonteCaptura) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { openExternalUrl(fonte.urlParaAbrir()) }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                fonte.titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.Navy800,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = AppColors.Blue500,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            fonte.url,
            fontSize = 11.sp,
            color = AppColors.Blue500,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (fonte.descricao.isNotBlank()) {
            Text(fonte.descricao, fontSize = 11.sp, color = AppColors.TextTertiary)
        }
    }
    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}

@Composable
private fun SobreLeiRow(lei: ReferenciaLegal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (lei.url.isNotBlank()) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { openExternalUrl(lei.url) }
                        .padding(vertical = 4.dp)
                } else {
                    Modifier.padding(vertical = 4.dp)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(lei.titulo, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.Navy800)
        Text(lei.descricao, fontSize = 12.sp, lineHeight = 18.sp, color = AppColors.TextSecondary)
    }
    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}
