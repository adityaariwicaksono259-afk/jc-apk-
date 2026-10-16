package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.data.session.SessionManager
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    authRepo: AuthRepository,
    onBack: () -> Unit = {},
    onSaved: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager.getInstance(context) }

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var originalName by remember { mutableStateOf("") }
    var originalBio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Load current profile
    LaunchedEffect(Unit) {
        name = session.getUserName() ?: ""
        bio = "" // TODO: ambil dari /api/auth/me
        originalName = name
        originalBio = bio
    }

    val hasChanges = name != originalName || bio != originalBio

    Column(modifier = Modifier.fillMaxSize()) {
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
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Edit Profil",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).ifBlank { "?" }.uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = t.textPrimary
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap untuk ganti foto (coming soon)",
                fontSize = 11.sp,
                color = t.textMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))

            // Name field
            Text(
                text = "NAMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = t.textMuted,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60); errorMsg = null; successMsg = null },
                placeholder = { Text("Nama kamu", color = t.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x0FFFFFFF),
                    unfocusedContainerColor = Color(0x0FFFFFFF),
                    focusedBorderColor = Color(0xFF4A9EFF),
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    cursorColor = Color(0xFF4A9EFF)
                )
            )

            Spacer(Modifier.height(20.dp))

            // Bio field
            Text(
                text = "BIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = t.textMuted,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200); errorMsg = null; successMsg = null },
                placeholder = { Text("Ceritain tentang kamu...", color = t.textMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x0FFFFFFF),
                    unfocusedContainerColor = Color(0x0FFFFFFF),
                    focusedBorderColor = Color(0xFF4A9EFF),
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    cursorColor = Color(0xFF4A9EFF)
                )
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "${bio.length}/200",
                fontSize = 10.sp,
                color = t.textMuted,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(Modifier.height(24.dp))

            // SAVE BUTTON
            Button(
                onClick = {
                    errorMsg = null
                    successMsg = null
                    scope.launch {
                        loading = true
                        val r = authRepo.updateProfile(name, bio)
                        loading = false
                        when (r) {
                            is AuthResult.Success -> {
                                successMsg = "Profil berhasil disimpan"
                                originalName = name
                                originalBio = bio
                                onSaved(name)
                            }
                            is AuthResult.Error -> errorMsg = r.message
                        }
                    }
                },
                enabled = hasChanges && name.isNotBlank() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A9EFF),
                    contentColor = t.textPrimary,
                    disabledContainerColor = Color(0x334A9EFF),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = t.textPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error / success message
            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FF6B6B))
                        .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "❌ $errorMsg",
                        color = Color(0xFFFFB0B0),
                        fontSize = 13.sp
                    )
                }
            }

            if (successMsg != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x337FFFA0))
                        .border(1.dp, Color(0x667FFFA0), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✅ $successMsg",
                        color = Color(0xFF7FFFA0),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
