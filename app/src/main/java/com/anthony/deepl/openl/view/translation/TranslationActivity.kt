package com.anthony.deepl.openl.view.translation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.speech.RecognizerIntent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.anthony.deepl.openl.manager.LanguageManager
import com.anthony.deepl.openl.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.viewModel

import java.util.Locale

import timber.log.Timber

class TranslationActivity : AppCompatActivity(), TranslationFragment.OnFragmentInteractionListener {

    companion object {
        private const val SPEECH_TO_TEXT_REQUEST_CODE = 32
    }

    private val viewModel by viewModel<TranslationViewModel>()
    private lateinit var translationFragment: TranslationFragment

    override val currentMediaVolume: Int
        get() {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // We remove splash screen theme before on create
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        volumeControlStream = AudioManager.STREAM_MUSIC

        // We handle possible share text intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                translationFragment.setToTranslateText(sharedText)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_TO_TEXT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)[0]
            translationFragment.setToTranslateText(text)
            viewModel.logEvent("speech_to_text_success", null)
        }
    }

    override fun onSpeechToTextTapped(selectedLocale: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_to_text_hint))
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (selectedLocale != LanguageManager.AUTO) selectedLocale.toLowerCase() else Locale.getDefault())

        try {
            startActivityForResult(intent, SPEECH_TO_TEXT_REQUEST_CODE)
            viewModel.logEvent("speech_to_text_displayed", null)
        } catch (e: ActivityNotFoundException) {
            translationFragment.view?.let {
                Snackbar.make(it, R.string.speech_to_text_error, Snackbar.LENGTH_SHORT).show()
            }
            Timber.e(e)
        }
    }

    private fun initViews() {
        setSupportActionBar(toolbar as Toolbar)
        translationFragment = supportFragmentManager.findFragmentById(R.id.main_fragment) as TranslationFragment
    }

}