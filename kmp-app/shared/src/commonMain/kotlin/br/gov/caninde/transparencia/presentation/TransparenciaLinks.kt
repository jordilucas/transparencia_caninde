package br.gov.caninde.transparencia.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.gov.caninde.transparencia.domain.LinkExterno

@Composable
fun TransparenciaLinksIntro(
    orgao: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Consulte $orgao nos portais oficiais de transparência. Toque em um item para ver o resumo e documentos disponíveis.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextSecondary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

fun LazyListScope.transparenciaLinksItems(
    links: List<LinkExterno>,
    sectionTitle: String = "Portal de transparência",
    onClick: (LinkExterno) -> Unit = {},
) {
    item { SectionHeader(title = sectionTitle, action = "") }
    if (links.isEmpty()) {
        item { EmptyState("Nenhum link de transparência disponível") }
        return
    }
    items(links) { link ->
        TransparenciaLinkRow(link, onClick)
        HorizontalDivider(
            color = AppColors.Divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

fun LazyListScope.hubLinksGroupedItems(
    links: List<LinkExterno>,
    onClick: (LinkExterno) -> Unit = {},
) {
    if (links.isEmpty()) {
        item { EmptyState("Nenhum link de transparência disponível") }
        return
    }
    val grouped = links.groupBy { link ->
        link.secao.ifBlank { link.categoria.replaceFirstChar { it.uppercase() } }
    }
    grouped.forEach { (secao, secaoLinks) ->
        item { SectionHeader(title = secao, action = "") }
        items(secaoLinks, key = { it.url.ifBlank { it.titulo } }) { link ->
            TransparenciaLinkRow(link, onClick)
            HorizontalDivider(
                color = AppColors.Divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun TransparenciaLinkRow(link: LinkExterno, onClick: (LinkExterno) -> Unit = {}) {
    val icon = when (link.categoria) {
        "financeiro" -> Icons.Default.AccountBalance
        "receita" -> Icons.Default.TrendingUp
        "despesa" -> Icons.Default.TrendingDown
        "compras" -> Icons.Default.ShoppingCart
        "obras" -> Icons.Default.Construction
        "emendas" -> Icons.Default.HowToVote
        "dadosabertos" -> Icons.Default.CloudDownload
        "fiscal" -> Icons.Default.Assessment
        "cidadania" -> Icons.Default.SupportAgent
        "pessoal" -> Icons.Default.Groups
        "saude" -> Icons.Default.LocalHospital
        "institucional" -> Icons.Default.Apartment
        "legislativo" -> Icons.Default.Gavel
        "atricon" -> Icons.Default.Verified
        else -> Icons.Default.OpenInNew
    }
    val subtitle = link.secao.takeIf { it.isNotBlank() && link.categoria.isNotBlank() }
        ?.let { link.categoria.replaceFirstChar { it.uppercase() } }
        ?: link.categoria.replaceFirstChar { it.uppercase() }
    ListRow(
        icon = {
            IconContainer(AppColors.Blue100) {
                Icon(icon, contentDescription = null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
            }
        },
        title = link.titulo,
        subtitle = subtitle,
        trailing = {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
        },
        onClick = {
            if (link.url.isNotBlank()) onClick(link)
        },
    )
}
