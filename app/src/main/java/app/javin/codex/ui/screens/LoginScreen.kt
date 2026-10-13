package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import app.javin.codex.data.model.User
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.ui.components.VideoBackground
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: (User) -> Unit,
    onNavigateRegister: () -> Unit = {},
    onNavigateResetPassword: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<String?>(null) }
    var showNotRegisteredDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Video background (loop)
        VideoBackground(videoResId = R.raw.login_bg, loop = true)

        // Dark overlay biar form kebaca
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TITLE
            Text(
                text = "Welcome back to Javin Codex",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in to continue",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            // FORM CARD
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // EMAIL
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); errorMsg = null; errorCode = null },
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
                        colors = loginTextFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))

                    // PASSWORD
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null; errorCode = null },
                        label = { Text("Password") },
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
                        colors = loginTextFieldColors()
                    )

                    // Reset button (kalau password salah ATAU belum punya password)
                    if (errorCode == "WRONG_PASSWORD" || errorCode == "NO_PASSWORD") {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onNavigateResetPassword() }) {
                            Text(
                                if (errorCode == "NO_PASSWORD") "Set password sekarang"
                                else "Lupa password? Ubah password",
                                color = Color(0xFFFFAA00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Daftar button (kalau belum terdaftar)
                    if (errorCode == "NOT_REGISTERED") {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showNotRegisteredDialog = true }) {
                            Text(
                                "Belum punya akun? Daftar",
                                color = Color(0xFF4A9EFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // LOGIN BUTTON
                    Button(
                        onClick = {
                            errorMsg = null
                            errorCode = null
                            scope.launch {
                                loading = true
                                val result = authRepo.loginPassword(email, password)
                                loading = false
                                when (result) {
                                    is AuthResult.Success -> onLoginSuccess(result.data)
                                    is AuthResult.Error -> {
                                        errorMsg = result.message
                                        errorCode = result.code
                                    }
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
                            Text("Masuk", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Belum punya akun? ", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
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


    // Not registered dialog
    if (showNotRegisteredDialog) {
        AlertDialog(
            onDismissRequest = { showNotRegisteredDialog = false },
            title = { Text("Belum Terdaftar") },
            text = { Text("Akun $email belum terdaftar.\n\nMau daftar sekarang?") },
            confirmButton = {
                TextButton(onClick = {
                    showNotRegisteredDialog = false
                    onNavigateRegister()
                }) { Text("Daftar") }
            },
            dismissButton = {
                TextButton(onClick = { showNotRegisteredDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
