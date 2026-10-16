package app.javin.codex.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.lock.LockManager
import kotlinx.coroutines.launch

@Composable
fun PinSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lockManager = remember { LockManager.getInstance(context) }

    var lockEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var timeoutMs by remember { mutableLongStateOf(0L) }
    var biometricAvailable by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var showChangeDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        lockEnabled = lockManager.isLockEnabled()
        biometricEnabled = lockManager.isBiometricEnabled()
        timeoutMs = lockManager.getTimeout()
        val bm = BiometricManager.from(context)
        biometricAvailable = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Kunci Aplikasi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = t.textPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // ==== PIN SECTION ====
            SectionTitle("PIN")

            PinRow(
                label = if (lockEnabled) "Kunci Aktif" else "Kunci Nonaktif",
                subtitle = if (lockEnabled) "PIN 6 digit aktif" else "Aktifkan buat keamanan ekstra",
                action = if (lockEnabled) "Matikan" else "Aktifkan",
                onClick = {
                    errorMsg = null
                    if (lockEnabled) {
                        scope.launch {
                            lockManager.disableLock()
                            lockEnabled = false
                            biometricEnabled = false
                        }
                    } else {
                        showSetupDialog = true
                    }
                }
            )

            if (lockEnabled) {
                Spacer(Modifier.height(8.dp))
                PinRow(
                    label = "Ganti PIN",
                    subtitle = "Ubah PIN 6 digit kamu",
                    action = "Ubah",
                    onClick = { showChangeDialog = true }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ==== BIOMETRIC ====
            SectionTitle("Biometrik")
            PinRow(
                label = if (biometricAvailable) "Sidik jari / Wajah" else "Tidak tersedia",
                subtitle = if (!biometricAvailable) "Device tidak support biometrik"
                          else if (!lockEnabled) "Aktifkan PIN dulu"
                          else "Buka pakai sidik jari atau wajah",
                action = if (biometricEnabled) "Aktif" else "Nonaktif",
                actionColor = if (biometricEnabled) Color(0xFF7FFFA0) else null,
                enabled = biometricAvailable && lockEnabled,
                onClick = {
                    if (lockEnabled && biometricAvailable) {
                        scope.launch {
                            val next = !biometricEnabled
                            lockManager.setBiometric(next)
                            biometricEnabled = next
                        }
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // ==== AUTO LOCK ====
            SectionTitle("Auto-lock")
            PinRow(
                label = "Kunci otomatis",
                subtitle = timeoutLabel(timeoutMs),
                action = "Ubah",
                enabled = lockEnabled,
                onClick = { if (lockEnabled) showTimeoutDialog = true }
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FF6B6B))
                        .padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFFB0B0), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // ==== SETUP PIN DIALOG ====
    if (showSetupDialog) {
        PinInputDialog(
            title = "Buat PIN Baru",
            subtitle = "6 digit angka",
            onDismiss = { showSetupDialog = false },
            onSubmit = { pin, confirm ->
                errorMsg = null
                if (pin.length != 6 || !pin.all { it.isDigit() }) {
                    errorMsg = "PIN harus 6 digit angka"
                    return@PinInputDialog
                }
                if (pin != confirm) {
                    errorMsg = "PIN konfirmasi tidak cocok"
                    return@PinInputDialog
                }
                scope.launch {
                    lockManager.setupPin(pin)
                    lockManager.setTimeout(LockManager.TIMEOUT_IMMEDIATE)
                    lockEnabled = true
                    timeoutMs = LockManager.TIMEOUT_IMMEDIATE
                    showSetupDialog = false
                }
            },
            errorMsg = errorMsg
        )
    }

    // ==== CHANGE PIN DIALOG ====
    if (showChangeDialog) {
        PinInputDialog(
            title = "Ganti PIN",
            subtitle = "6 digit angka baru",
            onDismiss = { showChangeDialog = false },
            onSubmit = { pin, confirm ->
                errorMsg = null
                if (pin.length != 6 || !pin.all { it.isDigit() }) {
                    errorMsg = "PIN harus 6 digit angka"
                    return@PinInputDialog
                }
                if (pin != confirm) {
                    errorMsg = "PIN konfirmasi tidak cocok"
                    return@PinInputDialog
                }
                scope.launch {
                    lockManager.setupPin(pin)
                    showChangeDialog = false
                }
            },
            errorMsg = errorMsg
        )
    }

    // ==== TIMEOUT DIALOG ====
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Kunci otomatis setelah", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        LockManager.TIMEOUT_IMMEDIATE to "Langsung",
                        LockManager.TIMEOUT_1MIN to "1 menit",
                        LockManager.TIMEOUT_5MIN to "5 menit",
                        LockManager.TIMEOUT_30MIN to "30 menit"
                    ).forEach { (ms, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch {
                                        lockManager.setTimeout(ms)
                                        timeoutMs = ms
                                        showTimeoutDialog = false
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = timeoutMs == ms,
                                onClick = {
                                    scope.launch {
                                        lockManager.setTimeout(ms)
                                        timeoutMs = ms
                                        showTimeoutDialog = false
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = t.textMuted, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun PinRow(
    label: String,
    subtitle: String,
    action: String,
    actionColor: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (enabled) t.textPrimary else Color.White.copy(alpha = 0.5f))
            Text(subtitle, fontSize = 12.sp, color = t.textMuted)
        }
        Text(
            action,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.3f)
                actionColor != null -> actionColor
                else -> Color(0xFF4A9EFF)
            }
        )
    }
}

@Composable
private fun PinInputDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    errorMsg: String?
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) pin = v },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) confirm = v },
                    label = { Text("Konfirmasi PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, color = Color(0xFFCC0000), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin, confirm) }) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun timeoutLabel(ms: Long): String = when (ms) {
    LockManager.TIMEOUT_IMMEDIATE -> "Langsung saat keluar app"
    LockManager.TIMEOUT_1MIN -> "Setelah 1 menit"
    LockManager.TIMEOUT_5MIN -> "Setelah 5 menit"
    LockManager.TIMEOUT_30MIN -> "Setelah 30 menit"
    else -> "Langsung"
}
