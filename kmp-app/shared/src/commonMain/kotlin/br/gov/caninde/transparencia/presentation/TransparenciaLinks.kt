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
import br.gov.caninde.transparencia.platform.openExternalUrl

@Composable
fun TransparenciaLinksIntro(
    orgao: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Acesse $orgao no portal oficial de transparência. Os dados financeiros detalhados abrem no navegador.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextSecondary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

fun LazyListScope.transparenciaLinksItems(
    links: List<LinkExterno>,
    sectionTitle: String = "Portal de transparência",
) {
    item { SectionHeader(title = sectionTitle, action = "") }
    if (links.isEmpty()) {
        item { EmptyState("Nenhum link de transparência disponível") }
        return
    }
    items(links) { link ->
        TransparenciaLinkRow(link)
        HorizontalDivider(
            color = AppColors.Divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun TransparenciaLinkRow(link: LinkExterno) {
    val icon = when (link.categoria) {
        "financeiro" -> Icons.Default.AccountBalance
        "compras" -> Icons.Default.ShoppingCart
        "pessoal" -> Icons.Default.People
        "obras" -> Icons.Default.Construction
        "emendas" -> Icons.Default.HowToVote
        "dadosabertos" -> Icons.Default.CloudDownload
        "fiscal" -> Icons.Default.Assessment
        else -> Icons.Default.OpenInNew
    }
    ListRow(
        icon = {
            IconContainer(AppColors.Blue100) {
                Icon(icon, contentDescription = null, tint = AppColors.Navy800, modifier = Modifier.size(18.dp))
            }
        },
        title = link.titulo,
        subtitle = link.categoria.replaceFirstChar { it.uppercase() },
        trailing = {
            Icon(Icons.Default.OpenInNew, contentDescription = null,
                tint = AppColors.TextTertiary, modifier = Modifier.size(16.dp))
        },
        onClick = {
            if (link.url.isNotBlank()) openExternalUrl(link.url)
        },
    )
}
