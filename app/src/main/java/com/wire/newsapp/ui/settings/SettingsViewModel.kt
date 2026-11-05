package com.wire.newsapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val cacheCount: Int? = null
)

class SettingsViewModel(
    private val repository: FeedRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        refreshCacheCount()
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun refreshCacheCount() {
        viewModelScope.launch {
            runCatching { repository.cacheCount() }
                .onSuccess { count -> _uiState.update { it.copy(cacheCount = count) } }
        }
    }

    /** Clears the article cache only — bookmarks are never touched. */
    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.update { it.copy(cacheCount = 0) }
        }
    }
}
