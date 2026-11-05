package com.wire.newsapp

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wire.newsapp.ui.bookmarks.BookmarksScreen
import com.wire.newsapp.ui.bookmarks.BookmarksViewModel
import com.wire.newsapp.ui.feed.FeedScreen
import com.wire.newsapp.ui.feed.FeedViewModel
import com.wire.newsapp.ui.reader.ReaderScreen
import com.wire.newsapp.ui.reader.ReaderViewModel
import com.wire.newsapp.ui.settings.SettingsScreen
import com.wire.newsapp.ui.settings.SettingsViewModel
import com.wire.newsapp.ui.sources.SourcesScreen
import com.wire.newsapp.ui.sources.SourcesViewModel
import com.wire.newsapp.ui.theme.WireTheme

object Routes {
    const val FEED = "feed"
    const val SAVED = "saved"
    const val SOURCES = "sources"
    const val SETTINGS = "settings"
    const val READER = "reader"
    fun readerRoute(link: String) = "reader?link=" + android.net.Uri.encode(link)
}

@Composable
fun WireApp() {
    val application = LocalContext.current.applicationContext as WireApplication
    val container = application.container
    val themeMode by container.settingsStore.themeMode.collectAsState(initial = "system")

    WireTheme(themeMode = themeMode) {
        val darkTheme = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
        // Status/navigation bar icons contrast
        val view = LocalView.current
        val window = LocalActivity.current?.window
        LaunchedEffect(darkTheme) {
            window?.let {
                WindowInsetsControllerCompat(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }

        val navController = rememberNavController()
        val bookmarkCount by container.feedRepository.observeBookmarkCount()
            .collectAsState(initial = 0)
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val tabRoutes = listOf(Routes.FEED, Routes.SAVED, Routes.SOURCES, Routes.SETTINGS)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (currentRoute != null && currentRoute in tabRoutes) {
                    WireBottomBar(
                        currentRoute = currentRoute,
                        bookmarkCount = bookmarkCount,
                        onTabSelected = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Routes.FEED) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.FEED,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.FEED) {
                    val vm: FeedViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { FeedViewModel(container.feedRepository) }
                        }
                    )
                    FeedScreen(
                        viewModel = vm,
                        onOpenArticle = { article ->
                            navController.navigate(Routes.readerRoute(article.link))
                        },
                        onOpenSources = { navController.navigate(Routes.SOURCES) }
                    )
                }
                composable(Routes.SAVED) {
                    val vm: BookmarksViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { BookmarksViewModel(container.feedRepository) }
                        }
                    )
                    BookmarksScreen(
                        viewModel = vm,
                        onOpenArticle = { article ->
                            navController.navigate(Routes.readerRoute(article.link))
                        }
                    )
                }
                composable(Routes.SOURCES) {
                    val vm: SourcesViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { SourcesViewModel(container.feedRepository) }
                        }
                    )
                    SourcesScreen(viewModel = vm)
                }
                composable(Routes.SETTINGS) {
                    val vm: SettingsViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { SettingsViewModel(container.feedRepository, container.settingsStore) }
                        }
                    )
                    SettingsScreen(
                        viewModel = vm,
                        onOpenSources = { navController.navigate(Routes.SOURCES) }
                    )
                }
                composable(
                    route = "${Routes.READER}?link={link}",
                    arguments = listOf(navArgument("link") { type = NavType.StringType })
                ) { backStackEntry ->
                    val encoded = backStackEntry.arguments?.getString("link").orEmpty()
                    val link = android.net.Uri.decode(encoded)
                    val vm: ReaderViewModel = viewModel(
                        key = "reader_${link.hashCode()}",
                        factory = viewModelFactory {
                            initializer { ReaderViewModel(link, container.feedRepository) }
                        }
                    )
                    ReaderScreen(
                        link = link,
                        viewModel = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private data class TabSpec(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val TABS = listOf(
    TabSpec(Routes.FEED, "Today", Icons.Filled.Newspaper),
    TabSpec(Routes.SAVED, "Saved", Icons.Filled.Bookmark),
    TabSpec(Routes.SOURCES, "Sources", Icons.Filled.RssFeed),
    TabSpec(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

/** iOS-style tab bar: translucent, hairline rule on top, no selection pill. */
@Composable
private fun WireBottomBar(
    currentRoute: String?,
    bookmarkCount: Int,
    onTabSelected: (String) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 0.dp
        ) {
            TABS.forEach { tab ->
                val selected = tab.route == currentRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab.route) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    icon = {
                        if (tab.route == Routes.SAVED && bookmarkCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFFF3B30),
                                        contentColor = Color.White
                                    ) { Text("$bookmarkCount") }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        } else {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    },
                    label = {
                        Text(tab.label, style = MaterialTheme.typography.labelSmall)
                    }
                )
            }
        }
    }
}
