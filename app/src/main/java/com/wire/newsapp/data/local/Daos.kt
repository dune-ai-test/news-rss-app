package com.wire.newsapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<CachedArticle>)

    @Query("SELECT * FROM cached_articles ORDER BY publishedAt DESC, fetchedAt DESC")
    fun observeAll(): Flow<List<CachedArticle>>

    @Query("SELECT * FROM cached_articles WHERE link = :link LIMIT 1")
    suspend fun getByLink(link: String): CachedArticle?

    @Query("SELECT COUNT(*) FROM cached_articles")
    suspend fun count(): Int

    @Query("DELETE FROM cached_articles")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE link = :link")
    suspend fun deleteByLink(link: String)

    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("SELECT link FROM bookmarks")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM bookmarks")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM bookmarks WHERE link = :link LIMIT 1")
    suspend fun getByLink(link: String): Bookmark?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE link = :link)")
    suspend fun exists(link: String): Boolean
}

@Dao
interface CustomFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feed: CustomFeed): Long

    @Query("DELETE FROM custom_feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM custom_feeds ORDER BY id DESC")
    fun observeAll(): Flow<List<CustomFeed>>

    @Query("SELECT * FROM custom_feeds")
    suspend fun getAll(): List<CustomFeed>
}
