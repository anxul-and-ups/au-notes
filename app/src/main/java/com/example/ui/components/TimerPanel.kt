package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AlarmScheduler
import com.example.ui.util.AlarmSoundPlayer
import kotlinx.coroutines.delay

/**
 * Easy countdown timer: pick a duration with one tap (or the +/- steppers), press Start.
 * It is a real exact alarm underneath, so it rings with the app closed. Start / Pause / Resume / Reset / Stop.
 * State is saved, so the timer keeps running when you close this window or the app.
 */
@Composable
fun TimerPanel(isDarkMode: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("au_timer", Context.MODE_PRIVATE) }
    val fg = if (isDarkMode) Color.White else Color(0xFF18181B)
    val soft = if (isDarkMode) Color(0x18FFFFFF) else Color(0x0F000000)

    var total by remember { mutableStateOf(prefs.getLong("total", 5 * 60_000L).coerceAtLeast(1_000L)) }
    var endAt by remember { mutableStateOf(prefs.getLong("end_at", 0L)) }
    var remaining by remember { mutableStateOf(prefs.getLong("remaining", 0L)) }
    var state by remember {
        val saved = prefs.getString("state", "idle") ?: "idle"
        mutableStateOf(if (saved == "running" && System.currentTimeMillis() >= prefs.getLong("end_at", 0L)) "done" else saved)
    }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    fun save() {
        prefs.edit().putString("state", state).putLong("end_at", endAt).putLong("remaining", remaining).putLong("total", total).apply()
    }

    LaunchedEffect(state) {
        while (state == "running") {
            now = System.currentTimeMillis()
            if (now >= endAt) { state = "done"; save() }
            delay(200)
        }
    }

    fun start(ms: Long) {
        endAt = System.currentTimeMillis() + ms
        AlarmScheduler.scheduleAlarm(context, endAt, "⏱ Timer finished", 0L, null)
        state = "running"; save()
    }
    fun pause() {
        remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
        AlarmScheduler.cancelAlarm(context, endAt, 0L)
        state = "paused"; save()
    }
    fun reset() {
        if (state == "running") AlarmScheduler.cancelAlarm(context, endAt, 0L)
        AlarmSoundPlayer.stop()
        endAt = 0L; remaining = 0L; state = "idle"; save()
    }
    fun setTotal(ms: Long) { total = ms.coerceIn(1_000L, 99L * 3_600_000L + 59L * 60_000L + 59_000L); save() }

    val leftMs = when (state) {
        "running" -> (endAt - now).coerceAtLeast(0L)
        "paused" -> remaining
        "done" -> 0L
        else -> total
    }
    val progress = if (state == "idle" || total <= 0L) 1f else (leftMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val secs = (leftMs + 999L) / 1000L
    val clock = if (secs >= 3600L) "%d:%02d:%02d".format(secs / 3600, (secs % 3600) / 60, secs % 60) else "%02d:%02d".format(secs / 60, secs % 60)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // countdown ring
        Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = fg.copy(alpha = 0.10f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke)
                )
                drawArc(
                    color = if (state == "done") Color(0xFFEF4444) else CrimsonPrimary,
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clock, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (state == "done") Color(0xFFEF4444) else fg)
                Text(
                    when (state) { "running" -> "Running"; "paused" -> "Paused"; "done" -> "Time's up!"; else -> "Ready" },
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (state == "idle") {
            // quick presets: one tap sets the duration
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("1m" to 1, "5m" to 5, "10m" to 10, "15m" to 15, "30m" to 30, "1h" to 60).forEach { (label, min) ->
                    val on = total == min * 60_000L
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) CrimsonPrimary.copy(alpha = 0.18f) else soft)
                            .border(1.dp, if (on) CrimsonPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { setTotal(min * 60_000L) },
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) CrimsonPrimary else fg) }
                }
            }
            Spacer(Modifier.height(8.dp))
            // fine tuning
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("-1 min" to -60_000L, "+10 sec" to 10_000L, "+1 min" to 60_000L, "+5 min" to 300_000L).forEach { (label, delta) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(soft)
                            .clickable { setTotal(total + delta) },
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg, textAlign = TextAlign.Center) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // main controls
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            when (state) {
                "idle" -> TimerBtn("Start", true, Modifier.weight(1f)) { start(total) }
                "running" -> {
                    TimerBtn("Pause", true, Modifier.weight(1f)) { pause() }
                    TimerBtn("Reset", false, Modifier.weight(1f)) { reset() }
                }
                "paused" -> {
                    TimerBtn("Resume", true, Modifier.weight(1f)) { start(remaining) }
                    TimerBtn("Reset", false, Modifier.weight(1f)) { reset() }
                }
                else -> {
                    TimerBtn("Stop alarm", true, Modifier.weight(1f)) { reset() }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when (state) {
                "running" -> "Keeps counting even if you close this window. It rings when the time is up."
                "done" -> "Tap Stop alarm to silence it."
                else -> "Pick a time, then tap Start."
            },
            fontSize = 11.sp, color = fg.copy(alpha = 0.55f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TimerBtn(label: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) CrimsonPrimary else Color(0x1F888888))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (primary) Color.White else Color(0xFFEF4444))
    }
}
