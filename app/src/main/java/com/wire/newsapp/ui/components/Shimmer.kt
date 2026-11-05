package com.wire.newsapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Moving highlight brush used by skeleton loaders. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(translate.value, 0f),
        end = androidx.compose.ui.geometry.Offset(translate.value + 400f, 0f)
    )
}

@Composable
fun ArticleCardShimmer(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Box(Modifier.width(120.dp).height(10.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(0.92f).height(16.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.65f).height(16.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(80.dp).height(10.dp).clip(CircleShape).background(brush))
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(104.dp).clip(MaterialTheme.shapes.medium).background(brush))
        }
    }
}
