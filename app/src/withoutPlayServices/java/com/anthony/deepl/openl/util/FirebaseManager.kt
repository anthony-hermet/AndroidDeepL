package com.anthony.deepl.openl.util

import android.os.Bundle

import timber.log.Timber

class FirebaseManager : FirebaseManagerInterface {

    override val productionTimberTree: Timber.Tree
        get() = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
        }

    override fun fetchRemoteConfigValues() {}

    override fun logEvent(event: String, bundle: Bundle?) {}
}
