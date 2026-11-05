package com.wire.newsapp.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.newsapp.data.FeedCatalog
import com.wire.newsapp.model.Article
import com.wire.newsapp.ui.components.*
import com.wire.newsapp.ui.shareArticle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onOpenArticle: (Article) -> Unit,
    onOpenSources: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // iOS behaviour: nav-bar title fades in once the large title scrolls away.
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    val navTitleAlpha by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(200),
        label = "navTitle"
    )

    // Infinite scroll: preload the next page when the 4th-from-last item appears.
    val lastVisibleIndex by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(lastVisibleIndex) {
        if (state.hasMore && lastVisibleIndex >= state.visible.size - 4) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IOSNavigationBar(
            titleAlpha = navTitleAlpha,
            refreshing = state.isRefreshing,
            onRefresh = viewModel::refresh
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Large title block (scrolls away)
                item(key = "large_title") {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
                        Text(
                            text = "Wire",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = largeTitleSubtitle(state.totalCount, state.fetchedFeeds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Category chips — sticky like a pinned iOS section header
                stickyHeader(key = "chips") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 6.dp)
                    ) {
                        CategoryChipRow(
                            categories = FeedCatalog.CATEGORIES,
                            selected = state.selectedCategory,
                            onSelect = viewModel::selectCategory
                        )
                    }
                }

                if (state.offline) {
                    item(key = "offline_banner") { OfflineBanner() }
                }

                if (state.isLoading && state.articles.isEmpty()) {
                    items(6) { ArticleCardShimmer(Modifier.padding(horizontal = 16.dp)) }
                } else if (state.visible.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            icon = Icons.Filled.WifiOff,
                            title = "Nothing here yet",
                            message = "No stories in this section right now. Check your connection or enable more sources.",
                            actionLabel = "Manage Sources",
                            onAction = onOpenSources
                        )
                    }
                } else {
                    itemsIndexed(
                        items = state.visible,
                        key = { _, article -> article.link }
                    ) { index, article ->
                        ArticleCard(
                            article = article,
                            isBookmarked = article.link in state.bookmarkedIds,
                            onOpen = onOpenArticle,
                            onToggleBookmark = viewModel::toggleBookmark,
                            onShare = { shareArticle(context, it) },
                            featured = index == 0 && state.selectedCategory == FeedCatalog.ALL,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item(key = "footer") {
                        FeedFooter(hasMore = state.hasMore)
                    }
                }
            }
        }
    }
}

private fun largeTitleSubtitle(total: Int, fetchedFeeds: Int): String {
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    return if (fetchedFeeds > 0) "$date  ·  $total stories from $fetchedFeeds sources"
    else date
}

@Composable
private fun IOSNavigationBar(
    titleAlpha: Float,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (refreshing) 360f else 0f,
        animationSpec = tween(700),
        label = "refreshSpin"
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App glyph — blue rounded square with white W (matches launcher icon)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Wire",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(titleAlpha)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
        // Hairline separator — iOS navigation bars sit on a 0.5pt rule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "You're offline — showing cached stories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LazyItemScope.FeedFooter(hasMore: Boolean) {
    if (hasMore) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ArticleCardShimmer()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You're all caught up",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pull down to refresh your feed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
