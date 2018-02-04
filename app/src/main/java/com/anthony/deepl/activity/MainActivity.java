package com.anthony.deepl.activity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.anthony.deepl.BuildConfig;
import com.anthony.deepl.R;
import com.anthony.deepl.fragment.MainFragment;
import com.anthony.deepl.manager.LanguageManager;

import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {

    private static final int SPEECH_TO_TEXT_REQUEST_CODE = 32;
    private static final String LAUNCH_DIALOG_TITLE_KEY = "launch_dialog_title";
    private static final String LAUNCH_DIALOG_CONTENT_KEY = "launch_dialog_content";
    private static final String LAUNCH_DIALOG_URL_KEY = "launch_dialog_url";
    private static final String LAUNCH_DIALOG_URL_LABEL_KEY = "launch_dialog_url_label";

    private MainFragment mMainFragment;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // We remove splash screen theme before on create
        setTheme(R.style.AppTheme);

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
                mMainFragment.setToTranslateText(sharedText);
            }
        }

        // We init and fetch values from Firebase analytics and remote config
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.this);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build());
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfigValues();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_TO_TEXT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            mMainFragment.setToTranslateText(text);
            logEvent("speech_to_text_success", null);
        }
    }

    @Override
    public void onSpeechToTextTapped(String selectedLocale) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_to_text_hint));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, !selectedLocale.equals(LanguageManager.AUTO) ? selectedLocale.toLowerCase() : Locale.getDefault());

        try {
            startActivityForResult(intent, SPEECH_TO_TEXT_REQUEST_CODE);
            logEvent("speech_to_text_displayed", null);
        } catch (ActivityNotFoundException e) {
            View view = mMainFragment.getView();
            if (view != null) {
                Snackbar.make(mMainFragment.getView(), R.string.speech_to_text_error, Snackbar.LENGTH_SHORT).show();
            }
            Timber.e(e);
        }
    }

    @Override
    public void logEvent(String event, Bundle bundle) {
        mFirebaseAnalytics.logEvent(event, bundle);
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
                            final String launchDialogUrl = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_KEY);
                            String launchDialogUrlLabel = mFirebaseRemoteConfig.getString(LAUNCH_DIALOG_URL_LABEL_KEY);
                            if (launchDialogContent != null && launchDialogContent.length() > 0) {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                                alertDialogBuilder
                                        .setTitle(launchDialogTitle)
                                        .setMessage(launchDialogContent.replace("\\n", "\n"))
                                        .setCancelable(true)
                                        .setPositiveButton(getString(R.string.ok), null);
                                if (launchDialogUrl != null && launchDialogUrl.length() > 0 &&
                                        launchDialogUrlLabel != null && launchDialogUrlLabel.length() > 0) {
                                    alertDialogBuilder.setNeutralButton(launchDialogUrlLabel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchDialogUrl));
                                            startActivity(browserIntent);
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
}
