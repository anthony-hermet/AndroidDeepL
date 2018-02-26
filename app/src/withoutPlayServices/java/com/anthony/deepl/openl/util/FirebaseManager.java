package com.anthony.deepl.openl.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import timber.log.Timber;

public class FirebaseManager implements FirebaseManagerInterface {

    @SuppressWarnings("unused")
    public FirebaseManager(Context context) {}

    @Override
    public void fetchRemoteConfigValues() {}

    @Override
    public void logEvent(String event, Bundle bundle) {}

    @Override
    public Timber.Tree getProductionTimberTree() {
        return new Timber.Tree() {
            @Override
            protected void log(int priority, String tag, @NonNull String message, Throwable t) {
            }
        };
    }
}
