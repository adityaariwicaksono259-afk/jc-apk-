package app.javin.codex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateBanner(
    version: String,
    notes: String,
    downloading: Boolean = false,
    progress: Int = 0,
    onUpdateClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    // Pulse animation untuk badge dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x334A9EFF),
                        Color(0x227F5AF0)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(Color(0x664A9EFF), Color(0x447F5AF0))
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ==== ICON dengan PULSE DOT ====
        Box(
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x334A9EFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color(0xFF7FBFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Red dot pulse
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4D4D).copy(alpha = dotAlpha))
                    .border(2.dp, Color(0xFF1A1A2E), CircleShape)
            )
        }

        Spacer(Modifier.width(14.dp))

        // ==== TEXT + PROGRESS ====
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (downloading) "Mengunduh Update..." else "Update Tersedia",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (downloading) "$progress%"
                       else "v$version • ${notes.take(40)}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2
            )
            if (downloading) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4A9EFF),
                    trackColor = Color(0x334A9EFF)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // ==== BUTTON / DISMISS ====
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!downloading) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0))
                            )
                        )
                        .clickable(onClick = onUpdateClick)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Update",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
