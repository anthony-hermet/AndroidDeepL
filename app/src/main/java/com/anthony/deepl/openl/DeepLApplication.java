package com.anthony.deepl.openl;

import android.app.Application;

import com.anthony.deepl.openl.util.FirebaseManager;

import timber.log.Timber;

public class DeepLApplication extends Application {

    private static final boolean ENABLE_CRASHLYTICS = true;

    @Override
    public void onCreate() {
        super.onCreate();
        setupLogTool();
    }

    private void setupLogTool() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else if (ENABLE_CRASHLYTICS) {
            Timber.plant(new FirebaseManager(this).getProductionTimberTree());
        }
    }
}
