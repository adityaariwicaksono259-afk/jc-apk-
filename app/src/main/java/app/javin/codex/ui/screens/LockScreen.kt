package app.javin.codex.ui.screens

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.javin.codex.data.lock.LockManager
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lockManager = remember { LockManager.getInstance(context) }

    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var showBiometric by remember { mutableStateOf(false) }

    // Cek biometric available
    LaunchedEffect(Unit) {
        val biometricEnabled = lockManager.isBiometricEnabled()
        if (biometricEnabled) {
            val bm = BiometricManager.from(context)
            val canAuth = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            showBiometric = canAuth == BiometricManager.BIOMETRIC_SUCCESS
            if (showBiometric) {
                // Auto-trigger biometric
                triggerBiometric(context, onSuccess = onUnlock, onError = {
                    showBiometric = false
                })
            }
        }
    }

    fun doVerify(input: String) {
        if (input.length < 4) {
            errorMsg = "PIN minimal 4 digit"
            return
        }
        scope.launch {
            checking = true
            val ok = lockManager.verifyPin(input)
            checking = false
            if (ok) {
                lockManager.updateLastActive()
                onUnlock()
            } else {
                attempts++
                errorMsg = if (attempts >= 5) "Terlalu banyak percobaan" else "PIN salah"
                pin = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F0F1E), Color(0xFF1A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("JC", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Masukkan PIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Untuk membuka Javin Codex",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(32.dp))

            // PIN DOTS
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                repeat(6) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) Color(0xFF4A9EFF)
                                else Color(0x1AFFFFFF)
                            )
                            .border(
                                2.dp,
                                if (filled) Color(0xFF4A9EFF)
                                else Color(0x33FFFFFF),
                                CircleShape
                            )
                    )
                }
            }

            // ERROR
            if (errorMsg != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    errorMsg!!,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // KEYPAD 1-9
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            KeypadButton(
                                label = digit,
                                onClick = {
                                    if (pin.length < 6) {
                                        errorMsg = null
                                        val newPin = pin + digit
                                        pin = newPin
                                        if (newPin.length >= 4) {
                                            // Auto-verify kalau length 6
                                            if (newPin.length == 6) doVerify(newPin)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Biometric slot
                    if (showBiometric) {
                        KeypadButton(
                            icon = Icons.Default.Fingerprint,
                            onClick = {
                                triggerBiometric(context, onSuccess = onUnlock, onError = {
                                    errorMsg = "Biometric gagal"
                                })
                            }
                        )
                    } else {
                        Spacer(Modifier.size(64.dp))
                    }
                    KeypadButton(label = "0", onClick = {
                        if (pin.length < 6) {
                            errorMsg = null
                            val newPin = pin + "0"
                            pin = newPin
                            if (newPin.length == 6) doVerify(newPin)
                        }
                    })
                    KeypadButton(
                        icon = Icons.Default.Backspace,
                        onClick = {
                            errorMsg = null
                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // OK BUTTON (kalau PIN < 6)
            if (pin.length >= 4 && pin.length < 6) {
                Button(
                    onClick = { doVerify(pin) },
                    enabled = !checking,
                    modifier = Modifier.width(200.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A9EFF),
                        contentColor = Color.White
                    )
                ) {
                    if (checking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Buka", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x0FFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
        } else if (label != null) {
            Text(
                label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

private fun triggerBiometric(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        val activity = context as? FragmentActivity ?: (context as? Activity) as? FragmentActivity
        if (activity == null) {
            onError()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError()
                }
                override fun onAuthenticationFailed() {
                    // Biarkan user coba lagi
                }
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Javin Codex")
            .setSubtitle("Gunakan biometrik untuk masuk")
            .setNegativeButtonText("Gunakan PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(info)
    } catch (e: Exception) {
        onError()
    }
}
