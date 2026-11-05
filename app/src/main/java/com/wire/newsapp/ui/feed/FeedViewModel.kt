package com.wire.newsapp.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.newsapp.data.FeedCatalog
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val articles: List<Article> = emptyList(),
    val bookmarkedIds: Set<String> = emptySet(),
    val selectedCategory: String = FeedCatalog.ALL,
    val visibleCount: Int = PAGE_SIZE,
    val offline: Boolean = false,
    val fetchedFeeds: Int = 0,
    val failedFeeds: List<String> = emptyList()
) {
    val filtered: List<Article>
        get() = if (selectedCategory == FeedCatalog.ALL) articles
        else articles.filter { it.category == selectedCategory }

    val visible: List<Article> get() = filtered.take(visibleCount)
    val hasMore: Boolean get() = filtered.size > visibleCount
    val totalCount: Int get() = filtered.size

    companion object { const val PAGE_SIZE = 12 }
}

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        observeBookmarks()
        seedFromCacheThenRefresh()
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            repository.observeBookmarkedIds().collect { ids ->
                _uiState.update { it.copy(bookmarkedIds = ids) }
            }
        }
    }

    private fun seedFromCacheThenRefresh() {
        viewModelScope.launch {
            // Show the cache instantly, then hit the network.
            val cached = repository.observeCachedArticles().first()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(articles = cached, isLoading = false) }
            }
            doRefresh()
        }
    }

    fun refresh() {
        viewModelScope.launch { doRefresh() }
    }

    private suspend fun doRefresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        try {
            val result = repository.refresh()
            _uiState.update {
                it.copy(
                    articles = result.articles,
                    isLoading = false,
                    isRefreshing = false,
                    offline = result.fromCache,
                    fetchedFeeds = result.fetchedFeeds,
                    failedFeeds = result.failedFeeds,
                    visibleCount = maxOf(it.visibleCount, FeedUiState.PAGE_SIZE)
                )
            }
        } catch (e: Exception) {
            val cached = runCatching { repository.observeCachedArticles().first() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    articles = cached.ifEmpty { it.articles },
                    isLoading = false,
                    isRefreshing = false,
                    offline = true
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, visibleCount = FeedUiState.PAGE_SIZE) }
    }

    /** Infinite scroll: reveal the next page of already-fetched articles. */
    fun loadMore() {
        _uiState.update {
            if (it.hasMore) it.copy(visibleCount = it.visibleCount + FeedUiState.PAGE_SIZE) else it
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch { repository.toggleBookmark(article) }
    }
}
