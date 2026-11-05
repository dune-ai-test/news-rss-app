package com.wire.newsapp.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarksUiState(
    val bookmarks: List<Article> = emptyList(),
    val loading: Boolean = true
)

class BookmarksViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    /** Recently removed item, kept so the snackbar can undo. */
    private var lastRemoved: Article? = null

    init {
        viewModelScope.launch {
            repository.observeBookmarks().collect { list ->
                _uiState.update { it.copy(bookmarks = list, loading = false) }
            }
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch { repository.toggleBookmark(article) }
    }

    fun remove(article: Article) {
        lastRemoved = article
        viewModelScope.launch { repository.removeBookmark(article.link) }
    }

    /** Re-insert the last removed bookmark. Returns it for the snackbar message. */
    fun undoRemove() {
        val article = lastRemoved ?: return
        viewModelScope.launch { repository.toggleBookmark(article) }
        lastRemoved = null
    }
}
