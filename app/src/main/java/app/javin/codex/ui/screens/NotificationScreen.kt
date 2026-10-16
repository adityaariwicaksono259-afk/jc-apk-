package app.javin.codex.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.notifDataStore by preferencesDataStore(name = "javin_notif")

@Composable
fun NotificationScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var msgNotif by remember { mutableStateOf(true) }
    var announceNotif by remember { mutableStateOf(true) }
    var updateNotif by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }

    // Key
    val keyMsg = booleanPreferencesKey("notif_msg")
    val keyAnnounce = booleanPreferencesKey("notif_announce")
    val keyUpdate = booleanPreferencesKey("notif_update")
    val keySound = booleanPreferencesKey("notif_sound")
    val keyVibration = booleanPreferencesKey("notif_vibration")

    // Load
    LaunchedEffect(Unit) {
        try {
            val prefs = context.notifDataStore.data.first()
            msgNotif = prefs[keyMsg] ?: true
            announceNotif = prefs[keyAnnounce] ?: true
            updateNotif = prefs[keyUpdate] ?: true
            sound = prefs[keySound] ?: true
            vibration = prefs[keyVibration] ?: true
        } catch (e: Exception) {}
        loading = false
    }

    fun save() {
        scope.launch {
            try {
                context.notifDataStore.edit { prefs ->
                    prefs[keyMsg] = msgNotif
                    prefs[keyAnnounce] = announceNotif
                    prefs[keyUpdate] = updateNotif
                    prefs[keySound] = sound
                    prefs[keyVibration] = vibration
                }
            } catch (e: Exception) {}
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
                Icon(Icons.Default.ArrowBack, null, tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Notifikasi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = t.textPrimary)
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4A9EFF))
            }
            return@Column
        }

        SectionHeader("Jenis Notifikasi")
        NotifToggle("Pesan chat", "Notif saat ada pesan masuk", msgNotif) { msgNotif = it; save() }
        NotifToggle("Pengumuman", "Notif dari admin/inbox", announceNotif) { announceNotif = it; save() }
        NotifToggle("Update aplikasi", "Notif saat ada versi baru", updateNotif) { updateNotif = it; save() }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Suara & Getar")

        NotifToggle("Suara", "Mainkan suara saat notif masuk", sound) { sound = it; save() }
        NotifToggle("Getar", "Getar saat notif masuk", vibration) { vibration = it; save() }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = t.textMuted,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun NotifToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
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
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.textPrimary)
            Text(subtitle, fontSize = 12.sp, color = t.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = t.textPrimary,
                checkedTrackColor = Color(0xFF4A9EFF)
            )
        )
    }
}
