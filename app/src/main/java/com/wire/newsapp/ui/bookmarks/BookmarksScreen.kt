package com.wire.newsapp.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.wire.newsapp.model.Article
import com.wire.newsapp.ui.components.ArticleCard
import com.wire.newsapp.ui.components.EmptyState
import com.wire.newsapp.ui.shareArticle

/**
 * Saved stories — readable offline. Swipe left on a card to remove it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onOpenArticle: (Article) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(28.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (state.bookmarks.isNotEmpty()) "${state.bookmarks.size}" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Box(
            Modifier.fillMaxWidth().height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        if (state.bookmarks.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Filled.Bookmark,
                title = "No saved stories",
                message = "Tap the bookmark icon on any story and it will be waiting for you here — even offline.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = "Read later, fully offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(state.bookmarks, key = { it.link }) { article ->
                    SwipeToDismissRow(
                        article = article,
                        onRemove = {
                            viewModel.remove(article)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed from Saved",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoRemove()
                                }
                            }
                        }
                    ) {
                        ArticleCard(
                            article = article,
                            isBookmarked = true,
                            onOpen = onOpenArticle,
                            onToggleBookmark = viewModel::toggleBookmark,
                            onShare = { shareArticle(context, it) },
                            modifier = Modifier
                        )
                    }
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissRow(
    article: Article,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }
    ) {
        Box(Modifier.padding(horizontal = 16.dp)) { content() }
    }
}
