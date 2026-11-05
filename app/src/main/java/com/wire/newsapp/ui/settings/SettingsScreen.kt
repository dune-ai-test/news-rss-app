package com.wire.newsapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.newsapp.BuildConfig
import com.wire.newsapp.ui.components.GroupedCard
import com.wire.newsapp.ui.components.GroupedDivider
import com.wire.newsapp.ui.components.SectionHeader
import com.wire.newsapp.ui.components.SegmentedControl
import com.wire.newsapp.ui.openInBrowser
import kotlinx.coroutines.launch

private const val REPO_URL = "https://github.com/dune-ai-test/news-rss-app"

/**
 * iOS-style grouped settings: Appearance / Storage / About.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSources: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(Modifier.size(28.dp))
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
                        Text(text = "Settings", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Appearance, storage and about Wire",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Appearance section
                item { SectionHeader(title = "Appearance") }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard {
                            Column(Modifier.padding(12.dp)) {
                                SegmentedControl(
                                    options = listOf("System", "Light", "Dark"),
                                    selectedIndex = when (state.themeMode) {
                                        "light" -> 1
                                        "dark" -> 2
                                        else -> 0
                                    },
                                    onSelect = { index ->
                                        viewModel.setThemeMode(
                                            when (index) {
                                                1 -> "light"
                                                2 -> "dark"
                                                else -> "system"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Storage section
                item { SectionHeader(title = "Storage") }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard {
                            IconRow(
                                icon = Icons.Filled.DownloadForOffline,
                                tint = Color(0xFF007AFF),
                                title = "Cached stories",
                                trailing = state.cacheCount?.let { "$it items" } ?: "…"
                            )
                            GroupedDivider()
                            // iOS destructive row — centered red text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable { showClearDialog = true }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear Cached Articles",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // About section
                item { SectionHeader(title = "About") }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard {
                            IconRow(
                                icon = Icons.Filled.Info,
                                tint = Color(0xFF8E8E93),
                                title = "Version",
                                trailing = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                            )
                            GroupedDivider()
                            IconRow(
                                icon = Icons.Filled.RssFeed,
                                tint = Color(0xFFFF9500),
                                title = "Manage Sources",
                                chevron = true,
                                onClick = onOpenSources
                            )
                            GroupedDivider()
                            IconRow(
                                icon = Icons.Filled.Code,
                                tint = Color(0xFF1C1C1E),
                                title = "Source Code on GitHub",
                                chevron = true,
                                onClick = { openInBrowser(context, REPO_URL) }
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Wire",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "The whole web. One quiet feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear cached articles?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This removes the offline copy of stories. Your bookmarks and sources are not affected.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCache()
                    scope.launch { snackbarHostState.showSnackbar("Cache cleared") }
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/** iOS settings row with a colored icon tile. */
@Composable
private fun IconRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    trailing: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (chevron) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
