package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.ui.components.ToolCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    userName: String? = null,
    onLogout: () -> Unit = {},
    onToolClick: (Tool) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }

    var tools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch tools (khusus AI dulu)
    LaunchedEffect(Unit) {
        loading = true
        errorMsg = null
        val result = repo.getTools()
        loading = false
        result.onSuccess { allTools ->
            // FILTER: cuma kategori AI dulu
            tools = allTools.filter { it.displayFolder.equals("AI", ignoreCase = true) }
        }.onFailure { e ->
            errorMsg = e.message ?: "Gagal load tools"
        }
    }

    // Filter by search
    val visibleTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) tools
        else {
            val q = searchQuery.lowercase()
            tools.filter {
                it.displayName.lowercase().contains(q) ||
                it.displayDesc.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1E8))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ============================================
            // HEADER
            // ============================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE600))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hai, ${userName ?: "Sobat"} 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0A0A0A)
                        )
                        Text(
                            text = "Mau ngapain hari ini?",
                            fontSize = 12.sp,
                            color = Color(0xFF4A4A4A)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari tool AI...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0A0A0A),
                        unfocusedBorderColor = Color(0xFF0A0A0A),
                        focusedTextColor = Color(0xFF0A0A0A),
                        unfocusedTextColor = Color(0xFF0A0A0A),
                        cursorColor = Color(0xFF0A0A0A)
                    )
                )
            }

            // ============================================
            // CONTENT
            // ============================================
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFF7FE5))
                            Spacer(Modifier.height(12.dp))
                            Text("Memuat tools...", color = Color(0xFF4A4A4A), fontSize = 13.sp)
                        }
                    }
                }

                errorMsg != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                errorMsg!!,
                                color = Color(0xFFCC0000),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        errorMsg = null
                                        repo.getTools(true).onSuccess {
                                            tools = it.filter { t -> t.displayFolder.equals("AI", true) }
                                        }.onFailure { e -> errorMsg = e.message }
                                        loading = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7FE5))
                            ) {
                                Text("Coba Lagi", color = Color(0xFF0A0A0A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                visibleTools.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "Belum ada tool AI"
                                else "Nggak ketemu: \"$searchQuery\"",
                                color = Color(0xFF4A4A4A),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section title
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .background(Color(0xFFFF7FE5))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "FITUR AI",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0A0A0A),
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "(${visibleTools.size})",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6A6A6A)
                                )
                            }
                        }

                        // List
                        items(visibleTools, key = { it.catalogId ?: it.displayName }) { tool ->
                            ToolCard(
                                name = tool.displayName,
                                desc = tool.displayDesc,
                                folder = tool.displayFolder,
                                onClick = { onToolClick(tool) }
                            )
                        }

                        // Logout button di bawah
                        item {
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFCC0000))
                            ) {
                                Text(
                                    "Logout",
                                    color = Color(0xFFCC0000),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}
