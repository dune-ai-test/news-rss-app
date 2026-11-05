package com.wire.newsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wire.newsapp.model.Article
import com.wire.newsapp.util.sourceColor
import com.wire.newsapp.util.timeAgo

/** Small colored initial dot — iOS source avatar. */
@Composable
fun SourceDot(name: String, size: androidx.compose.ui.unit.Dp = 10.dp) {
    val initial = name.trim().firstOrNull()?.uppercaseChar() ?: 'W'
    Box(
        modifier = Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(sourceColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            fontSize = (size.value * 0.55).sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
fun SourceLine(article: Article, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SourceDot(article.sourceName)
        Spacer(Modifier.width(6.dp))
        Text(
            text = article.sourceName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val time = timeAgo(article.publishedAt)
        if (time.isNotEmpty()) {
            Text(
                text = "  ·  $time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * iOS-News style story card. When [featured] is true, the image spans full width on top.
 */
@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onOpen: (Article) -> Unit,
    onToggleBookmark: (Article) -> Unit,
    onShare: (Article) -> Unit,
    featured: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable { onOpen(article) },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp
    ) {
        if (featured) {
            Column {
                if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(article.imageUrl).crossfade(true).build(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    SourceLine(article)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (article.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = article.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ActionRow(article, isBookmarked, onToggleBookmark, onShare)
                }
            }
        } else {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SourceLine(article)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (article.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = article.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ActionRow(article, isBookmarked, onToggleBookmark, onShare)
                }
                if (!article.imageUrl.isNullOrBlank()) {
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(article.imageUrl).crossfade(true).build(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    article: Article,
    isBookmarked: Boolean,
    onToggleBookmark: (Article) -> Unit,
    onShare: (Article) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = article.category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { onShare(article) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.IosShare,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(
            onClick = { onToggleBookmark(article) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Save for later",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
