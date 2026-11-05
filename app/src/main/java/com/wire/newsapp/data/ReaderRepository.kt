package com.wire.newsapp.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

sealed interface ReaderBlock {
    data class Paragraph(val text: String) : ReaderBlock
    data class Heading(val text: String) : ReaderBlock
    data class Quote(val text: String) : ReaderBlock
    data class BlockImage(val url: String, val caption: String?) : ReaderBlock
}

data class ReaderContent(
    val title: String,
    val siteName: String?,
    val byline: String?,
    val heroImageUrl: String?,
    val blocks: List<ReaderBlock>,
    val readingMinutes: Int
)

/**
 * "Readability-lite": fetches the article page and extracts a clean sequence of
 * paragraphs, headings, quotes and images with boilerplate stripped away.
 */
object ReaderRepository {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val NOISE = listOf(
        "script", "style", "noscript", "iframe", "nav", "footer", "header", "aside",
        "form", "button", "select", "input", "svg", "canvas", "form",
        ".ad", ".ads", ".advertisement", ".social", ".share", ".sharing",
        ".newsletter", ".subscribe", ".subscription", ".related", ".recommended",
        ".comments", ".comment", ".cookie", ".consent", ".modal", ".popup",
        "[class*=newsletter]", "[class*=subscribe]", "[class*=social]",
        "[class*=comment]", "[class*=related]", "[class*=promo]", "[id*=newsletter]"
    ).joinToString(", ")

    fun extract(url: String): ReaderContent {
        val doc = Jsoup.connect(url)
            .userAgent(UA)
            .timeout(15_000)
            .followRedirects(true)
            .ignoreContentType(true)
            .get()

        doc.select(NOISE).remove()

        val title = meta(doc, "og:title") ?: doc.selectFirst("h1")?.text()?.trim()
            ?: doc.title().substringBefore("|").trim()
        val siteName = meta(doc, "og:site_name")
            ?: runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull()
        val byline = doc.selectFirst("[rel=author], .byline, .author-name, [itemprop=author]")
            ?.text()?.trim()?.take(120)
        val hero = meta(doc, "og:image")
            ?: meta(doc, "twitter:image")

        val root: Element = pickRoot(doc)
        val blocks = mutableListOf<ReaderBlock>()
        var words = 0
        var lastText: String? = null

        root.select("p, h1, h2, h3, h4, blockquote, figure, img").forEach { el ->
            when (el.tagName()) {
                "p" -> {
                    val text = el.text().trim()
                    if (text.length >= 30 && text != lastText) {
                        blocks += ReaderBlock.Paragraph(text)
                        words += text.split(Regex("\\s+")).size
                        lastText = text
                    }
                }
                "h1", "h2", "h3", "h4" -> {
                    val text = el.text().trim()
                    if (text.length in 3..120) blocks += ReaderBlock.Heading(text)
                }
                "blockquote" -> {
                    val text = el.text().trim()
                    if (text.length >= 30) blocks += ReaderBlock.Quote(text)
                }
                "figure" -> {
                    val img = el.selectFirst("img")
                    img?.let { imageBlock(it) }?.let(blocks::add)
                }
                "img" -> {
                    if (el.parent()?.tagName() != "figure") {
                        imageBlock(el)?.let(blocks::add)
                    }
                }
            }
            if (blocks.size >= 120) return@forEach
        }

        val deduped = dedupe(blocks)
        if (deduped.isEmpty()) {
            throw IllegalStateException("Couldn't extract readable content from this page.")
        }
        val minutes = maxOf(1, Math.round(words / 215.0).toInt())
        return ReaderContent(
            title = title,
            siteName = siteName,
            byline = byline,
            heroImageUrl = hero,
            blocks = deduped,
            readingMinutes = minutes
        )
    }

    private fun pickRoot(doc: Document): Element {
        val simple = doc.select("article, [itemprop=articleBody], [role=main], main, .article-body, .post-content, .story-body, .entry-content")
        simple.firstOrNull()?.let { candidate ->
            if (candidate.select("p").sumOf { it.text().length } >= 200) return candidate
        }
        // Paragraph-density heuristic: pick the element with the most <p> text.
        var best: Element? = null
        var bestScore = 0
        doc.select("div, section, article").forEach { el ->
            val pText = el.select("p").sumOf { it.text().length }
            val linkText = el.select("a").sumOf { it.text().length }
            val score = pText - (linkText / 2)
            if (pText >= 400 && score > bestScore) {
                bestScore = score
                best = el
            }
        }
        return best ?: doc.body()
    }

    private fun imageBlock(img: Element): ReaderBlock.BlockImage? {
        val absSrc = img.absUrl("src")
        val src = if (absSrc.isBlank()) img.absUrl("data-src") else absSrc
        if (src.isBlank() || src.startsWith("data:")) return null
        if (img.attr("width").toIntOrNull()?.let { it < 200 } == true) return null
        val caption = (img.parent()?.selectFirst("figcaption")?.text()
            ?: img.attr("alt")).trim().takeIf { it.isNotBlank() && it.length < 200 }
        return ReaderBlock.BlockImage(url = src, caption = caption)
    }

    private fun dedupe(blocks: List<ReaderBlock>): List<ReaderBlock> =
        blocks.fold(mutableListOf()) { acc, block ->
            if (acc.lastOrNull() != block) acc.add(block)
            acc
        }

    private fun meta(doc: Document, property: String): String? =
        doc.selectFirst("meta[property=$property]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[name=$property]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
}
