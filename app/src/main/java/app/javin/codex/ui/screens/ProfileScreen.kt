package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.session.SessionManager
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onPrivacySettings: () -> Unit = {},
    onNotificationSettings: () -> Unit = {},
    onSecuritySettings: () -> Unit = {},
    onAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager.getInstance(context) }

    var userName by remember { mutableStateOf("User") }
    var userCode by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userBio by remember { mutableStateOf("") }
    var userJoined by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userName = session.getUserName() ?: "User"
        userCode = session.getUserCode() ?: ""
        userEmail = session.getUserEmail() ?: ""
        // Format joined date dari saved_at
        val savedAt = try {
            session.getSavedAt()
        } catch (e: Exception) { 0L }
        userJoined = if (savedAt > 0) {
            try {
                val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID"))
                sdf.format(java.util.Date(savedAt))
            } catch (e: Exception) { "baru-baru ini" }
        } else "baru-baru ini"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ============================================
        // HEADER — FOTO + NAMA + BIO
        // ============================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto profil dengan ring gradient
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0), Color(0xFFFF7FE5))
                            )
                        )
                )
                // Photo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A2E))
                        .border(3.dp, Color(0xFF0F0F1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                // Edit badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A9EFF))
                        .border(2.dp, Color(0xFF0F0F1E), CircleShape)
                        .align(Alignment.BottomEnd)
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Nama
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            // User code
            if (userCode.isNotBlank()) {
                Text(
                    text = "@$userCode",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Bio
            Text(
                text = userBio.ifBlank { "Belum ada bio" },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Joined date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Bergabung ${userJoined.ifBlank { "baru-baru ini" }}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ============================================
        // SECTION: PRIVASI & PENGATURAN
        // ============================================
        SectionTitle("Privasi & Pengaturan")

        GlassMenuItem(
            icon = Icons.Default.PrivacyTip,
            label = "Privasi",
            subtitle = "Last seen, foto profil, status",
            onClick = onPrivacySettings
        )
        GlassMenuItem(
            icon = Icons.Default.Notifications,
            label = "Notifikasi",
            subtitle = "Pesan, grup, status",
            onClick = onNotificationSettings
        )
        GlassMenuItem(
            icon = Icons.Default.Security,
            label = "Keamanan",
            subtitle = "PIN, biometrik, auto-lock",
            onClick = onSecuritySettings
        )

        Spacer(Modifier.height(8.dp))

        // ============================================
        // SECTION: LAINNYA
        // ============================================
        SectionTitle("Lainnya")

        GlassMenuItem(
            icon = Icons.Default.Info,
            label = "Tentang Javin Codex",
            subtitle = "Versi 1.0.0",
            onClick = onAbout
        )
        GlassMenuItem(
            icon = Icons.Default.HelpOutline,
            label = "Bantuan",
            subtitle = "FAQ, kontak support",
            onClick = { /* TODO */ }
        )

        Spacer(Modifier.height(24.dp))

        // ============================================
        // LOGOUT BUTTON
        // ============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        session.clearSession()
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FF6B6B), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Logout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Bottom spacer untuk bottom nav
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0))
                    )
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun GlassMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4A9EFF),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF0F0F5)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0x66FFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}
