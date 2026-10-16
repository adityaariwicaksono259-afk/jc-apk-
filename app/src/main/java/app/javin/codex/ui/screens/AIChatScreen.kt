package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.platform.LocalContext
import app.javin.codex.data.session.ChatHistoryManager
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.data.session.ChatHistoryItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AIChatScreen(
    toolName: String,
    toolIconUrl: String?,
    catalogId: String?,
    onBack: () -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val appContext = LocalContext.current
    val chatHistory = remember { ChatHistoryManager.getInstance(appContext) }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Load history dari DataStore
    LaunchedEffect(catalogId) {
        if (catalogId.isNullOrBlank()) {
            messages = emptyList()
            return@LaunchedEffect
        }
        val saved = chatHistory.load(catalogId)
        messages = saved.map { ChatMessage(it.text, it.fromUser, it.timestamp) }
    }

    // Auto-save tiap ada perubahan pesan
    LaunchedEffect(messages) {
        if (!catalogId.isNullOrBlank() && messages.isNotEmpty()) {
            chatHistory.save(catalogId, messages.map {
                ChatHistoryItem(it.text, it.fromUser, it.timestamp)
            })
        }
    }

    // Auto-scroll ke bawah
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty() || isTyping || catalogId.isNullOrBlank()) return

        // Add user message
        messages = messages + ChatMessage(text = text, fromUser = true)
        inputText = ""
        isTyping = true

        // Panggil backend proxy-ext
        scope.launch {
            val repo = app.javin.codex.data.repository.ToolRepository(app.javin.codex.data.api.ApiClient.get())
            val result = repo.callTool(catalogId, text)
            result.onSuccess { reply ->
                messages = messages + ChatMessage(text = reply, fromUser = false)
            }.onFailure { err ->
                messages = messages + ChatMessage(
                    text = "❌ Error: ${err.message ?: "Gagal memanggil AI"}",
                    fromUser = false
                )
            }
            isTyping = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ============================================
        // HEADER
        // ============================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .border(1.dp, t.cardBorder, CircleShape)
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

            // Tool icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(t.iconBg)
                    .border(1.dp, t.cardBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!toolIconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = toolIconUrl,
                        contentDescription = toolName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(toolName.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = t.textPrimary)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toolName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.textPrimary
                )
                Text(
                    text = if (isTyping) "Mengetik..." else "Online",
                    fontSize = 11.sp,
                    color = if (isTyping) Color(0xFF7FFFA0) else t.textPrimary
                )
            }

            // Tombol Clear chat
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        scope.launch {
                            if (!catalogId.isNullOrBlank()) {
                                chatHistory.clear(catalogId)
                            }
                            messages = emptyList()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear chat",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = t.iconBg)

        // ============================================
        // CHAT LIST
        // ============================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (isTyping) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                .background(t.iconBg)
                                .border(1.dp, t.cardBorder, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("● ● ●", fontSize = 12.sp, color = t.textPrimary)
                        }
                    }
                }
            }
        }

        // ============================================
        // INPUT BAR
        // ============================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Ketik pesan...", fontSize = 14.sp, color = t.textPrimary)
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = t.iconBg,
                    unfocusedContainerColor = t.iconBg,
                    focusedBorderColor = t.cardBorder,
                    unfocusedBorderColor = t.cardBorder,
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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(t.accent, Color(0xFF7F5AF0))
                        )
                    )
                    .clickable(enabled = inputText.isNotBlank() && !isTyping) {
                        sendMessage()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val t = LocalThemeTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (message.fromUser)
                        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                    else
                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .background(
                    if (message.fromUser)
                        Brush.linearGradient(
                            colors = listOf(t.accent, Color(0xFF7F5AF0))
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(t.iconBg, t.cardBg)
                        )
                )
                .then(
                    if (!message.fromUser)
                        Modifier.border(1.dp, t.cardBorder, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = t.textPrimary,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatChatTime(message.timestamp),
                    fontSize = 10.sp,
                    color = t.textPrimary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatChatTime(ts: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(ts))
    } catch (e: Exception) {
        ""
    }
}
