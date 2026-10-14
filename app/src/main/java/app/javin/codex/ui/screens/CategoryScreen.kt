package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.ui.components.getToolIconUrl
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    categoryName: String,
    onBack: () -> Unit = {},
    onToolClick: (Tool) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }

    var tools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(categoryName) {
        loading = true
        errorMsg = null
        val result = repo.getToolsByFolder(categoryName)
        loading = false
        result.onSuccess { tools = it }
            .onFailure { errorMsg = it.message ?: "Gagal load" }
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
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
                    .background(Color(0x1AFFFFFF))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = categoryName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${tools.size} tools",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // ============================================
        // SEARCH BAR
        // ============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari di $categoryName...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x0FFFFFFF),
                    unfocusedContainerColor = Color(0x0FFFFFFF),
                    focusedBorderColor = Color(0x1AFFFFFF),
                    unfocusedBorderColor = Color(0x1AFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF4A9EFF)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // ============================================
        // CONTENT
        // ============================================
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A9EFF), strokeWidth = 3.dp)
                }
            }

            errorMsg != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    errorMsg = null
                                    repo.getTools(true).onSuccess {
                                        tools = it.filter { t -> t.displayFolder.equals(categoryName, true) }
                                    }.onFailure { e -> errorMsg = e.message }
                                    loading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x334A9EFF))
                        ) { Text("Coba Lagi", color = Color.White) }
                    }
                }
            }

            visibleTools.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isBlank()) "Belum ada tool di $categoryName"
                        else "Nggak ketemu: \"$searchQuery\"",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    items(visibleTools, key = { it.catalogId ?: it.displayName }) { tool ->
                        ToolRow(
                            name = tool.displayName,
                            desc = tool.displayDesc,
                            tag = getToolTag(tool.displayName),
                            iconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                            onClick = { onToolClick(tool) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 66.dp)
                                .height(1.dp)
                                .background(Color(0x0FFFFFFF))
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

// Tag otomatis per tool
private fun getToolTag(name: String): String = when {
    name.contains("stalker", true) || name.contains("profile", true) -> "Profile HD"
    name.contains("downloader", true) || name.contains("download", true) -> "No Watermark"
    name.contains("v2", true) -> "V2"
    name.contains("multi", true) || name.contains("3-in-1", true) -> "3-in-1"
    else -> ""
}

@Composable
private fun ToolRow(
    name: String,
    desc: String,
    tag: String,
    iconUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (!iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tag.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            if (desc.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0x66FFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}
