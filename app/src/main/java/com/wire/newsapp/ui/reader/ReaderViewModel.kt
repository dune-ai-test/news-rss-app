package com.wire.newsapp.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.data.ReaderBlock
import com.wire.newsapp.data.ReaderContent
import com.wire.newsapp.data.ReaderRepository
import com.wire.newsapp.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

data class ReaderUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val content: ReaderContent? = null,
    val error: String? = null,
    val isBookmarked: Boolean = false,
    /** 0 = Small, 1 = Medium, 2 = Large */
    val fontScale: Int = 1
) {
    val blocks: List<ReaderBlock> get() = content?.blocks.orEmpty()
}

class ReaderViewModel(
    private val link: String,
    private val repository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadMeta()
        extract()
    }

    private fun loadMeta() {
        viewModelScope.launch {
            val cached = repository.cachedArticle(link) ?: repository.bookmarkedArticle(link)
            val article = cached ?: Article(
                id = link,
                title = hostOf(link),
                link = link,
                description = "",
                sourceName = hostOf(link),
                category = "",
                imageUrl = null,
                publishedAt = 0L,
                author = null
            )
            _uiState.update { it.copy(article = article) }
            _uiState.update { it.copy(isBookmarked = repository.isBookmarked(link)) }
        }
    }

    private fun extract() {
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) { ReaderRepository.extract(link) }
                _uiState.update { it.copy(isLoading = false, content = content, error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't load the full article. Check your connection or open the original."
                    )
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        extract()
    }

    fun cycleFontScale() {
        _uiState.update { it.copy(fontScale = (it.fontScale + 1) % 3) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val article = _uiState.value.article ?: return@launch
            // Prefer the freshest meta we can find (cache may have been cleared).
            val full = repository.cachedArticle(link) ?: repository.bookmarkedArticle(link) ?: article
            repository.toggleBookmark(full)
            _uiState.update { it.copy(isBookmarked = repository.isBookmarked(link)) }
        }
    }

    private fun hostOf(url: String): String =
        runCatching { URI(url).host?.removePrefix("www.") ?: "Reader" }.getOrDefault("Reader")
}
