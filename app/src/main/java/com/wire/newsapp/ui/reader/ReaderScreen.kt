package com.wire.newsapp.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wire.newsapp.data.ReaderBlock
import com.wire.newsapp.ui.components.SourceDot
import com.wire.newsapp.ui.openInBrowser
import com.wire.newsapp.ui.shareArticle
import com.wire.newsapp.util.fullDateTime
import com.wire.newsapp.util.hostOf
import com.wire.newsapp.util.timeAgo

/**
 * Immersive in-app reader — iOS Safari-Reader vibes: serif body, wide margins,
 * minimal chrome with back / share / bookmark / text-size controls.
 */
@Composable
fun ReaderScreen(
    link: String,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val article = state.article

    val bodyFontSize = when (state.fontScale) {
        0 -> 15.sp; 2 -> 20.sp; else -> 17.sp
    }
    val bodyLineHeight = when (state.fontScale) {
        0 -> 23.sp; 2 -> 31.sp; else -> 26.sp
    }
    val titleFontSize = when (state.fontScale) {
        0 -> 24.sp; 2 -> 33.sp; else -> 29.sp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReaderTopBar(
            sourceName = article?.sourceName.orEmpty(),
            isBookmarked = state.isBookmarked,
            onBack = onBack,
            onAa = viewModel::cycleFontScale,
            onShare = { article?.let { shareArticle(context, it) } },
            onToggleBookmark = viewModel::toggleBookmark
        )
        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = androidx.compose.ui.graphics.Color.Transparent
            )
        }

        when {
            state.error != null && state.content == null -> ReaderError(
                message = state.error!!,
                onRetry = viewModel::retry,
                onOpenOriginal = { openInBrowser(context, link) },
                onBack = onBack
            )

            state.content == null && state.isLoading -> ReaderLoading()

            else -> {
                val content = state.content!!
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 48.dp)
                ) {
                    // Source + timing row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SourceDot(article?.sourceName ?: "Wire")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = (article?.sourceName ?: hostOf(link)).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (article?.publishedAt != null && article.publishedAt > 0) {
                            Text(
                                text = "  ·  ${timeAgo(article.publishedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${content.readingMinutes} min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = article?.title?.takeIf { it.isNotBlank() } ?: content.title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFontSize,
                        lineHeight = titleFontSize * 1.18,
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    content.byline?.let { byline ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = byline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (article?.publishedAt != null && article.publishedAt > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fullDateTime(article.publishedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Hero image
                    val hero = content.heroImageUrl ?: article?.imageUrl
                    if (!hero.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(hero).crossfade(true).build(),
                            contentDescription = content.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(216.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(18.dp))

                    // Article body
                    content.blocks.forEach { block -> ReaderBlockView(block, bodyFontSize, bodyLineHeight) }

                    Spacer(Modifier.height(28.dp))
                    // End mark — a nod to print journalism
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Originally published at ${hostOf(link)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { openInBrowser(context, link) },
                        modifier = Modifier.align(Alignment.CenterHorizontally).height(44.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Read original article", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    sourceName: String,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onAa: () -> Unit,
    onShare: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = sourceName.ifBlank { "Reader" }.toString().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            TextButton(onClick = onAa, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(
                    text = "Aa",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.IosShare,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Toggle bookmark",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun ReaderBlockView(
    block: ReaderBlock,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    when (block) {
        is ReaderBlock.Paragraph ->
            Text(
                text = block.text,
                fontFamily = FontFamily.Serif,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        is ReaderBlock.Heading ->
            Text(
                text = block.text,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize + 4.sp,
                lineHeight = lineHeight + 4.sp,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
        is ReaderBlock.Quote ->
            Row(modifier = Modifier.padding(vertical = 10.dp)) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = block.text,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        is ReaderBlock.BlockImage -> Column(Modifier.padding(vertical = 10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(block.url).crossfade(true).build(),
                contentDescription = block.caption ?: "Article image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shadow(0.dp)
            )
            block.caption?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReaderLoading() {
    val brush = com.wire.newsapp.ui.components.shimmerBrush()
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Box(Modifier.fillMaxWidth(0.85f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        Box(Modifier.fillMaxWidth(0.6f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        Box(Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large).background(brush))
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        }
        Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(brush))
    }
}

@Composable
private fun ReaderError(
    message: String,
    onRetry: () -> Unit,
    onOpenOriginal: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Can't load this story",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(20.dp)) { Text("Back") }
            FilledTonalButton(onClick = onRetry, shape = RoundedCornerShape(20.dp)) { Text("Retry") }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onOpenOriginal) {
            Text("Open original in browser", color = MaterialTheme.colorScheme.primary)
        }
    }
}
