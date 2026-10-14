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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.ToolRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch

@Composable
fun AmPremScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }

    var action by remember { mutableStateOf("send-magiclink") }
    var email by remember { mutableStateOf("") }
    var rawLink by remember { mutableStateOf("") }
    var idToken by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val actions = listOf(
        "send-magiclink" to "Kirim Magic Link",
        "verify-account" to "Verifikasi Akun",
        "apply-premium" to "Aktifkan Premium"
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("AM Premium", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Aktivasi akun Alight Motion Premium", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))

            // ACTION SELECTOR
            Text("AKSI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))

            actions.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { action = key; errorMsg = null }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = action == key,
                        onClick = { action = key; errorMsg = null },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF4A9EFF),
                            unselectedColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 14.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // EMAIL
            Text("EMAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim(); errorMsg = null },
                placeholder = { Text("email@example.com", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = amFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            // RAW LINK (kalau verify-account)
            if (action == "verify-account") {
                Text("MAGIC LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rawLink,
                    onValueChange = { rawLink = it.trim(); errorMsg = null },
                    placeholder = { Text("https://...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    enabled = !loading,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = amFieldColors()
                )
                Spacer(Modifier.height(12.dp))
            }

            // ID TOKEN (kalau apply-premium)
            if (action == "apply-premium") {
                Text("ID TOKEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = idToken,
                    onValueChange = { idToken = it.trim(); errorMsg = null },
                    placeholder = { Text("ID token dari magic link...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    enabled = !loading,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = amFieldColors()
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    errorMsg = null
                    resultText = null
                    scope.launch {
                        loading = true
                        // Build body JSON
                        val bodyMap = mutableMapOf<String, String>()
                        bodyMap["action"] = action
                        bodyMap["email"] = email
                        if (action == "verify-account" && rawLink.isNotBlank()) bodyMap["rawLink"] = rawLink
                        if (action == "apply-premium" && idToken.isNotBlank()) bodyMap["idToken"] = idToken
                        val bodyJson = Gson().toJson(bodyMap)

                        val r = repo.amPrem(bodyJson)
                        loading = false
                        r.onSuccess { resultText = it }
                            .onFailure { errorMsg = it.message }
                    }
                },
                enabled = email.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A9EFF), contentColor = Color.White,
                    disabledContainerColor = Color(0x334A9EFF), disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Kirim", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FF6B6B))
                        .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFFB0B0), fontSize = 13.sp)
                }
            }

            if (resultText != null) {
                Spacer(Modifier.height(16.dp))
                Text("HASIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(resultText!!, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun amFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF4A9EFF),
    unfocusedBorderColor = Color(0x1AFFFFFF),
    cursorColor = Color(0xFF4A9EFF)
)
