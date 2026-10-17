package app.javin.codex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
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
import app.javin.codex.data.model.ChatMessage
import app.javin.codex.data.repository.ChatRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatRoomScreen(
    roomId: Long,
    otherCode: String,
    otherName: String,
    otherAvatar: String?,
    otherBio: String? = null,
    otherLastSeen: Long = 0L,
    isGlobal: Boolean = false,
    onlineCount: Int = 0,
    totalUsers: Int = 0,
    onBack: () -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { ChatRepository(ApiClient.get()) }
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var lastMsgTime by remember { mutableStateOf(0L) }
    var otherTyping by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(roomId) {
        while (true) {
            try { otherTyping = repo.checkTyping(roomId) } catch (_: Exception) {}
            delay(1500)
        }
    }

    LaunchedEffect(roomId) {
        while (true) {
            try {
                val sinceTime = maxOf(0L, lastMsgTime - 300_000L)
                val r = repo.getMessages(roomId, sinceTime)
                r.onSuccess { newMsgs ->
                    if (newMsgs.isNotEmpty()) {
                        val merged = (messages + newMsgs).groupBy { it.id }.map { (_, l) -> l.last() }.sortedBy { it.createdAt }
                        messages = merged
                        val maxTime = newMsgs.maxOfOrNull { it.createdAt } ?: lastMsgTime
                        if (maxTime > lastMsgTime) {
                            lastMsgTime = maxTime
                            try { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
            loading = false
            delay(2500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(t.bg)
            .imePadding()
    ) {
        // ===== TOP BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(t.cardBg)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .clickable { showProfile = true },
                contentAlignment = Alignment.Center
            ) {
                if (!otherAvatar.isNullOrBlank()) {
                    AsyncImage(model = otherAvatar, contentDescription = otherName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(otherName.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                }
            }
            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showProfile = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = otherName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = t.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isGlobal) {
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF1DA1F2),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = when {
                            otherTyping -> "Sedang mengetik..."
                            isGlobal && onlineCount > 0 -> "$onlineCount online"
                            else -> "Online"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            otherTyping -> Color(0xFFFF3B5C)
                            isGlobal && onlineCount > 0 -> Color(0xFF22C55E)
                            else -> Color(0xFF22C55E)
                        }
                    )
                }
            }
        }

        // ===== MESSAGES =====
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages.size) { idx ->
                MessageBubble(messages[idx], messages[idx].fromMe, isGlobal)
            }
        }

        // ===== INPUT BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(t.cardBg)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    if (it.isNotBlank()) scope.launch { repo.sendTyping(roomId) }
                },
                placeholder = { Text("Ketik pesan...", fontSize = 14.sp, color = t.textMuted) },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = t.iconBg,
                    unfocusedContainerColor = t.iconBg,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    cursorColor = t.accent
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isBlank()) t.textMuted else t.accent)
                    .clickable(enabled = inputText.isNotBlank()) {
                        val text = inputText.trim()
                        inputText = ""
                        val temp = ChatMessage(
                            id = System.currentTimeMillis(),
                            roomId = roomId,
                            text = text,
                            createdAt = System.currentTimeMillis(),
                            fromMe = true
                        )
                        messages = messages + temp
                        scope.launch { repo.sendMessage(roomId, text) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, "Kirim", tint = t.textPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }

    // ===== PROFILE DIALOG (WA-style) =====
    if (showProfile) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showProfile = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(t.bg)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(t.iconBg).border(2.dp, t.cardBorder, CircleShape).clickable { showProfile = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(if (isGlobal) "Info Grup" else "Info Kontak", fontSize = 18.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                    }
                    
                    // Avatar besar
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(160.dp).clip(CircleShape).background(t.iconBg).border(3.dp, t.cardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!otherAvatar.isNullOrBlank()) {
                                AsyncImage(model = otherAvatar, contentDescription = otherName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(otherName.take(1).uppercase(), fontSize = 56.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(otherName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                            if (isGlobal) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, "Verified", tint = Color(0xFF1DA1F2), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        if (isGlobal) {
                            Text("${onlineCount} online · ${totalUsers} anggota", fontSize = 13.sp, color = t.success, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(otherCode, fontSize = 13.sp, color = t.textMuted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Info bio + bergabung
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        if (!otherBio.isNullOrBlank()) {
                            Text(if (isGlobal) "DESKRIPSI GRUP" else "TENTANG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.textMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(otherBio, fontSize = 14.sp, color = t.textPrimary)
                            Spacer(Modifier.height(16.dp))
                        }
                        Text("BERGABUNG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text("Oktober 2026", fontSize = 14.sp, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(24.dp))
                        if (!isGlobal) {
                            DialogMenuItem("Blokir Kontak", t.danger) { /* TODO */ }
                            Spacer(Modifier.height(8.dp))
                            DialogMenuItem("Laporkan", t.danger) { /* TODO */ }
                            Spacer(Modifier.height(8.dp))
                            DialogMenuItem("Hapus Kontak", t.danger) { /* TODO */ }
                        } else {
                            Text("ANGGOTA GRUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.textMuted)
                            Spacer(Modifier.height(8.dp))
                            Text("Semua user otomatis tergabung", fontSize = 13.sp, color = t.textMuted, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isFromMe: Boolean, isGlobal: Boolean = false) {
    val t = LocalThemeTokens.current
    val shape = if (isFromMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(if (isFromMe) t.accent else t.cardBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (isGlobal && !isFromMe && message.senderName.isNotBlank()) {
                    Text(message.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = t.accent, modifier = Modifier.padding(bottom = 2.dp))
                }
                Text(message.text, fontSize = 14.sp, color = if (isFromMe) t.accentOn else t.textPrimary, lineHeight = 19.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(formatMsgTime(message.createdAt))
                        if (isFromMe) {
                            append(" ")
                            append(if (message.readAt > 0) "✓✓" else "✓")
                        }
                    },
                    fontSize = 9.sp,
                    color = if (isFromMe) t.accentOn.copy(alpha = 0.7f) else t.textMuted
                )
            }
        }
    }
}

private fun formatMsgTime(ms: Long): String {
    if (ms <= 0) return ""
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ms
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

@Composable
private fun DialogMenuItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(t.cardBg)
            .border(2.dp, t.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
