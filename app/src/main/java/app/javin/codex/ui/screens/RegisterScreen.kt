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
fun RegisterScreen(
    authRepo: AuthRepository,
    onRegisterSuccess: (User) -> Unit,
    onNavigateLogin: () -> Unit = {}
) {
    var step by remember { mutableStateOf(LoginStep.PASSWORD) }  // reuse enum dari LoginScreen
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(600) }

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
                text = if (step == LoginStep.PASSWORD) "Buat Akun Baru" else "Verifikasi Email",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (step == LoginStep.PASSWORD)
                    "Daftar Javin Codex"
                else
                    "Kode OTP dikirim ke $email",
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
                        // NAME
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(60) },
                            label = { Text("Nama") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = registerTextFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))

                        // EMAIL
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = registerTextFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))

                        // PASSWORD
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (min 12 karakter)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !loading,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = registerTextFieldColors()
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    val result = authRepo.register(email, password, name.ifBlank { null })
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            step = LoginStep.OTP
                                            otp = ""
                                        }
                                        is AuthResult.Error -> errorMsg = result.message
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && password.isNotBlank() && !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A9EFF),
                                contentColor = Color.White
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Daftar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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
                            colors = registerTextFieldColors()
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Berlaku ${formatRegCountdown(countdown)}",
                            color = if (countdown < 60) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    val result = authRepo.verifyOtp(email, otp)
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> onRegisterSuccess(result.data)
                                        is AuthResult.Error -> errorMsg = result.message
                                    }
                                }
                            },
                            enabled = otp.length == 6 && countdown > 0 && !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A9EFF),
                                contentColor = Color.White
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Verifikasi & Masuk", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    authRepo.register(email, password, name.ifBlank { null })
                                    loading = false
                                    countdown = 600
                                }
                            },
                            enabled = !loading
                        ) {
                            Text("Kirim ulang kode", color = Color(0xFF4A9EFF), fontSize = 12.sp)
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
                            Text("< Kembali", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
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

            Row(horizontalArrangement = Arrangement.Center) {
                Text("Udah punya akun? ", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(
                    "Masuk",
                    color = Color(0xFF4A9EFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onNavigateLogin)
                )
            }
        }
    }
}

// ============================================
// HELPER LOCAL
// ============================================

@Composable
private fun registerTextFieldColors() = OutlinedTextFieldDefaults.colors(
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

private fun formatRegCountdown(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
