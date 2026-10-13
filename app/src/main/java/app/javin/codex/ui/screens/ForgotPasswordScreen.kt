package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.ui.components.VideoBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    authRepo: AuthRepository,
    onResetSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    var step by remember { mutableStateOf(1) }  // 1 = email+name, 2 = otp+password
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(600) }
    var successMode by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(step) {
        if (step == 2) {
            countdown = 600
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoBackground(videoResId = R.raw.login_bg, loop = true)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (successMode) "Password Direset!"
                       else if (step == 1) "Lupa Password?" else "Verifikasi & Password Baru",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (successMode) "Silakan login dengan password baru"
                       else if (step == 1) "Verifikasi akun kamu dulu" else "Masukkan kode OTP + password baru",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
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
                    if (successMode) {
                        // ==== SUCCESS STATE ====
                        Text(
                            text = "Password berhasil direset!\nSilakan login dengan password baru kamu.",
                            fontSize = 14.sp,
                            color = Color(0xFF7FFFA0),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onBackToLogin,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7FFFA0),
                                contentColor = Color(0xFF0A0A0A)
                            )
                        ) {
                            Text("Kembali ke Login", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (step == 1) {
                        // ==== STEP 1: EMAIL + NAMA ====
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim(); errorMsg = null },
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
                            colors = forgotFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMsg = null },
                            label = { Text("Nama yang terdaftar") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            enabled = !loading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = forgotFieldColors()
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    val result = authRepo.forgotRequest(email, name)
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            step = 2
                                            otp = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                        }
                                        is AuthResult.Error -> errorMsg = result.message
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && name.isNotBlank() && !loading,
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
                                Text("Kirim Kode OTP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // ==== STEP 2: OTP + PASSWORD BARU ====
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
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = forgotFieldColors()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Berlaku ${formatForgotCountdown(countdown)}",
                            color = if (countdown < 60) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; errorMsg = null },
                            label = { Text("Password Baru (min 12)") },
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
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = forgotFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMsg = null },
                            label = { Text("Konfirmasi Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            enabled = !loading,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = forgotFieldColors()
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                if (newPassword != confirmPassword) {
                                    errorMsg = "Password konfirmasi tidak cocok"
                                    return@Button
                                }
                                scope.launch {
                                    loading = true
                                    val result = authRepo.forgotConfirm(email, otp, newPassword)
                                    loading = false
                                    when (result) {
                                        is AuthResult.Success -> successMode = true
                                        is AuthResult.Error -> errorMsg = result.message
                                    }
                                }
                            },
                            enabled = otp.length == 6 && newPassword.isNotBlank() && confirmPassword.isNotBlank() && countdown > 0 && !loading,
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
                                Text("Reset Password", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                errorMsg = null
                                scope.launch {
                                    loading = true
                                    authRepo.forgotRequest(email, name)
                                    loading = false
                                    countdown = 600
                                }
                            },
                            enabled = !loading
                        ) {
                            Text("Kirim ulang kode", color = Color(0xFF4A9EFF), fontSize = 12.sp)
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

            if (!successMode) {
                Spacer(Modifier.height(20.dp))
                TextButton(
                    onClick = {
                        if (step == 2) {
                            step = 1
                            otp = ""
                            newPassword = ""
                            confirmPassword = ""
                            errorMsg = null
                        } else {
                            onBackToLogin()
                        }
                    },
                    enabled = !loading
                ) {
                    Text(
                        "< Kembali",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun forgotFieldColors() = OutlinedTextFieldDefaults.colors(
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

private fun formatForgotCountdown(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
