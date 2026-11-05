package com.wire.newsapp.data

import com.wire.newsapp.data.local.*
import com.wire.newsapp.data.rss.RssParser
import com.wire.newsapp.model.Article
import com.wire.newsapp.model.FeedSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches, merges, caches and bookmarks articles.
 * All network work degrades gracefully to the Room cache when offline.
 */
class FeedRepository(
    private val httpClient: OkHttpClient,
    private val database: AppDatabase,
    private val settings: SettingsStore
) {
    private val cachedDao = database.cachedArticleDao()
    private val bookmarkDao = database.bookmarkDao()
    private val customFeedDao = database.customFeedDao()

    data class RefreshResult(
        val articles: List<Article>,
        val fetchedFeeds: Int,
        val failedFeeds: List<String>,
        val fromCache: Boolean
    )

    // ---------- Sources ----------

    /** Built-in catalog with the user's disabled ids applied. */
    fun observeDisabledFeedIds(): Flow<Set<String>> = settings.disabledFeedIds

    fun observeCustomFeeds(): Flow<List<CustomFeed>> = customFeedDao.observeAll()

    suspend fun setFeedEnabled(id: String, enabled: Boolean) = settings.setFeedEnabled(id, enabled)

    suspend fun addCustomFeed(name: String, url: String, category: String): Long =
        customFeedDao.insert(CustomFeed(name = name.trim(), url = url.trim(), category = category))

    suspend fun removeCustomFeed(id: Long) = customFeedDao.deleteById(id)

    private suspend fun enabledSources(): List<FeedSource> {
        val disabled = settings.disabledFeedIds.first()
        val builtIn = FeedCatalog.FEEDS.filterNot { it.id in disabled }
        val custom = customFeedDao.getAll()
            .map { FeedSource(id = "custom:${it.id}", name = it.name, url = it.url, category = it.category, isCustom = true) }
            .filterNot { it.id in disabled }
        return builtIn + custom
    }

    // ---------- Fetching ----------

    suspend fun refresh(): RefreshResult {
        val sources = enabledSources()
        val results = coroutineScope {
            sources.map { source ->
                async { source to fetchFeed(source) }
            }.awaitAll()
        }

        val failed = results.filter { it.second == null }.map { it.first.name }
        val fetched = results.filter { it.second != null }.map { it.first to it.second!! }

        val merged = fetched
            .flatMap { it.second }
            .associateBy { it.id }
            .values
            .sortedWith(compareByDescending<Article> { it.publishedAt }.thenBy { it.sourceName })

        if (merged.isNotEmpty()) {
            cachedDao.upsertAll(merged.map { it.toCached(System.currentTimeMillis()) })
            settings.setLastSync(System.currentTimeMillis())
            return RefreshResult(merged, fetched.size, failed, fromCache = false)
        }

        // Total failure (e.g. offline) -> fall back to cache
        val cached = cachedDao.observeAll().first().map { it.toArticle() }
        return RefreshResult(cached, fetchedFeeds = 0, failedFeeds = failed, fromCache = true)
    }

    private fun fetchFeed(source: FeedSource): List<Article>? {
        return try {
            val request = Request.Builder()
                .url(source.url)
                .header("User-Agent", "Wire/1.0 (Android; +https://github.com/dune-ai-test/news-rss-app)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                RssParser.parse(body, source.name, source.category)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Cache ----------

    fun observeCachedArticles(): Flow<List<Article>> =
        cachedDao.observeAll().map { list -> list.map { it.toArticle() } }

    suspend fun cachedArticle(link: String): Article? = cachedDao.getByLink(link)?.toArticle()

    suspend fun clearCache() = cachedDao.clear()

    suspend fun cacheCount(): Int = cachedDao.count()

    // ---------- Bookmarks ----------

    fun observeBookmarks(): Flow<List<Article>> =
        bookmarkDao.observeAll().map { list -> list.map { it.toArticle() } }

    fun observeBookmarkedIds(): Flow<Set<String>> =
        bookmarkDao.observeIds().map { it.toSet() }

    fun observeBookmarkCount(): Flow<Int> = bookmarkDao.observeCount()

    suspend fun isBookmarked(link: String): Boolean = bookmarkDao.exists(link)

    suspend fun bookmarkedArticle(link: String): Article? = bookmarkDao.getByLink(link)?.toArticle()

    suspend fun toggleBookmark(article: Article): Boolean {
        return if (bookmarkDao.exists(article.link)) {
            bookmarkDao.deleteByLink(article.link)
            false
        } else {
            bookmarkDao.upsert(article.toBookmark(System.currentTimeMillis()))
            true
        }
    }

    suspend fun removeBookmark(link: String) = bookmarkDao.deleteByLink(link)
}

/** Shared OkHttp client factory. */
object HttpClients {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
}
