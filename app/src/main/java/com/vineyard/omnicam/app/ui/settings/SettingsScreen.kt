package com.vineyard.omnicam.app.ui.settings

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.crash.OmniCrashHandler
import com.vineyard.omnicam.app.core.theme.AmberWarning
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.RoseAlert
import com.vineyard.omnicam.app.core.theme.ThemeMode
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by settingsRepository.themeMode.collectAsState(initial = ThemeMode.DARK)
    val isBiometric by settingsRepository.isBiometricEnabled.collectAsState(initial = false)
    val isAutoCleanup by settingsRepository.isAutoCleanupEnabled.collectAsState(initial = true)
    val maxClipSec by settingsRepository.maxClipDuration.collectAsState(initial = 20)
    val coroutineScope = rememberCoroutineScope()

    var crashReports by remember { mutableStateOf(OmniCrashHandler.getCrashReports()) }
    var selectedReportContent by remember { mutableStateOf<Pair<String, String>?>(null) }
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }
    var showCrashConfirmDialog by remember { mutableStateOf(false) }

    val omniLogDir = remember {
        OmniCrashHandler.getOmniLogDirectory() ?: File(Environment.getExternalStorageDirectory(), "omni log")
    }

    if (showCrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCrashConfirmDialog = false },
            title = { Text("Trigger Crash Test?", color = RoseAlert) },
            text = {
                Text(
                    text = "This will deliberately throw an UncaughtException. OmniCrashHandler will catch it, write the full stack trace & system info into '${omniLogDir.absolutePath}', and then terminate cleanly.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCrashConfirmDialog = false
                        // Deliberate test crash to demonstrate uncaught exception handler
                        throw RuntimeException("OmniCam Vision deliberate test crash: Testing crash logging to /sdcard/omni log/")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    modifier = Modifier.testTag("btn_confirm_crash_test")
                ) {
                    Text("Trigger Crash Now", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCrashConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedReportContent?.let { (filename, text) ->
        AlertDialog(
            onDismissRequest = { selectedReportContent = null },
            title = { Text(text = filename, style = MaterialTheme.typography.titleSmall, color = CyanAccent) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedReportContent = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Text(
            text = "Settings & Diagnostics",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "System Diagnostics, Crash Logging & App Configuration",
            style = MaterialTheme.typography.bodySmall,
            color = CyanAccent
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: CRASH REPORTS & LOGGING (Prominently featured per user prompt)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, RoseAlert.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .testTag("crash_reports_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoseAlert.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = RoseAlert,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Crash Reports & Logging",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Target: /sdcard/omni log/",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            crashReports = OmniCrashHandler.getCrashReports()
                            actionStatusMessage = "Refreshed: ${crashReports.size} report(s) found in omni log"
                        },
                        modifier = Modifier.testTag("btn_refresh_crash_logs")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Reports", tint = CyanAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Storage location callout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Storage Path Policy:",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Crash dumps are saved exclusively on the SD card / external root: '${omniLogDir.absolutePath}'. Never saved in private app files or /Android system directories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic Test Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = OmniCrashHandler.writeManualTestReport(
                                context,
                                "Manual test report generated by user in Settings"
                            )
                            crashReports = OmniCrashHandler.getCrashReports()
                            actionStatusMessage = "Report written: ${file?.name ?: "failed"}"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_write_test_report"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("Write Diagnostic", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showCrashConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_crash_trigger"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlert),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseAlert)
                    ) {
                        Text("Test App Crash", color = RoseAlert, style = MaterialTheme.typography.labelMedium)
                    }
                }

                actionStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLive
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Saved Logs in /sdcard/omni log/ (${crashReports.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (crashReports.isEmpty()) {
                    Text(
                        text = "No crash reports yet. If the app ever crashes, logs will automatically appear here and in /sdcard/omni log/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    crashReports.take(5).forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${file.length()} bytes • ${file.parent}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    val content = try { file.readText() } catch (e: Exception) { "Error reading file: ${e.message}" }
                                    selectedReportContent = file.name to content
                                },
                                modifier = Modifier.size(width = 64.dp, height = 32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                            ) {
                                Text("View", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION: THEME PREFERENCE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ThemeMode.DARK to "Slate Dark",
                        ThemeMode.AMOLED to "OLED Black",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.SYSTEM to "System"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = {
                                coroutineScope.launch {
                                    settingsRepository.setThemeMode(mode)
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: SECURITY & BIOMETRIC
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Biometric / PIN Lock", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Require fingerprint or face on launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isBiometric,
                        onCheckedChange = { chk ->
                            coroutineScope.launch {
                                settingsRepository.setBiometricEnabled(chk)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                        modifier = Modifier.testTag("switch_biometric")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = EmeraldLive)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Auto-Prune Old Clips", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Retain 15GB Google Drive quota free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isAutoCleanup,
                        onCheckedChange = { chk ->
                            coroutineScope.launch {
                                settingsRepository.setAutoCleanupEnabled(chk)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldLive),
                        modifier = Modifier.testTag("switch_autocleanup")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: ABOUT & BUILD INFO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("About OmniCam Vision", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version: 1.0.0 (Debug APK Build)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Architecture: Multi-Brand ONVIF/RTSP/Tuya P2P Serverless", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Cloud Storage: Google Drive 15GB Zero-Cost Tier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Crash Handler: OmniCrashHandler active (/sdcard/omni log)", style = MaterialTheme.typography.bodySmall, color = CyanAccent)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
