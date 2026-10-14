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

    var currentTheme by remember { mutableStateOf(ThemeMode.DARK) }
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
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Pengaturan",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                ThemeMode.DARK -> "Gelap"
                ThemeMode.LIGHT -> "Terang"
                ThemeMode.SYSTEM -> "Ikut Sistem"
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
                        label = "Gelap",
                        selected = currentTheme == ThemeMode.DARK,
                        onClick = {
                            scope.launch { themeManager.setTheme(ThemeMode.DARK) }
                            currentTheme = ThemeMode.DARK
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = "Terang",
                        selected = currentTheme == ThemeMode.LIGHT,
                        onClick = {
                            scope.launch { themeManager.setTheme(ThemeMode.LIGHT) }
                            currentTheme = ThemeMode.LIGHT
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = "Ikut Sistem",
                        selected = currentTheme == ThemeMode.SYSTEM,
                        onClick = {
                            scope.launch { themeManager.setTheme(ThemeMode.SYSTEM) }
                            currentTheme = ThemeMode.SYSTEM
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
private fun SettingsItem(
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

@Composable
private fun StatusCard(online: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (online) Color(0x1A7FFFA0) else Color(0x1AFF6B6B)
            )
            .border(
                1.dp,
                if (online) Color(0x337FFFA0) else Color(0x33FF6B6B),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (online) Color(0xFF7FFFA0) else Color(0xFFFF6B6B))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (online) "Online" else "Offline",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (online) Color(0xFF7FFFA0) else Color(0xFFFF6B6B)
            )
            Text(
                text = if (online) "Koneksi internet aktif" else "Tidak ada koneksi internet",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
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
