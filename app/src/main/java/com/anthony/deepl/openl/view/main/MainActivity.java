package com.anthony.deepl.openl.view.main;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.RecognizerIntent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.anthony.deepl.openl.manager.LanguageManager;
import com.anthony.deepl.openl.R;
import com.anthony.deepl.openl.util.FirebaseManager;

import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {

    private static final int SPEECH_TO_TEXT_REQUEST_CODE = 32;

    private MainFragment mMainFragment;
    private FirebaseManager mFirebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // We remove splash screen theme before on create
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

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
        mFirebaseManager = new FirebaseManager(MainActivity.this);
        mFirebaseManager.fetchRemoteConfigValues();
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
    public int getCurrentMediaVolume() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audio != null ? audio.getStreamVolume(AudioManager.STREAM_MUSIC) : -1;
    }

    @Override
    public void logEvent(String event, Bundle bundle) {
        mFirebaseManager.logEvent(event, bundle);
    }

    private void initViews() {
        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
    }
}
