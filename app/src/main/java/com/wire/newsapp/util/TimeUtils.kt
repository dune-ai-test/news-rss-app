package com.wire.newsapp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** "Just now" / "12m" / "3h" / "2d" — iOS-newsfeed style relative time. */
fun timeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/** "Thu, Jun 12 · 09:41" for the reader header. */
fun fullDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(timestamp))
}

/** Deterministic iOS-system color per source, used for the source dot. */
fun sourceColor(name: String): androidx.compose.ui.graphics.Color {
    val palette = listOf(
        androidx.compose.ui.graphics.Color(0xFF007AFF), // systemBlue
        androidx.compose.ui.graphics.Color(0xFFFF9500), // systemOrange
        androidx.compose.ui.graphics.Color(0xFF34C759), // systemGreen
        androidx.compose.ui.graphics.Color(0xFFFF3B30), // systemRed
        androidx.compose.ui.graphics.Color(0xFFAF52DE), // systemPurple
        androidx.compose.ui.graphics.Color(0xFFFF2D55), // systemPink
        androidx.compose.ui.graphics.Color(0xFF5856D6), // systemIndigo
        androidx.compose.ui.graphics.Color(0xFF5AC8FA), // systemTeal-ish
        androidx.compose.ui.graphics.Color(0xFFFFCC00), // systemYellow
        androidx.compose.ui.graphics.Color(0xFF64D2FF)  // systemCyan (dark)
    )
    return palette[Math.floorMod(name.hashCode(), palette.size)]
}

fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)
