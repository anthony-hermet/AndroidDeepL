package com.anthony.deepl.openl.util;

import android.os.Bundle;

import timber.log.Timber;

public interface FirebaseManagerInterface {
    void fetchRemoteConfigValues();
    void logEvent(String event, Bundle bundle);
    Timber.Tree getProductionTimberTree();
}
