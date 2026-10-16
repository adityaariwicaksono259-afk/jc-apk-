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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.model.PrivacySettings
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.data.model.PrivacyUpdateRequest
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(
    authRepo: AuthRepository,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var lastSeen by remember { mutableStateOf("everyone") }
    var profilePhoto by remember { mutableStateOf("everyone") }
    var status by remember { mutableStateOf("everyone") }
    var readReceipts by remember { mutableStateOf(true) }

    var showDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        val r = authRepo.getPrivacy()
        if (r is AuthResult.Success) {
            lastSeen = r.data.lastSeen
            profilePhoto = r.data.profilePhoto
            status = r.data.status
            readReceipts = r.data.readReceipts
        }
        loading = false
    }

    fun savePrivacy() {
        scope.launch {
            authRepo.updatePrivacy(PrivacyUpdateRequest(
                lastSeen = lastSeen,
                profilePhoto = profilePhoto,
                status = status,
                readReceipts = readReceipts
            ))
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // HEADER
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
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Privasi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4A9EFF))
            }
            return@Column
        }

        // SECTION: SIAPA YANG BISA LIHAT
        SectionHeader("Siapa yang bisa lihat")

        PrivacyItem(
            label = "Terakhir dilihat",
            subtitle = labelOf(lastSeen),
            onClick = { showDialog = "last_seen" }
        )
        PrivacyItem(
            label = "Foto profil",
            subtitle = labelOf(profilePhoto),
            onClick = { showDialog = "profile_photo" }
        )
        PrivacyItem(
            label = "Status",
            subtitle = labelOf(status),
            onClick = { showDialog = "status" }
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader("Baca & konfirmasi")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Laporan dibaca", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    if (readReceipts) "Aktif — kirim centang biru" else "Nonaktif",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = readReceipts,
                onCheckedChange = {
                    readReceipts = it
                    savePrivacy()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4A9EFF)
                )
            )
        }

        Spacer(Modifier.height(100.dp))
    }

    // DIALOG PICKER
    val currentDialog = showDialog
    if (currentDialog != null) {
        val currentValue = when (currentDialog) {
            "last_seen" -> lastSeen
            "profile_photo" -> profilePhoto
            "status" -> status
            else -> "everyone"
        }
        val title = when (currentDialog) {
            "last_seen" -> "Terakhir dilihat"
            "profile_photo" -> "Foto profil"
            "status" -> "Status"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "everyone" to "Semua orang",
                        "contacts" to "Kontak saya",
                        "nobody" to "Tidak ada"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    when (currentDialog) {
                                        "last_seen" -> lastSeen = value
                                        "profile_photo" -> profilePhoto = value
                                        "status" -> status = value
                                    }
                                    showDialog = null
                                    savePrivacy()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = {
                                    when (currentDialog) {
                                        "last_seen" -> lastSeen = value
                                        "profile_photo" -> profilePhoto = value
                                        "status" -> status = value
                                    }
                                    showDialog = null
                                    savePrivacy()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = null }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.8f),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun PrivacyItem(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
        Text("Ubah", fontSize = 12.sp, color = Color(0xFF4A9EFF), fontWeight = FontWeight.SemiBold)
    }
}

private fun labelOf(value: String) = when (value) {
    "everyone" -> "Semua orang"
    "contacts" -> "Kontak saya"
    "nobody" -> "Tidak ada"
    else -> value
}
