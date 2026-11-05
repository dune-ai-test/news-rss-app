package com.wire.newsapp

import android.app.Application
import com.wire.newsapp.data.FeedRepository
import com.wire.newsapp.data.HttpClients
import com.wire.newsapp.data.SettingsStore
import com.wire.newsapp.data.local.AppDatabase

/** Manual dependency container — keeps the app testable and DI-framework free. */
class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.getInstance(application)
    val settingsStore: SettingsStore = SettingsStore(application)
    private val httpClient = HttpClients.create()
    val feedRepository: FeedRepository = FeedRepository(httpClient, database, settingsStore)
}

class WireApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
