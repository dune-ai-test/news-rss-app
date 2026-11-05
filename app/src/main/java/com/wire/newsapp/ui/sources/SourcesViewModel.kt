package com.wire.newsapp.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.newsapp.data.FeedCatalog
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.data.local.CustomFeed
import com.wire.newsapp.model.FeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

data class SourceSection(
    val category: String,
    val feeds: List<Pair<FeedSource, Boolean>>   // source to enabled
)

data class SourcesUiState(
    val sections: List<SourceSection> = emptyList(),
    val customFeeds: List<Pair<CustomFeed, Boolean>> = emptyList(),
    val activeCount: Int = 0,
    val totalCount: Int = 0
)

class SourcesViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeDisabledFeedIds(),
                repository.observeCustomFeeds()
            ) { disabled, custom ->
                buildState(disabled, custom)
            }.collect { state -> _uiState.update { state } }
        }
    }

    private fun buildState(disabled: Set<String>, custom: List<CustomFeed>): SourcesUiState {
        val groups = FeedCatalog.FEEDS
            .filter { it.category != FeedCatalog.ALL }
            .groupBy { it.category }

        val orderedCategories = FeedCatalog.CATEGORIES.filter { it != FeedCatalog.ALL }
        val sections = orderedCategories.mapNotNull { category ->
            groups[category]?.let { feeds ->
                SourceSection(
                    category = category,
                    feeds = feeds.map { it to (it.id !in disabled) }
                )
            }
        }

        val customPairs = custom.map { feed ->
            feed to ("custom:${feed.id}" !in disabled)
        }

        val builtInActive = sections.sumOf { s -> s.feeds.count { it.second } }
        val customActive = customPairs.count { it.second }
        val total = FeedCatalog.FEEDS.size + custom.size

        return SourcesUiState(
            sections = sections,
            customFeeds = customPairs,
            activeCount = builtInActive + customActive,
            totalCount = total
        )
    }

    fun setFeedEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setFeedEnabled(id, enabled) }
    }

    fun setCustomFeedEnabled(customId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setFeedEnabled("custom:$customId", enabled) }
    }

    fun addCustomFeed(name: String, url: String, category: String): String? {
        val trimmedName = name.trim()
        val trimmedUrl = url.trim()
        if (trimmedName.length < 2) return "Give your feed a name."
        val uri = runCatching { URI(trimmedUrl) }.getOrNull()
        val valid = uri != null &&
            (uri.scheme == "http" || uri.scheme == "https") &&
            !uri.host.isNullOrBlank() &&
            uri.host.contains(".")
        if (!valid) return "Enter a full URL, e.g. https://example.com/feed.xml"
        viewModelScope.launch { repository.addCustomFeed(trimmedName, trimmedUrl, category) }
        return null
    }

    fun removeCustomFeed(id: Long) {
        viewModelScope.launch { repository.removeCustomFeed(id) }
    }
}
