package com.anthony.deepl.openl

import android.app.Application

import com.anthony.deepl.openl.util.FirebaseManager

import timber.log.Timber

const val ENABLE_CRASHLYTICS = true

class DeepLApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLogTool()
    }

    private fun setupLogTool() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else if (ENABLE_CRASHLYTICS) {
            Timber.plant(FirebaseManager(this).productionTimberTree)
        }
    }

}
