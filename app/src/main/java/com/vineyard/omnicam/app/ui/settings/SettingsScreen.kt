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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
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
import com.vineyard.omnicam.app.data.models.UserRole
import com.vineyard.omnicam.app.data.repository.AuthRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.ui.components.MemberProfileCard
import com.vineyard.omnicam.app.ui.landing.ByoFirebaseDialog
import java.io.File
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository? = null,
    onNavigateToLanding: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by settingsRepository.themeMode.collectAsState(initial = ThemeMode.DARK)
    val isBiometric by settingsRepository.isBiometricEnabled.collectAsState(initial = false)
    val isAutoCleanup by settingsRepository.isAutoCleanupEnabled.collectAsState(initial = true)
    val maxClipSec by settingsRepository.maxClipDuration.collectAsState(initial = 20)
    val customFirebaseJson by settingsRepository.customFirebaseJson.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Safe, type-inferred state collection
    val currentUser = authRepository?.currentUser?.collectAsState()?.value
    val isDriveConnected = authRepository?.isDriveConnected?.collectAsState()?.value ?: false

    val userRole = UserRole.fromString(currentUser?.role)
    val isAdmin = userRole == UserRole.ADMIN

    var crashReports by remember { mutableStateOf(OmniCrashHandler.getCrashReports()) }
    var selectedReportContent by remember { mutableStateOf<Pair<String, String>?>(null) }
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }
    var showCrashConfirmDialog by remember { mutableStateOf(false) }
    var showByoDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val omniLogDir = remember {
        OmniCrashHandler.getOmniLogDirectory() ?: File(Environment.getExternalStorageDirectory(), "omni log")
    }

    // Parse configured project ID from custom Firebase JSON
    val configuredProjectId = remember(customFirebaseJson) {
        if (!customFirebaseJson.isNullOrBlank()) {
            try {
                val root = JSONObject(customFirebaseJson!!)
                if (root.has("project_info")) {
                    root.getJSONObject("project_info").optString("project_id", "Configured")
                } else if (root.has("p")) {
                    root.optString("p", "Configured")
                } else {
                    "Custom Project Active"
                }
            } catch (_: Exception) {
                "Custom Project Active"
            }
        } else {
            null
        }
    }

    // BYO Firebase Dialog (Admin only)
    if (showByoDialog && isAdmin) {
        ByoFirebaseDialog(
            onDismiss = { showByoDialog = false },
            onSaveJson = { json ->
                coroutineScope.launch {
                    settingsRepository.saveCustomFirebaseJson(json)
                    actionStatusMessage = "Firebase credentials updated successfully."
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out & Switch Home?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = if (isAdmin) {
                        "This will sign you out as House Admin, erase the active custom Firebase JSON configuration, clear temporary access tokens, and reset the app to the onboarding landing page."
                    } else {
                        "This will sign out your House Member session, disconnect temporary access grants, and return to the onboarding landing page."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        coroutineScope.launch {
                            if (isAdmin) {
                                settingsRepository.clearCustomFirebaseJson()
                            }
                            settingsRepository.saveActiveGuestShareToken("")
                            authRepository?.signOut()
                            onNavigateToLanding?.invoke()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    modifier = Modifier.testTag("btn_confirm_sign_out")
                ) {
                    Text("Sign Out Now", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION: MEMBER / ADMIN IDENTITY PROFILE CARD
        currentUser?.let { profile ->
            MemberProfileCard(userProfile = profile)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION: GOOGLE DRIVE CLOUD STORAGE INTEGRATION
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDriveConnected) EmeraldLive.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDriveConnected) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (isDriveConnected) EmeraldLive else CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Drive (15 GB Free)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDriveConnected) "Active • Browser PKCE Authorized" else "Disconnected • No cloud backup",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDriveConnected) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Encrypted motion event clips are backed up directly to your personal Google Drive storage without recurring monthly subscriptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isDriveConnected) {
                    OutlinedButton(
                        onClick = { authRepository?.disconnectDrive() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlert),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseAlert),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = RoseAlert, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect Google Drive", color = RoseAlert, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = { authRepository?.initiateGoogleDriveOAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Google Drive via Browser", color = Color.Black, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: HOUSE FIREBASE CONFIGURATION (ROLE-SCOPED)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .testTag("card_admin_firebase_setup"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                .background(if (configuredProjectId != null) EmeraldLive.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (configuredProjectId != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (configuredProjectId != null) EmeraldLive else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isAdmin) "House Admin Firebase" else "House Network Hub",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (configuredProjectId != null) "Project: $configuredProjectId" else "Default Network",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (configuredProjectId != null) EmeraldLive else AmberWarning
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isAdmin) {
                        if (configuredProjectId != null) {
                            "Your custom google-services.json is active. Generated member QR codes will automatically bundle these credentials."
                        } else {
                            "Upload your private google-services.json so generated QR codes can link family members to your database."
                        }
                    } else {
                        "You are connected to the House Admin's private hub as a House Member. Database access rules and camera permissions are managed by the House Admin."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Administrative controls are ONLY visible to the House Admin
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showByoDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_manage_byo_firebase"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (configuredProjectId != null) "Update JSON" else "Upload JSON",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (configuredProjectId != null) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        settingsRepository.clearCustomFirebaseJson()
                                        actionStatusMessage = "Custom Firebase configuration cleared."
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlert),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoseAlert)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseAlert, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", color = RoseAlert, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: CRASH REPORTS & LOGGING
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

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: ACCOUNT & SESSION MANAGEMENT (Sign Out / Switch Home)
        Button(
            onClick = { showSignOutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_sign_out_home"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoseAlert)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out & Switch Home", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}