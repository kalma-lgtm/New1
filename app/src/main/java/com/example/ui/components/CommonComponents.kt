package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusSuspended
import java.util.Locale

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("metric_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "active", "online", "paid" -> Pair(StatusActive.copy(alpha = 0.14f), StatusActive)
        "suspended", "partially paid", "warning" -> Pair(StatusSuspended.copy(alpha = 0.16f), StatusSuspended)
        "disconnected", "offline", "overdue", "unpaid" -> Pair(StatusDisconnected.copy(alpha = 0.14f), StatusDisconnected)
        "archived" -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
        else -> Pair(Color(0xFFE2E8F0), Color(0xFF475569))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionButton != null) {
            actionButton()
        }
    }
}

@Composable
fun SimpleBarChart(
    labels: List<String>,
    values1: List<Float>,
    values2: List<Float>? = null,
    legend1: String = "Collections",
    legend2: String? = "Expenses",
    color1: Color = MaterialTheme.colorScheme.primary,
    color2: Color = Color(0xFFE11D48),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color1))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = legend1, style = MaterialTheme.typography.labelSmall)

                if (values2 != null && legend2 != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color2))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = legend2, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal1 = values1.maxOrNull() ?: 1f
            val maxVal2 = values2?.maxOrNull() ?: 0f
            val maxVal = maxOf(maxVal1, maxVal2, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val barCount = labels.size
                if (barCount == 0) return@Canvas
                val groupWidth = size.width / barCount
                val barWidth = if (values2 != null) groupWidth * 0.28f else groupWidth * 0.5f

                for (i in 0 until barCount) {
                    val xBase = i * groupWidth + (groupWidth - (if (values2 != null) barWidth * 2.2f else barWidth)) / 2

                    // Bar 1
                    val h1 = (values1.getOrElse(i) { 0f } / maxVal) * (size.height - 24.dp.toPx())
                    drawRoundRect(
                        color = color1,
                        topLeft = Offset(xBase, size.height - h1 - 24.dp.toPx()),
                        size = Size(barWidth, h1),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    // Bar 2 if present
                    if (values2 != null) {
                        val h2 = (values2.getOrElse(i) { 0f } / maxVal) * (size.height - 24.dp.toPx())
                        drawRoundRect(
                            color = color2,
                            topLeft = Offset(xBase + barWidth + 4.dp.toPx(), size.height - h2 - 24.dp.toPx()),
                            size = Size(barWidth, h2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleDonutChart(
    activeCount: Int,
    suspendedCount: Int,
    disconnectedCount: Int,
    modifier: Modifier = Modifier
) {
    val total = maxOf(1, activeCount + suspendedCount + disconnectedCount)
    val activeAngle = (activeCount.toFloat() / total) * 360f
    val suspendedAngle = (suspendedCount.toFloat() / total) * 360f
    val disconnectedAngle = (disconnectedCount.toFloat() / total) * 360f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Subscriber Connection Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        var start = -90f
                        drawArc(
                            color = StatusActive,
                            startAngle = start,
                            sweepAngle = activeAngle,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        start += activeAngle
                        drawArc(
                            color = StatusSuspended,
                            startAngle = start,
                            sweepAngle = suspendedAngle,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        start += suspendedAngle
                        drawArc(
                            color = StatusDisconnected,
                            startAngle = start,
                            sweepAngle = disconnectedAngle,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem("Active ($activeCount)", StatusActive)
                    LegendItem("Suspended ($suspendedCount)", StatusSuspended)
                    LegendItem("Disconnected ($disconnectedCount)", StatusDisconnected)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PrintableReceiptDialog(
    receiptNumber: String,
    date: String,
    subscriberName: String,
    accountNumber: String,
    amount: Double,
    paymentMethod: String,
    referenceNumber: String,
    receivedBy: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Collection Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "NETPULSE BROADBAND ISP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "High-Speed Fiber Internet & Network Services",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReceiptRow("Receipt No:", receiptNumber)
                        ReceiptRow("Date:", date)
                        ReceiptRow("Account No:", accountNumber)
                        ReceiptRow("Subscriber:", subscriberName)
                        ReceiptRow("Payment Method:", paymentMethod)
                        if (referenceNumber.isNotBlank()) {
                            ReceiptRow("Reference No:", referenceNumber)
                        }
                        ReceiptRow("Cashier/Agent:", receivedBy)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AMOUNT PAID",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", amount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusPaid
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Thank you for your payment! Keep this receipt for your records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
