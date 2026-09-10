package com.vineyard.omnicam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.AmberWarning
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.RoseAlert
import java.util.Locale

@Composable
fun StorageProgressBar(
    usedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val fraction = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    val progressColor = when {
        fraction > 0.85f -> RoseAlert
        fraction > 0.65f -> AmberWarning
        else -> CyanAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("storage_meter_box")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Google Drive Anti-Theft Cloud",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.1f", usedGb)} GB / ${totalGb.toInt()} GB Free",
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("storage_progress_indicator"),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Zero-Cost Serverless Storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(fraction * 100).toInt()}% Used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
