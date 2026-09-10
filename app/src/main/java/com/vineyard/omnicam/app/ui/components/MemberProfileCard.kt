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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vineyard.omnicam.app.data.models.UserProfile
import com.vineyard.omnicam.app.data.models.UserRole

/**
 * Visual profile identification card for Dashboard and Settings screens.
 * 
 * Distinctly displays the user's role ("House Admin" vs "House Member"),
 * verified email address, and active Google Drive backup status.
 */
@Composable
fun MemberProfileCard(
    userProfile: UserProfile,
    modifier: Modifier = Modifier
) {
    val role = UserRole.fromString(userProfile.role)
    val isAdmin = role == UserRole.ADMIN

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isAdmin) Color(0xFF1E3A5F) else Color(0xFF0F3E3E),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111923)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar or Fallback Initials
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isAdmin) Color(0xFF1E293B) else Color(0xFF064E3B)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = userProfile.displayName.firstOrNull()?.uppercase()
                        ?: userProfile.email.firstOrNull()?.uppercase()
                        ?: "U"
                    Text(
                        text = initial,
                        color = if (isAdmin) Color(0xFF38BDF8) else Color(0xFF34D399),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Display Name & Email Identifier
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile.displayName.ifBlank { if (isAdmin) "House Admin" else "House Member" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userProfile.email.ifBlank { "No email associated" },
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Strongly Defined Role Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAdmin) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFF065F46).copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAdmin) Color(0xFF3B82F6) else Color(0xFF10B981)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Role Badge",
                            tint = if (isAdmin) Color(0xFF60A5FA) else Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = role.displayName,
                            color = if (isAdmin) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Integration Status Strip (Cloud Drive & Network Status)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D131B))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (userProfile.driveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Drive Status",
                        tint = if (userProfile.driveConnected) Color(0xFF10B981) else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (userProfile.driveConnected) "15 GB Google Drive Active" else "Google Drive Disconnected",
                        color = if (userProfile.driveConnected) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (isAdmin) "Primary Hub" else "QR Linked",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}