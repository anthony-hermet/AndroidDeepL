package com.anthony.deepl.openl.util

import android.os.Bundle

import timber.log.Timber

interface FirebaseManagerInterface {
    val productionTimberTree: Timber.Tree
    fun fetchRemoteConfigValues()
    fun logEvent(event: String, bundle: Bundle?)
}
