package com.wire.newsapp.ui

import android.content.Context
import android.content.Intent
import com.wire.newsapp.model.Article

/** Android share sheet for a story. */
fun shareArticle(context: Context, article: Article) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
    }
    context.startActivity(Intent.createChooser(intent, "Share story"))
}
