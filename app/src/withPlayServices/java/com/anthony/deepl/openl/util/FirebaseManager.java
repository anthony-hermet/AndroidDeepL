package com.anthony.deepl.openl.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.anthony.deepl.openl.BuildConfig;
import com.anthony.deepl.openl.R;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import timber.log.Timber;

public class FirebaseManager implements FirebaseManagerInterface {

    private static final String LAUNCH_DIALOG_TITLE_KEY = BuildConfig.DEBUG ? "launch_dialog_title_test" : "launch_dialog_title";
    private static final String LAUNCH_DIALOG_CONTENT_KEY = BuildConfig.DEBUG ? "launch_dialog_content_test" : "launch_dialog_content";
    private static final String LAUNCH_DIALOG_URL_KEY = BuildConfig.DEBUG ? "launch_dialog_url_test" : "launch_dialog_url";
    private static final String LAUNCH_DIALOG_URL_LABEL_KEY = BuildConfig.DEBUG ? "launch_dialog_url_label_test" : "launch_dialog_url_label";
    private static final String LAUNCH_DIALOG_MAX_VERSION_KEY = BuildConfig.DEBUG ? "launch_dialog_max_version_test" : "launch_dialog_max_version";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Context mContext;

    public FirebaseManager(Context context) {
        mContext = context;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build());
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
    }

    @Override
    public void fetchRemoteConfigValues() {
        long cacheExpiration = mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled() ? 0 : 3600;
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                            long currentAppVersionCode;
                            try {
                                PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                                currentAppVersionCode = pInfo.versionCode;
                            } catch (PackageManager.NameNotFoundException e) {
                                Timber.e(e);
                                return;
                            }
                            String launchDialogTitle = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_TITLE_KEY);
                            String launchDialogContent = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_CONTENT_KEY);
                            final String launchDialogUrl = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_KEY);
                            String launchDialogUrlLabel = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_LABEL_KEY);
                            long maxAppVersion = mFirebaseRemoteConfig.getLong(LAUNCH_DIALOG_MAX_VERSION_KEY);

                            if (launchDialogContent != null && launchDialogContent.length() > 0 && currentAppVersionCode <= maxAppVersion) {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                                alertDialogBuilder
                                        .setTitle(launchDialogTitle)
                                        .setMessage(launchDialogContent.replace("\\n", "\n"))
                                        .setCancelable(true)
                                        .setPositiveButton(mContext.getString(R.string.ok), null);
                                if (launchDialogUrl != null && launchDialogUrl.length() > 0 &&
                                        launchDialogUrlLabel != null && launchDialogUrlLabel.length() > 0) {
                                    alertDialogBuilder.setNeutralButton(launchDialogUrlLabel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchDialogUrl));
                                            mContext.startActivity(browserIntent);
                                        }
                                    });
                                }
                                alertDialogBuilder.create().show();
                                logEvent("launch_dialog_displayed", null);
                            }
                        }
                    }
                });
    }

    @Override
    public void logEvent(String event, Bundle bundle) {
        if (BuildConfig.DEBUG) return;
        mFirebaseAnalytics.logEvent(event, bundle);
    }

    public Timber.Tree getProductionTimberTree() {
        return new CrashlyticsTree();
    }

    public class CrashlyticsTree extends Timber.Tree {
        private static final String CRASHLYTICS_KEY_PRIORITY = "priority";

        @Override
        protected void log(int priority, @Nullable String tag, @Nullable String message, @Nullable Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return;
            }

            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority);
            Crashlytics.logException(t != null ? t : new Exception(message));
        }
    }
}
