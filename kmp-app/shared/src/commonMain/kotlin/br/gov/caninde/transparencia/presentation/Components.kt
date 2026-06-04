package br.gov.caninde.transparencia.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.caninde.transparencia.domain.ConnectionState

// ─── Live Badge ───────────────────────────────────────────────────────────────

@Composable
fun LiveBadge(label: String = "Tempo real") {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Green100)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AppColors.Green500.copy(alpha = alpha))
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, color = AppColors.Green700, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Aviso de erro / dados incompletos ───────────────────────────────────────

@Composable
fun DataStatusBanner(
    error: String?,
    modifier: Modifier = Modifier,
) {
    if (error.isNullOrBlank()) return
    val isPartial = error.startsWith("Alguns dados")
    Box(
        modifier
            .fillMaxWidth()
            .background(if (isPartial) AppColors.Amber100 else AppColors.Red100)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            if (isPartial) error else "Erro nos dados: $error",
            fontSize = 11.sp,
            color = if (isPartial) AppColors.Amber700 else AppColors.Red700,
        )
    }
}

// ─── Connection Banner ────────────────────────────────────────────────────────

@Composable
fun ConnectionBanner(state: ConnectionState) {
    val (bg, text) = when (state) {
        is ConnectionState.Connecting   -> AppColors.Amber100 to "Conectando ao servidor…"
        is ConnectionState.Reconnecting -> AppColors.Amber100 to "Reconectando…"
        is ConnectionState.Error        -> AppColors.Red100   to "Sem conexão: ${state.message}"
        is ConnectionState.Connected    -> return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 11.sp, color = AppColors.TextSecondary)
    }
}

// ─── Metric Card ─────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    label: String,
    value: String,
    delta: String = "",
    deltaPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = AppColors.TextTertiary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            if (delta.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    delta,
                    fontSize = 10.sp,
                    color = if (deltaPositive) AppColors.Green500 else AppColors.Red700
                )
            }
        }
    }
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(text: String) {
    val (bg, fg) = when {
        text.contains("Pago", ignoreCase = true)
            || text.contains("Homologado", ignoreCase = true)
            || text.contains("Adjudicado", ignoreCase = true) ->
            AppColors.Green100 to AppColors.Green700
        text.contains("Parcial", ignoreCase = true)
            || text.contains("andamento", ignoreCase = true) ->
            AppColors.Amber100 to AppColors.Amber700
        else -> AppColors.Red100 to AppColors.Red700
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, action: String = "Ver todos", onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        if (onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(action, fontSize = 11.sp, color = AppColors.Blue500)
            }
        }
    }
}

// ─── Avatar Inicial ───────────────────────────────────────────────────────────

private val avatarColors = listOf(
    AppColors.Blue100 to AppColors.Navy800,
    AppColors.Green100 to AppColors.Green700,
    AppColors.Amber100 to AppColors.Amber700,
    AppColors.Purple100 to AppColors.Purple700,
    AppColors.Red100 to AppColors.Red700,
)

@Composable
fun InitialAvatar(name: String, size: Int = 36) {
    val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val (bg, fg) = avatarColors[name.length % avatarColors.size]
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontSize = (size * 0.35f).sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ─── Shimmer loading ─────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Divider.copy(alpha = alpha)))
}

// ─── List Row ─────────────────────────────────────────────────────────────────

@Composable
fun ListRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 11.sp, color = AppColors.TextTertiary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

// ─── Icon Container ──────────────────────────────────────────────────────────

@Composable
fun IconContainer(bg: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ─── Progress Row ────────────────────────────────────────────────────────────

@Composable
fun ProgressRow(label: String, pct: Float, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = AppColors.TextSecondary, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = AppColors.Divider
        )
        Text("${(pct * 100).toInt()}%", fontSize = 11.sp, color = AppColors.TextTertiary,
            modifier = Modifier.width(32.dp))
    }
}

// ─── Timestamp ────────────────────────────────────────────────────────────────

@Composable
fun LastUpdatedText(ts: String) {
    if (ts.isEmpty()) return
    val display = ts.take(19).replace("T", " ")
    Text(
        "Atualizado: $display",
        fontSize = 10.sp, color = AppColors.TextTertiary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
