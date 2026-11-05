package com.wire.newsapp.data

import com.wire.newsapp.model.FeedSource

/**
 * Curated catalog of high-quality RSS feeds across every category.
 * Every id is a stable slug so user enable/disable preferences survive updates.
 */
object FeedCatalog {

    const val ALL = "For You"

    val CATEGORIES = listOf(
        ALL,
        "Top Stories",
        "World",
        "Technology",
        "Science",
        "Business",
        "Sports",
        "Culture"
    )

    val FEEDS = listOf(
        // Top Stories
        FeedSource("bbc-top", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml", "Top Stories"),
        FeedSource("npr-top", "NPR News", "https://feeds.npr.org/1001/rss.xml", "Top Stories"),
        FeedSource("aljazeera-top", "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", "Top Stories"),
        FeedSource("guardian-top", "The Guardian", "https://www.theguardian.com/rss", "Top Stories"),

        // World
        FeedSource("bbc-world", "BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", "World"),
        FeedSource("guardian-world", "Guardian World", "https://www.theguardian.com/world/rss", "World"),
        FeedSource("dw-world", "Deutsche Welle", "https://rss.dw.com/rdf/rss-en-world", "World"),
        FeedSource("france24-world", "France 24", "https://www.france24.com/en/rss", "World"),

        // Technology
        FeedSource("theverge-tech", "The Verge", "https://www.theverge.com/rss/index.xml", "Technology"),
        FeedSource("techcrunch-tech", "TechCrunch", "https://techcrunch.com/feed/", "Technology"),
        FeedSource("arstechnica-tech", "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", "Technology"),
        FeedSource("hackernews-tech", "Hacker News", "https://hnrss.org/frontpage", "Technology"),
        FeedSource("wired-tech", "WIRED", "https://www.wired.com/feed/rss", "Technology"),
        FeedSource("guardian-tech", "Guardian Technology", "https://www.theguardian.com/technology/rss", "Technology"),

        // Science
        FeedSource("sciencedaily-sci", "ScienceDaily", "https://www.sciencedaily.com/rss/top/top_news.xml", "Science"),
        FeedSource("nature-sci", "Nature News", "https://www.nature.com/nature.rss", "Science"),
        FeedSource("nasa-sci", "NASA Breaking News", "https://www.nasa.gov/rss/dyn/breaking_news.rss", "Science"),
        FeedSource("guardian-sci", "Guardian Science", "https://www.theguardian.com/science/rss", "Science"),

        // Business
        FeedSource("cnbc-biz", "CNBC Top News", "https://www.cnbc.com/id/100003114/device/rss/rss.html", "Business"),
        FeedSource("marketwatch-biz", "MarketWatch", "https://feeds.marketwatch.com/marketwatch/marketpulse/", "Business"),
        FeedSource("bbc-biz", "BBC Business", "https://feeds.bbci.co.uk/news/business/rss.xml", "Business"),
        FeedSource("guardian-biz", "Guardian Business", "https://www.theguardian.com/business/rss", "Business"),

        // Sports
        FeedSource("espn-sports", "ESPN Top News", "https://www.espn.com/espn/rss/news", "Sports"),
        FeedSource("bbc-sports", "BBC Sport", "https://feeds.bbci.co.uk/sport/rss.xml", "Sports"),
        FeedSource("guardian-sports", "Guardian Sport", "https://www.theguardian.com/sport/rss", "Sports"),

        // Culture
        FeedSource("bbc-culture", "BBC Entertainment", "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml", "Culture"),
        FeedSource("guardian-culture", "Guardian Culture", "https://www.theguardian.com/culture/rss", "Culture"),
        FeedSource("variety-culture", "Variety", "https://variety.com/feed/", "Culture"),
        FeedSource("rollingstone-culture", "Rolling Stone", "https://www.rollingstone.com/feed/", "Culture"),
        FeedSource("thr-culture", "The Hollywood Reporter", "https://www.hollywoodreporter.com/feed/", "Culture")
    )
}
