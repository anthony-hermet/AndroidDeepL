package com.anthony.deepl;

import android.app.Application;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class DeepLApplication extends Application {

    private static final boolean ENABLE_CRASHLYTICS = true;

    @Override
    public void onCreate() {
        super.onCreate();
        setupLogTool();
    }

    // region Log enhancements

    private void setupLogTool() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else if (ENABLE_CRASHLYTICS) {
            Timber.plant(new CrashlyticsTree());
        }
    }

    private class CrashlyticsTree extends Timber.Tree {
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

    // endregion
}
