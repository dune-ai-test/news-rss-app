package com.wire.newsapp.model

/**
 * A source of articles: either a curated built-in feed or a user-added custom one.
 * Built-in ids are stable slugs; custom ids look like "custom:12".
 */
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val isCustom: Boolean = false
)
