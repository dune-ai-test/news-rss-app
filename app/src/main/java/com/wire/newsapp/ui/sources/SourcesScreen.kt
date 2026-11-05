package com.wire.newsapp.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.newsapp.data.FeedCatalog
import com.wire.newsapp.ui.components.GroupedCard
import com.wire.newsapp.ui.components.GroupedDivider
import com.wire.newsapp.ui.components.SectionHeader
import com.wire.newsapp.util.sourceColor
import java.net.URI

/**
 * Manage built-in sources and add your own RSS/Atom feeds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(viewModel: SourcesViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

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
                    text = "Sources",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { showAddSheet = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add custom feed",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                    Text(text = "Sources", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${state.activeCount} of ${state.totalCount} active · grouped by desk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.sections.forEach { section ->
                item(key = "header_${section.category}") {
                    SectionHeader(
                        title = section.category,
                        trailing = "${section.feeds.count { it.second }}/${section.feeds.size} on"
                    )
                }
                item(key = "card_${section.category}") {
                    GroupedCard(Modifier.padding(horizontal = 16.dp)) {
                        section.feeds.forEachIndexed { index, (source, enabled) ->
                            SourceRow(
                                name = source.name,
                                url = source.url,
                                enabled = enabled,
                                onToggle = { viewModel.setFeedEnabled(source.id, it) }
                            )
                            if (index < section.feeds.size - 1) GroupedDivider()
                        }
                    }
                }
            }

            if (state.customFeeds.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Custom",
                        trailing = "${state.customFeeds.size} added"
                    )
                }
                item {
                    GroupedCard(Modifier.padding(horizontal = 16.dp)) {
                        state.customFeeds.forEachIndexed { index, (feed, enabled) ->
                            SourceRow(
                                name = feed.name,
                                url = feed.url,
                                enabled = enabled,
                                onToggle = { viewModel.setCustomFeedEnabled(feed.id, it) },
                                onDelete = { viewModel.removeCustomFeed(feed.id) }
                            )
                            if (index < state.customFeeds.size - 1) GroupedDivider()
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Disabled desks are skipped on refresh.\nCustom feeds are stored only on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (showAddSheet) {
        AddFeedSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, url, category ->
                val error = viewModel.addCustomFeed(name, url, category)
                if (error == null) showAddSheet = false
                error
            }
        )
    }
}

@Composable
private fun SourceRow(
    name: String,
    url: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(sourceColor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W",
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hostOf(url),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = iosSwitchColors()
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove feed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun iosSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = androidx.compose.ui.graphics.Color(0xFF34C759), // iOS systemGreen
    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
    checkedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

private fun hostOf(url: String): String =
    runCatching { URI(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFeedSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, category: String) -> String?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Add Custom Feed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Any RSS or Atom URL works",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Name") },
                placeholder = { Text("e.g. My Favourite Blog") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = { Text("Feed URL") },
                placeholder = { Text("https://example.com/feed.xml") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedCatalog.CATEGORIES.filter { it != FeedCatalog.ALL }.forEach { c ->
                    FilterChip(
                        selected = c == category,
                        onClick = { category = c },
                        label = { Text(c) }
                    )
                }
            }
            error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { error = onAdd(name, url, category) },
                    enabled = name.isNotBlank() && url.isNotBlank(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Add Feed", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
