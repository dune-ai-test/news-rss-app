package com.wire.newsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wire.newsapp.model.Article
import com.wire.newsapp.util.handleOf
import com.wire.newsapp.util.sourceColor
import com.wire.newsapp.util.timeAgo

/** Twitter/X blue — used for verified badges. */
private val VerifiedBlue = Color(0xFF1D9BF0)

/** Small colored initial avatar — doubles as the "profile pic". */
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
            fontSize = (size.value * 0.48).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
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
 * Twitter/X-style feed item: big round avatar, verified source, bold headline,
 * gray preview text, full-width rounded media when present, hairline divider.
 * No image → the tweet is pure text. No fake counts, no wasted space.
 */
@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onOpen: (Article) -> Unit,
    onToggleBookmark: (Article) -> Unit,
    onShare: (Article) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen(article) }
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Avatar column
            SourceDot(article.sourceName, size = 42.dp)
            Spacer(Modifier.width(12.dp))

            // Content column
            Column(modifier = Modifier.weight(1f)) {
                // Header: Source ✓ @handle · 3h
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.sourceName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = "Verified source",
                        tint = VerifiedBlue,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = handleOf(article.link),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val time = timeAgo(article.publishedAt)
                    if (time.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "· $time",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))
                // Headline — the tweet's text
                Text(
                    text = article.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.1).sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (article.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = article.description,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Media — only when the story has one
                if (!article.imageUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .clip(MaterialTheme.shapes.large)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    )
                }

                Spacer(Modifier.height(6.dp))
                // Action row — share / bookmark
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onShare(article) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Filled.IosShare,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = { onToggleBookmark(article) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove from Saved" else "Save for later",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = article.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Hairline divider between items, full-bleed like the X timeline
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
        )
    }
}
