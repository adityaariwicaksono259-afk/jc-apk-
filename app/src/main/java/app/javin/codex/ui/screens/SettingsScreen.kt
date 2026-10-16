package app.javin.codex.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.data.session.ThemeManager
import app.javin.codex.data.session.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager.getInstance(context) }
    val t = LocalThemeTokens.current

    var currentTheme by remember { mutableStateOf(ThemeMode.GLASS) }
    var isOnline by remember { mutableStateOf(checkInternet(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Load theme
    LaunchedEffect(Unit) {
        currentTheme = themeManager.getTheme()
    }

    // Check internet tiap 3 detik
    LaunchedEffect(Unit) {
        while (true) {
            isOnline = checkInternet(context)
            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ============================================
        // HEADER
        // ============================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .border(if (t.isNeo) 2.dp else 1.dp, t.cardBorder, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Pengaturan",
                fontSize = 22.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
        }

        // ============================================
        // INTERNET STATUS
        // ============================================
        StatusCard(
            online = isOnline,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ============================================
        // SECTION: TAMPILAN
        // ============================================
        SectionTitle("Tampilan")

        SettingsItem(
            icon = Icons.Default.DarkMode,
            label = "Tema",
            subtitle = when (currentTheme) {
                ThemeMode.GLASS -> "Glassmorphism (default)"
                ThemeMode.NEOBRUTALISM -> "NeoBrutalism (soft)"
                ThemeMode.SKELMORPHISM -> "Skeuomorphism (3D)"
            },
            onClick = { showThemeDialog = true }
        )

        Spacer(Modifier.height(16.dp))

        // ============================================
        // SECTION: APLIKASI
        // ============================================
        SectionTitle("Aplikasi")

        SettingsItem(
            icon = Icons.Default.Update,
            label = "Update Aplikasi",
            subtitle = "Cek versi terbaru",
            onClick = { /* TODO: check update */ }
        )
        SettingsItem(
            icon = Icons.Default.Info,
            label = "Tentang Javin Codex",
            subtitle = "Versi 1.0.0",
            onClick = { showAboutDialog = true }
        )
        SettingsItem(
            icon = Icons.Default.Delete,
            label = "Hapus Cache",
            subtitle = "Bersihkan cache aplikasi",
            onClick = { /* TODO: clear cache */ }
        )

        Spacer(Modifier.height(100.dp))
    }

    // ============================================
    // THEME DIALOG
    // ============================================
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOption(
                        label = "Glassmorphism",
                        selected = currentTheme == ThemeMode.GLASS,
                        onClick = {
                            scope.launch { themeManager.setTheme(ThemeMode.GLASS) }
                            currentTheme = ThemeMode.GLASS
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = "NeoBrutalism",
                        selected = currentTheme == ThemeMode.NEOBRUTALISM,
                        onClick = {
                            scope.launch { themeManager.setTheme(ThemeMode.NEOBRUTALISM) }
                            currentTheme = ThemeMode.NEOBRUTALISM
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // ============================================
    // ABOUT DIALOG
    // ============================================
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Tentang Javin Codex", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Javin Codex", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Versi 1.0.0 (Build 1)", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aplikasi all-in-one untuk AI, tools, downloader, dan banyak lagi.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "© 2026 Javin Codex",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

// ============================================
// HELPER COMPOSABLES
// ============================================

@Composable
private fun SectionTitle(text: String) {
    val t = LocalThemeTokens.current
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
                .background(t.accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = t.textMuted,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(if (t.isNeo) 12.dp else 16.dp))
            .background(t.cardBg)
            .border(
                if (t.isNeo) 2.dp else 1.dp,
                t.cardBorder,
                RoundedCornerShape(if (t.isNeo) 12.dp else 16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(t.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (t.isNeo) t.iconTint else t.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (t.isNeo) FontWeight.Bold else FontWeight.SemiBold,
                color = t.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = t.textMuted
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = t.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatusCard(online: Boolean, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (online) t.success.copy(alpha = 0.15f) else t.danger.copy(alpha = 0.15f)
            )
            .border(
                if (t.isNeo) 2.dp else 1.dp,
                if (online) t.success else t.danger,
                RoundedCornerShape(if (t.isNeo) 12.dp else 16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (online) t.success else t.danger)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (online) "Online" else "Offline",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (online) t.success else t.danger
            )
            Text(
                text = if (online) "Koneksi internet aktif" else "Tidak ada koneksi internet",
                fontSize = 11.sp,
                color = t.textMuted
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ============================================
// UTILS
// ============================================

fun checkInternet(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        false
    }
}
