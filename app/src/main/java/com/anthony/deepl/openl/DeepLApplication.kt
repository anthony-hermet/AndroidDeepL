package com.anthony.deepl.openl

import android.app.Application
import com.anthony.deepl.openl.di.deeplAppModule

import com.anthony.deepl.openl.util.FirebaseManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

import timber.log.Timber

class DeepLApplication : Application() {

    companion object {
        private const val ENABLE_CRASHLYTICS = true
    }

    override fun onCreate() {
        super.onCreate()
        setupLogTool()
        startKoin {
            logger(Level.ERROR)
            androidContext(this@DeepLApplication)
            modules(listOf(deeplAppModule))
        }
    }

    private fun setupLogTool() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else if (ENABLE_CRASHLYTICS) {
            Timber.plant(FirebaseManager(this).productionTimberTree)
        }
    }

}
