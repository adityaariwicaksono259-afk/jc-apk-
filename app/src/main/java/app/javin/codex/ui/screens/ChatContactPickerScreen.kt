package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.ChatContact
import app.javin.codex.data.repository.ChatRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatContactPickerScreen(
    onBack: () -> Unit = {},
    onContactSelected: (roomId: Long, code: String, name: String, avatar: String?, bio: String?, lastSeen: Long) -> Unit = { _, _, _, _, _, _ -> }
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { ChatRepository(ApiClient.get()) }

    var contacts by remember { mutableStateOf<List<ChatContact>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var opening by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val r = repo.getContacts()
            r.onSuccess { contacts = it }
        } catch (_: Exception) {}
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(t.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(t.accentSoft).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Pilih Kontak", fontSize = 22.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
            contacts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada kontak", color = t.textMuted, fontSize = 14.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts.size) { idx ->
                    val c = contacts[idx]
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(2.dp, Color(0xFF0A0A0A), RoundedCornerShape(14.dp))
                            .clickable(enabled = opening != c.code) {
                                opening = c.code
                                scope.launch {
                                    val r = repo.openRoomWithUser(c.code)
                                    r.onSuccess { roomId ->
                                        onContactSelected(roomId, c.code, c.name, c.avatar, c.bio, if (c.isOnline) System.currentTimeMillis() else 0L)
                                    }
                                    opening = null
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(t.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!c.avatar.isNullOrBlank()) {
                                AsyncImage(model = c.avatar, contentDescription = c.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(c.name.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (c.isOnline) "Online" else (c.bio ?: "Offline"),
                                fontSize = 11.sp,
                                color = if (c.isOnline) t.success else t.textMuted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (opening == c.code) CircularProgressIndicator(color = t.accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}
