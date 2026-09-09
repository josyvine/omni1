package com.vineyard.omnicam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.CyanAccent

@Composable
fun PtzJoystick(
    onCommand: (direction: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastCommand by remember { mutableStateOf("Ready") }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.width(220.dp)
        ) {
            Text(
                text = "PTZ Pan / Tilt / Zoom",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = lastCommand,
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8-Direction D-Pad
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, CyanAccent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Row 1: NW, UP, NE
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PtzButton(icon = Icons.Default.NorthWest, tag = "ptz_nw") {
                    lastCommand = "↖ Pan Left Up"
                    onCommand("NW")
                }
                PtzButton(icon = Icons.Default.ArrowUpward, tag = "ptz_up") {
                    lastCommand = "↑ Tilt Up"
                    onCommand("UP")
                }
                PtzButton(icon = Icons.Default.NorthEast, tag = "ptz_ne") {
                    lastCommand = "↗ Pan Right Up"
                    onCommand("NE")
                }
            }

            // Row 2: LEFT, CENTER, RIGHT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PtzButton(icon = Icons.Default.ArrowBack, tag = "ptz_left") {
                    lastCommand = "← Pan Left"
                    onCommand("LEFT")
                }
                // Center Reset / Preset
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyanAccent)
                        .clickable {
                            lastCommand = "⦿ Centered"
                            onCommand("CENTER")
                        }
                        .testTag("ptz_center"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Center Preset",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
                PtzButton(icon = Icons.Default.ArrowForward, tag = "ptz_right") {
                    lastCommand = "→ Pan Right"
                    onCommand("RIGHT")
                }
            }

            // Row 3: SW, DOWN, SE
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                PtzButton(icon = Icons.Default.SouthWest, tag = "ptz_sw") {
                    lastCommand = "↙ Pan Left Down"
                    onCommand("SW")
                }
                PtzButton(icon = Icons.Default.ArrowDownward, tag = "ptz_down") {
                    lastCommand = "↓ Tilt Down"
                    onCommand("DOWN")
                }
                PtzButton(icon = Icons.Default.SouthEast, tag = "ptz_se") {
                    lastCommand = "↘ Pan Right Down"
                    onCommand("SE")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Zoom Controls Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    lastCommand = "🔍 Zoom Out"
                    onCommand("ZOOM_OUT")
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("ptz_zoom_out")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "OPTICAL ZOOM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = {
                    lastCommand = "🔎 Zoom In"
                    onCommand("ZOOM_IN")
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("ptz_zoom_in")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PtzButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .testTag(tag),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tag,
            modifier = Modifier.size(22.dp)
        )
    }
}
