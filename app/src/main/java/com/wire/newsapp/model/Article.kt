package com.wire.newsapp.model

/**
 * A single news article, normalized across RSS 2.0 / Atom feeds.
 * [id] is the article link and is used as the stable primary key everywhere.
 */
data class Article(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val sourceName: String,
    val category: String,
    val imageUrl: String?,
    val publishedAt: Long,   // epoch millis, 0 when unknown
    val author: String?
)
