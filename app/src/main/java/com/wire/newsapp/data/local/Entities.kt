package com.wire.newsapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wire.newsapp.model.Article

/** Cached copy of a fetched article (drives offline reading). */
@Entity(tableName = "cached_articles", indices = [Index("publishedAt")])
data class CachedArticle(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val sourceName: String,
    val category: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val author: String?,
    val fetchedAt: Long
)

/** A bookmarked article, kept separately so cache clears never drop saved stories. */
@Entity(tableName = "bookmarks", indices = [Index("savedAt")])
data class Bookmark(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val sourceName: String,
    val category: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val author: String?,
    val savedAt: Long
)

/** A user-added RSS/Atom feed. */
@Entity(tableName = "custom_feeds")
data class CustomFeed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val category: String
)

fun CachedArticle.toArticle() = Article(
    id = link, title = title, link = link, description = description,
    sourceName = sourceName, category = category, imageUrl = imageUrl,
    publishedAt = publishedAt, author = author
)

fun Bookmark.toArticle() = Article(
    id = link, title = title, link = link, description = description,
    sourceName = sourceName, category = category, imageUrl = imageUrl,
    publishedAt = publishedAt, author = author
)

fun Article.toCached(fetchedAt: Long) = CachedArticle(
    link = link, title = title, description = description, sourceName = sourceName,
    category = category, imageUrl = imageUrl, publishedAt = publishedAt,
    author = author, fetchedAt = fetchedAt
)

fun Article.toBookmark(savedAt: Long) = Bookmark(
    link = link, title = title, description = description, sourceName = sourceName,
    category = category, imageUrl = imageUrl, publishedAt = publishedAt,
    author = author, savedAt = savedAt
)
