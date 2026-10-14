package app.javin.codex.ui.components

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.ui.theme.LocalThemeTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceInfo(
    val time: String,
    val battery: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val deviceModel: String
)

@Composable
fun rememberDeviceInfo(): State<DeviceInfo> {
    val context = LocalContext.current
    val info = remember { mutableStateOf(readDeviceInfo(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            info.value = readDeviceInfo(context)
            delay(5000)
        }
    }
    return info
}

fun readDeviceInfo(context: Context): DeviceInfo {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFmt.format(Date())

    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val totalMb = memInfo.totalMem / (1024 * 1024)
    val availMb = memInfo.availMem / (1024 * 1024)
    val usedMb = totalMb - availMb

    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val deviceModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model"
    }

    return DeviceInfo(
        time = time,
        battery = batteryPct,
        ramUsedMb = usedMb,
        ramTotalMb = totalMb,
        deviceModel = deviceModel
    )
}

@Composable
fun DeviceStatsHeader() {
    val info by rememberDeviceInfo()
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(if (t.isNeo) 14.dp else 20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (t.isNeo) Brush.linearGradient(listOf(t.cardBg, t.cardBg))
                else Brush.linearGradient(listOf(Color(0x1FFFFFFF), Color(0x0DFFFFFF)))
            )
            .border(
                if (t.isNeo) 2.dp else 1.dp,
                if (t.isNeo) t.cardBorder else Color(0x26FFFFFF),
                shape
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.time,
                fontSize = 32.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Text(
                text = info.deviceModel,
                fontSize = 12.sp,
                color = t.textMuted,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                icon = Icons.Default.BatteryFull,
                label = "Baterai",
                value = "${info.battery}%",
                modifier = Modifier.weight(1f)
            )
            StatChip(
                icon = Icons.Default.Memory,
                label = "RAM",
                value = "${info.ramUsedMb} / ${info.ramTotalMb} MB",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (t.isNeo) t.accentSoft else Color(0x14FFFFFF))
            .border(
                if (t.isNeo) 1.5.dp else 1.dp,
                if (t.isNeo) t.cardBorder else Color(0x1AFFFFFF),
                shape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (t.isNeo) t.iconTint else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = t.textMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 11.sp,
                color = t.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
