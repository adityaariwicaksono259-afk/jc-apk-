package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import app.javin.codex.data.model.ChatRoom
import app.javin.codex.data.repository.ChatRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun ChatListScreen(
    onOpenRoom: (roomId: Long, otherCode: String, otherName: String, otherAvatar: String?, otherBio: String?, otherLastSeen: Long) -> Unit = { _, _, _, _, _, _ -> },
    onNewChat: () -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val repo = remember { ChatRepository(ApiClient.get()) }

    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Load + polling (safe)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val r = repo.getRooms()
                r.onSuccess {
                    rooms = it
                    loading = false
                }.onFailure {
                    loading = false
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                loading = false
            }
            delay(3000)
        }
    }

    val filtered = if (searchQuery.isBlank()) rooms
    else rooms.filter {
        it.other.name.lowercase().contains(searchQuery.lowercase()) ||
        (it.lastMessage?.text ?: "").lowercase().contains(searchQuery.lowercase())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== HEADER =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = t.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                // New chat button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(t.accent)
                        .border(2.dp, Color(0xFF0A0A0A), CircleShape)
                        .clickable(onClick = { /* TODO */ }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Chat baru",
                        tint = t.accentOn,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari chat atau user...", fontSize = 14.sp, color = t.textMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = t.textPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0A0A0A),
                    unfocusedBorderColor = Color(0xFF0A0A0A),
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    cursorColor = t.accent
                )
            )
        }

        // ===== LIST =====
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum ada chat", color = t.textMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap + untuk mulai chat", color = t.textMuted, fontSize = 12.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered.size) { idx ->
                    val room = filtered[idx]
                    ChatRoomRow(
                        room = room,
                        onClick = {
                            onOpenRoom(
                                room.id,
                                room.other.code,
                                room.other.name,
                                room.other.avatar,
                                room.other.bio,
                                room.other.lastSeen
                            )
                        }
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun ChatRoomRow(
    room: ChatRoom,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(2.dp, Color(0xFF0A0A0A), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(t.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            if (!room.other.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = room.other.avatar,
                    contentDescription = room.other.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = room.other.name.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = t.textPrimary
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.other.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0A0A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (room.lastMessageAt > 0) {
                    Text(
                        text = formatTime(room.lastMessageAt),
                        fontSize = 10.sp,
                        color = t.textMuted
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (room.lastMessage?.fromMe == true) "✓ ${room.lastMessage?.text ?: ""}"
                           else room.lastMessage?.text ?: "Mulai chat",
                    fontSize = 12.sp,
                    color = t.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (room.unread > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(t.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (room.unread > 99) "99+" else room.unread.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = t.accentOn
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - ms
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ms
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val min = cal.get(java.util.Calendar.MINUTE)
    val hhmm = "${hour.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}"

    return when {
        diff < 24 * 3600_000L -> hhmm
        diff < 7 * 24 * 3600_000L -> {
            val days = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
        else -> {
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.get(java.util.Calendar.MONTH) + 1
            "$day/$month"
        }
    }
}
