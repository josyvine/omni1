package com.vineyard.omnicam.app.ui.landing

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.vineyard.omnicam.app.core.constants.CentralConfig
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.ElectricBlue
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.IndigoAccent
import com.vineyard.omnicam.app.ui.share.ScanQrDialog

@Composable
fun LandingScreen(
    viewModel: LandingViewModel,
    onNavigateToDashboard: () -> Unit,
    onGuestQrScanned: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val showByoDialog by viewModel.showByoDialog.collectAsState()
    val configuredProjectId by viewModel.configuredProjectId.collectAsState()
    val isDriveConnected by viewModel.isDriveConnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var showScanQrDialog by remember { mutableStateOf(false) }

    // Explicit role tracking to prevent Guest Members from defaulting to Admin
    var pendingRole by remember { mutableStateOf("admin") }
    var isGuestQrImported by remember { mutableStateOf(false) }

    // Google Sign-In Options configured with Central Web Client ID
    val gso: GoogleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(CentralConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    val googleSignInClient: GoogleSignInClient = remember(context) { 
        GoogleSignIn.getClient(context, gso) 
    }

    // Native Google Account Picker Launcher with explicit role delegation
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData: Intent? = result.data
            if (intentData != null) {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intentData)
                try {
                    val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                    val idToken: String? = account?.idToken
                    if (!idToken.isNullOrBlank()) {
                        viewModel.signInWithGoogle(idToken, pendingRole) { success: Boolean ->
                            if (success) {
                                onNavigateToDashboard()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // BYO-Firebase Configuration Dialog (House Admin path)
    if (showByoDialog) {
        ByoFirebaseDialog(
            onDismiss = { viewModel.dismissByoDialog() },
            onSaveJson = { json ->
                pendingRole = "admin"
                isGuestQrImported = false
                viewModel.saveByoFirebase(json)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )
    }

    // CameraX Guest QR Scanner Dialog (House Member path)
    if (showScanQrDialog) {
        ScanQrDialog(
            onDismissRequest = { showScanQrDialog = false },
            onQrCodeScanned = { rawPayload ->
                showScanQrDialog = false
                pendingRole = "guest"
                isGuestQrImported = true
                if (onGuestQrScanned != null) {
                    onGuestQrScanned(rawPayload)
                }
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Icon with Cyan Halo
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyanAccent.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(2.dp, CyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "OmniCam Vision Logo",
                tint = CyanAccent,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "OmniCam Vision",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Universal Multi-Brand Security Dashboard",
            style = MaterialTheme.typography.titleMedium,
            color = CyanAccent
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Zero-Cost Serverless Architecture. Unify TP-Link Tapo, Reolink, Dahua, Hikvision & Tuya Smart Bulbs without monthly cloud subscriptions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Architecture Highlights Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PillarBadge(icon = Icons.Default.FlashOn, label = "Direct P2P", color = CyanAccent)
            PillarBadge(icon = Icons.Default.CloudQueue, label = "15 GB Drive", color = EmeraldLive)
            PillarBadge(icon = Icons.Default.Security, label = "Serverless", color = IndigoAccent)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real-Time Feedback Banner
        if (statusMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldLive.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLive, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = statusMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { viewModel.clearStatusMessage() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Action Card 1: Quick Start (Local & Direct)
        ActionCard(
            title = "Quick Start (Local & P2P)",
            subtitle = "Zero configuration required. Scan local WiFi subnet and connect ONVIF/RTSP cameras instantly.",
            icon = Icons.Default.FlashOn,
            accentColor = CyanAccent,
            testTag = "action_quick_start",
            onClick = onNavigateToDashboard
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Card 2: Sign In with Google (Identity Verification)
        val isUserLoggedIn = currentUser != null
        val googleSubtitle = if (isUserLoggedIn) {
            val userRoleLabel = if (currentUser?.role == "guest") "House Member" else "House Admin"
            "Signed in as: ${currentUser?.email} ($userRoleLabel). Identity verified."
        } else {
            "Sign in with Google account to verify identity on Central Developer Firebase."
        }
        ActionCard(
            title = if (isUserLoggedIn) "Google Account Connected" else "Sign in with Google",
            subtitle = googleSubtitle,
            icon = if (isUserLoggedIn) Icons.Default.CloudDone else Icons.Default.CloudQueue,
            accentColor = EmeraldLive,
            statusBadge = if (isUserLoggedIn) "Signed In" else null,
            testTag = "action_google_signin",
            onClick = {
                pendingRole = if (isGuestQrImported) "guest" else "admin"
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Card 3: Join as Guest (Scan QR Code)
        val isMemberJoined = isGuestQrImported || currentUser?.role == "guest"
        val guestSubtitle = if (isMemberJoined && configuredProjectId != null) {
            "Joined Host Project: $configuredProjectId as House Member. QR configuration verified."
        } else {
            "Scan an Admin's encrypted QR code to import configuration, then verify with Google."
        }
        ActionCard(
            title = if (isMemberJoined) "House Network Joined (Member Mode)" else "Join as Guest (Scan QR Code)",
            subtitle = guestSubtitle,
            icon = if (isMemberJoined) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
            accentColor = if (isMemberJoined) EmeraldLive else IndigoAccent,
            statusBadge = if (isMemberJoined) "Member Ready" else null,
            testTag = "action_scan_guest_qr",
            onClick = { showScanQrDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Card 4: Bring-Your-Own Firebase (House Admin Cloud)
        val isAdminConfigured = !isMemberJoined && configuredProjectId != null
        val byoTitle = when {
            isAdminConfigured -> "House Admin Firebase Configured"
            isMemberJoined -> "House Admin Cloud (Linked via QR)"
            else -> "Bring-Your-Own Firebase"
        }
        val byoSubtitle = when {
            isAdminConfigured -> "Project: $configuredProjectId active. Bundled in member QR codes."
            isMemberJoined -> "Linked to Host Project: $configuredProjectId. Storage managed by Admin."
            else -> "Supply your own google-services.json for dedicated private cloud storage."
        }
        ActionCard(
            title = byoTitle,
            subtitle = byoSubtitle,
            icon = if (configuredProjectId != null) Icons.Default.CheckCircle else Icons.Default.Storage,
            accentColor = if (isAdminConfigured) EmeraldLive else ElectricBlue,
            statusBadge = if (configuredProjectId != null) "Active" else null,
            testTag = "action_byo_firebase",
            onClick = {
                if (!isMemberJoined) {
                    viewModel.openByoDialog()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gate to Enter Dashboard
        Button(
            onClick = {
                if (isUserLoggedIn) {
                    onNavigateToDashboard()
                } else {
                    pendingRole = if (isGuestQrImported) "guest" else "admin"
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("launch_dashboard_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
        ) {
            Text(
                text = if (isUserLoggedIn) "Enter Security Dashboard" else "Sign In & Enter Dashboard",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PillarBadge(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    statusBadge: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (statusBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}