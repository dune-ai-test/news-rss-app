package com.wire.newsapp.data.rss

import com.wire.newsapp.model.Article
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lightweight, dependency-free RSS 2.0 / Atom parser built on XmlPullParser.
 * Namespace processing is disabled so prefixed tags ("media:content", "dc:creator")
 * can be matched by plain string equality.
 */
object RssParser {

    private val IMG_SRC = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val TAGS = Regex("<[^>]+>")
    private const val MAX_PER_FEED = 40

    fun parse(xml: String, sourceName: String, category: String): List<Article> {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val articles = mutableListOf<Article>()
        var inItem = false

        var title = ""
        var link = ""
        var description = ""
        var encoded: String? = null
        var dateRaw: String? = null
        var author: String? = null
        var imageUrl: String? = null

        fun reset() {
            title = ""; link = ""; description = ""; encoded = null
            dateRaw = null; author = null; imageUrl = null
        }

        fun leafText(): String = try {
            parser.nextText().trim()
        } catch (e: Exception) {
            ""
        }

        fun finishItem() {
            val cleanTitle = stripHtml(title)
            if (cleanTitle.isBlank()) return
            val url = if (link.isBlank()) cleanTitle else link
            val summarySource = if (description.isBlank()) encoded.orEmpty() else description
            val summary = stripHtml(summarySource)
            val image = imageUrl ?: IMG_SRC.find(encoded.orEmpty())?.groupValues?.get(1)
                ?: IMG_SRC.find(description)?.groupValues?.get(1)
            articles += Article(
                id = url,
                title = cleanTitle,
                link = url,
                description = truncate(summary, 280),
                sourceName = sourceName,
                category = category,
                imageUrl = image?.let { absolutize(it) },
                publishedAt = dateRaw?.let(::parseDate) ?: 0L,
                author = author?.let(::stripHtml)?.takeIf { it.isNotBlank() }
            )
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && articles.size < MAX_PER_FEED) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name ?: ""
                    when {
                        name == "item" || name == "entry" -> {
                            inItem = true
                            reset()
                        }
                        !inItem -> Unit
                        name == "title" -> title = leafText()
                        name == "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            val rel = parser.getAttributeValue(null, "rel")
                            if (href != null) {
                                // Atom: prefer rel="alternate" (or missing rel)
                                if (link.isBlank() || rel == null || rel == "alternate") link = href.trim()
                            } else {
                                link = leafText()
                            }
                        }
                        name == "description" || name == "summary" -> description = leafText()
                        name == "content" || name == "content:encoded" || name == "encoded" ->
                            encoded = leafText()
                        name == "pubDate" || name == "published" || name == "updated" ||
                            name == "date" || name == "dc:date" -> dateRaw = leafText()
                        name == "author" || name == "dc:creator" || name == "creator" -> {
                            // Only capture when the element holds direct text; nested
                            // <author><name>..</name></author> in Atom is skipped safely.
                            val text = try { parser.nextText().trim() } catch (e: Exception) { "" }
                            if (text.isNotBlank()) author = text
                        }
                        name == "media:thumbnail" || name == "media:content" ||
                            name == "thumbnail" || name == "enclosure" -> {
                            val url = parser.getAttributeValue(null, "url")
                                ?: parser.getAttributeValue(null, "src")
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            if (url != null && (name != "enclosure" || type.startsWith("image"))) {
                                if (imageUrl == null) imageUrl = url.trim()
                            }
                        }
                        name == "image" -> {
                            // <image><url>..</url></image> at channel level is irrelevant;
                            // inside an item it is rare, so we also accept a direct child url.
                            Unit
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name ?: ""
                    if (name == "item" || name == "entry") {
                        finishItem()
                        inItem = false
                    }
                }
            }
            try {
                eventType = parser.next()
            } catch (e: Exception) {
                break
            }
        }
        return articles
    }

    private fun parseDate(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0L
        // ISO 8601 variants
        listOf(
            { Instant.parse(s).toEpochMilli() },
            { OffsetDateTime.parse(s).toInstant().toEpochMilli() },
            { ZonedDateTime.parse(s).toInstant().toEpochMilli() }
        ).forEach { attempt ->
            try {
                return attempt()
            } catch (_: Exception) { }
        }
        // RFC 1123 / 822 variants
        try {
            return DateTimeFormatter.RFC_1123_DATE_TIME.parse(s, Instant::from).toEpochMilli()
        } catch (_: Exception) { }
        val rfc = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        try {
            return rfc.parse(s)?.time ?: 0L
        } catch (_: Exception) { }
        val rfcNoZone = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
        try {
            return rfcNoZone.parse(s)?.time ?: 0L
        } catch (_: Exception) { }
        return 0L
    }

    fun stripHtml(input: String): String {
        if (input.isBlank()) return ""
        return TAGS.replace(input, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.take(max - 1).trimEnd() + "…"

    private fun absolutize(url: String): String =
        when {
            url.startsWith("//") -> "https:$url"
            else -> url
        }
}
