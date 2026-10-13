package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.R
import app.javin.codex.data.model.User
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.ui.components.VideoBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LoginStep { PASSWORD, OTP }

@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: (User) -> Unit,
    onNavigateRegister: () -> Unit = {}
) {
    var step by remember { mutableStateOf(LoginStep.PASSWORD) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showResetButton by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(600) }
    var showResetDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(step) {
        if (step == LoginStep.OTP) {
            countdown = 600
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoBackground(videoResId = R.raw.login_bg, loop = true)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (step == LoginStep.PASSWORD) "Welcome Back!" else "Cek Email Kamu",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (step == LoginStep.PASSWORD) "Masuk ke Javin Codex" else "Kode OTP dikirim ke $email",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (step == LoginStep.PASSWORD) {
                        FieldEmail(
                            value = email,
                            onValueChange = { email = it.trim() },
                            enabled = !loading,
                            imeAction = ImeAction.Next
                        )
                        Spacer(Modifier.height(12.dp))
                        FieldPassword(
                            value = password,
                            onValueChange = { password = it },
                            visible = passwordVisible,
                            onToggleVisible = { passwordVisible = !passwordVisible },
                            enabled = !loading,
                            imeAction = ImeAction.Done
                        )

                        if (showResetButton) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showResetDialog = true }) {
                                Text(
                                    "Lupa password? Ubah password",
                                    color = Color(0xFFFFAA00),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        ButtonPrimary(
                            text = "Lanjut",
                            loading = loading,
                            enabled = email.isNotBlank() && password.isNotBlank() && !loading,
                            onClick = {
                                errorMsg = null
                                showResetButton = false
                                scope.launch {
                                    loading = true
                                    val result = authRepo.loginPassword(email, password)
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            step = LoginStep.OTP
                                            otp = ""
                                        }
                                        is AuthResult.Error -> {
                                            errorMsg = result.message
                                            if (result.code == "WRONG_PASSWORD") showResetButton = true
                                        }
                                    }
                                }
                            }
                        )

                    } else {
                        // ==== STEP 2: OTP ====
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { v ->
                                if (v.length <= 6 && v.all { it.isDigit() }) otp = v
                            },
                            label = { Text("Kode OTP (6 digit)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors()
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Berlaku ${formatCountdown(countdown)}",
                            color = if (countdown < 60) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        ButtonPrimary(
                            text = "Masuk",
                            loading = loading,
                            enabled = otp.length == 6 && countdown > 0 && !loading,
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    val result = authRepo.verifyOtp(email, otp)
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> onLoginSuccess(result.data)
                                        is AuthResult.Error -> errorMsg = result.message
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    authRepo.loginPassword(email, password)
                                    loading = false
                                    countdown = 600
                                }
                            },
                            enabled = !loading
                        ) {
                            Text(
                                "Kirim ulang kode",
                                color = Color(0xFF4A9EFF),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                step = LoginStep.PASSWORD
                                otp = ""
                                errorMsg = null
                                countdown = 600
                            },
                            enabled = !loading
                        ) {
                            Text(
                                "< Kembali ke email",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = errorMsg!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text("  atau  ", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { /* TODO: Google Sign-In */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("G", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Lanjutkan dengan Google", fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Baru di Javin? ", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(
                    "Daftar",
                    color = Color(0xFF4A9EFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onNavigateRegister)
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = { Text("Kirim link reset ke $email?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        loading = true
                        val r = authRepo.resetPassword(email)
                        loading = false
                        errorMsg = when (r) {
                            is AuthResult.Success -> "Link reset dikirim ke email"
                            is AuthResult.Error -> r.message
                        }
                    }
                }) { Text("Kirim") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }
}

// ============================================
// HELPER COMPOSABLES
// ============================================

@Composable
private fun FieldEmail(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = textFieldColors()
    )
}

@Composable
private fun FieldPassword(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Sembunyikan" else "Tampilkan"
                )
            }
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = textFieldColors()
    )
}

@Composable
private fun ButtonPrimary(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A9EFF),
            contentColor = Color.White
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF4A9EFF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = Color(0xFF4A9EFF),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedLeadingIconColor = Color(0xFF4A9EFF),
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
    focusedTrailingIconColor = Color(0xFF4A9EFF),
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFF4A9EFF)
)

private fun formatCountdown(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
