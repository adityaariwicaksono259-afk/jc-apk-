package app.javin.codex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    BackHandler(enabled = true) { onBack() }

    // Polling typing indicator
    LaunchedEffect(roomId) {
        while (true) {
            try {
                otherTyping = repo.checkTyping(roomId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {}
            delay(1500)
        }
    }

    // Load + polling (safe)
    LaunchedEffect(roomId) {
        while (true) {
            try {
                val sinceTime = maxOf(0L, lastMsgTime - 300_000L)
                val r = repo.getMessages(roomId, sinceTime)
                r.onSuccess { newMsgs ->
                    if (newMsgs.isNotEmpty()) {
                        val merged = (messages + newMsgs).groupBy { it.id }.map { (_, list) -> list.last() }
                            .sortedBy { it.createdAt }
                        messages = merged
                        val maxTime = newMsgs.maxOfOrNull { it.createdAt } ?: lastMsgTime
                        if (maxTime > lastMsgTime) {
                            lastMsgTime = maxTime
                            try {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // ignore
            }
            loading = false
            delay(2500)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== TOP BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(0.dp, Color.Transparent)
                .padding(horizontal = 12.dp)
                .padding(top = 48.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.accentSoft)
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
            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(t.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                if (!otherAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = otherAvatar,
                        contentDescription = otherName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = otherName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = t.textPrimary
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0A0A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (otherTyping) "Sedang mengetik..." else "Online",
                    fontSize = 11.sp,
                    color = if (otherTyping) t.accent else t.success,
                    fontWeight = if (otherTyping) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // ===== MESSAGES =====
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages.size) { idx ->
                val msg = messages[idx]
                MessageBubble(
                    message = msg,
                    isFromMe = msg.fromMe
                )
            }
        }

        // ===== INPUT BAR =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    if (it.isNotBlank()) {
                        scope.launch { repo.sendTyping(roomId) }
                    }
                },
                placeholder = { Text("Ketik pesan...", fontSize = 14.sp, color = t.textMuted) },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color(0xFF0A0A0A),
                    unfocusedBorderColor = Color(0xFF0A0A0A),
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
                        scope.launch {
                            repo.sendMessage(roomId, text)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Kirim",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isFromMe: Boolean
) {
    val t = LocalThemeTokens.current
    val shape = if (isFromMe)
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    else
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(if (isFromMe) t.accent else Color.White)
                .border(2.dp, Color(0xFF0A0A0A), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isFromMe) t.accentOn else Color(0xFF0A0A0A),
                    lineHeight = 19.sp
                )
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