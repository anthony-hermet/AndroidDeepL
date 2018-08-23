package com.anthony.deepl.openl.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog

import com.anthony.deepl.openl.BuildConfig
import com.anthony.deepl.openl.R
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

import timber.log.Timber

class FirebaseManager(private val mContext: Context) : FirebaseManagerInterface {

    companion object {
        private val LAUNCH_DIALOG_TITLE_KEY = if (BuildConfig.DEBUG) "launch_dialog_title_test" else "launch_dialog_title"
        private val LAUNCH_DIALOG_CONTENT_KEY = if (BuildConfig.DEBUG) "launch_dialog_content_test" else "launch_dialog_content"
        private val LAUNCH_DIALOG_URL_KEY = if (BuildConfig.DEBUG) "launch_dialog_url_test" else "launch_dialog_url"
        private val LAUNCH_DIALOG_URL_LABEL_KEY = if (BuildConfig.DEBUG) "launch_dialog_url_label_test" else "launch_dialog_url_label"
        private val LAUNCH_DIALOG_MAX_VERSION_KEY = if (BuildConfig.DEBUG) "launch_dialog_max_version_test" else "launch_dialog_max_version"
    }

    private val mFirebaseRemoteConfig: FirebaseRemoteConfig
    private val mFirebaseAnalytics: FirebaseAnalytics

    override val productionTimberTree: Timber.Tree
        get() = CrashlyticsTree()

    init {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(mContext)
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        mFirebaseRemoteConfig.setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build())
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)
    }

    override fun fetchRemoteConfigValues() {
        val cacheExpiration = (if (mFirebaseRemoteConfig.info.configSettings.isDeveloperModeEnabled) 0 else 3600).toLong()
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        mFirebaseRemoteConfig.activateFetched()
                        val currentAppVersionCode: Long
                        try {
                            val pInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
                            currentAppVersionCode = pInfo.versionCode.toLong()
                        } catch (e: PackageManager.NameNotFoundException) {
                            Timber.e(e)
                            return@OnCompleteListener
                        }

                        val launchDialogTitle = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_TITLE_KEY)
                        val launchDialogContent = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_CONTENT_KEY)
                        val launchDialogUrl = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_KEY)
                        val launchDialogUrlLabel = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_LABEL_KEY)
                        val maxAppVersion = mFirebaseRemoteConfig.getLong(LAUNCH_DIALOG_MAX_VERSION_KEY)

                        if (launchDialogContent != null && launchDialogContent.length > 0 && currentAppVersionCode <= maxAppVersion) {
                            val alertDialogBuilder = AlertDialog.Builder(mContext)
                            alertDialogBuilder
                                    .setTitle(launchDialogTitle)
                                    .setMessage(launchDialogContent.replace("\\n", "\n"))
                                    .setCancelable(true)
                                    .setPositiveButton(mContext.getString(R.string.ok), null)
                            if (launchDialogUrl.isNotEmpty() && launchDialogUrlLabel.isNotEmpty()) {
                                alertDialogBuilder.setNeutralButton(launchDialogUrlLabel) { _, _ ->
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launchDialogUrl))
                                    mContext.startActivity(browserIntent)
                                }
                            }
                            alertDialogBuilder.create().show()
                            logEvent("launch_dialog_displayed", null!!)
                        }
                    }
                })
    }

    override fun logEvent(event: String, bundle: Bundle?) {
        if (BuildConfig.DEBUG) return
        mFirebaseAnalytics.logEvent(event, bundle)
    }

    inner class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return
            }

            Crashlytics.setInt("priority" , priority)
            Crashlytics.logException(t ?: Exception(message))
        }
    }

}
