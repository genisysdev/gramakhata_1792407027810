package com.gramakhata.app

import android.app.Application
import com.gramakhata.app.data.db.AppDatabase
import com.gramakhata.app.data.repository.AppRepository

/**
 * Application class that initializes the database and repository as singletons.
 * Provides access to the repository throughout the app lifecycle.
 */
class GramaKhataApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AppRepository(database.customerDao(), database.transactionDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GramaKhataApp
            private set
    }
}
