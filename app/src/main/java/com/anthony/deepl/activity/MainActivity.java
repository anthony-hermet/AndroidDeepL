package com.anthony.deepl.activity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.anthony.deepl.BuildConfig;
import com.anthony.deepl.R;
import com.anthony.deepl.fragment.MainFragment;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {

    private static final String LAUNCH_DIALOG_TITLE_KEY = "launch_dialog_title";
    private static final String LAUNCH_DIALOG_CONTENT_KEY = "launch_dialog_content";

    private MainFragment mMainFragment;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        // We handle possible share text intent
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        String intentType = intent.getType();
        if (intentAction != null && intentAction.equals(Intent.ACTION_SEND) &&
                intentType != null && intentType.equals("text/plain")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                mMainFragment.setSharedText(sharedText);
            }
        }

        // We init and fetch values from Firebase remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build());
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfigValues();
    }

    private void initViews() {
        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
    }

    private void fetchRemoteConfigValues() {
        long cacheExpiration = mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled() ? 0 : 3600;
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                            String launchDialogTitle = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_TITLE_KEY);
                            String launchDialogContent = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_CONTENT_KEY);
                            if (launchDialogContent != null && launchDialogContent.length() > 0) {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                                alertDialogBuilder
                                        .setTitle(launchDialogTitle)
                                        .setMessage(launchDialogContent.replace("\\n", "\n"))
                                        .setCancelable(true)
                                        .setPositiveButton(getString(R.string.ok), null)
                                        .create()
                                        .show();
                            }
                        }
                    }
                });
    }

}
