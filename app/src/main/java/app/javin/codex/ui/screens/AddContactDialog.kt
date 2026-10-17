package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.ChatRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import kotlinx.coroutines.launch

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit = {},
    onSaved: (roomId: Long, code: String, name: String) -> Unit = { _, _, _ -> }
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { ChatRepository(ApiClient.get()) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(t.bg)
                .border(3.dp, Color(0xFF0A0A0A), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Tambah Kontak", fontSize = 20.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
            Spacer(Modifier.height(16.dp))

            Text("NAMA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.textMuted)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMsg = null },
                placeholder = { Text("Nama kontak", color = t.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedBorderColor = t.accent, unfocusedBorderColor = Color(0xFF0A0A0A),
                    focusedTextColor = t.textPrimary, unfocusedTextColor = t.textPrimary,
                    cursorColor = t.accent
                )
            )
            Spacer(Modifier.height(12.dp))
            Text("KODE USER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.textMuted)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; errorMsg = null },
                placeholder = { Text("Contoh: JH-DHD3HN", color = t.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedBorderColor = t.accent, unfocusedBorderColor = Color(0xFF0A0A0A),
                    focusedTextColor = t.textPrimary, unfocusedTextColor = t.textPrimary,
                    cursorColor = t.accent
                )
            )
            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = t.danger)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFF0A0A0A), RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                }
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(t.accent)
                        .border(2.dp, Color(0xFF0A0A0A), RoundedCornerShape(12.dp))
                        .clickable(enabled = !loading && name.isNotBlank() && code.isNotBlank()) {
                            scope.launch {
                                loading = true
                                errorMsg = null
                                val r = repo.openRoomWithUser(code.trim())
                                r.onSuccess { roomId -> onSaved(roomId, code.trim(), name.trim()) }
                                 .onFailure { e -> errorMsg = e.message ?: "Gagal" }
                                loading = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (loading) "..." else "Simpan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = t.accentOn)
                }
            }
        }
    }
}
