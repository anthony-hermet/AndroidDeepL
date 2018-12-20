package com.anthony.deepl.openl.view.translation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.TextView
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.anthony.deepl.openl.R


import java.util.ArrayList

import timber.log.Timber

import com.anthony.deepl.openl.manager.LanguageManager
import com.anthony.deepl.openl.manager.LanguageManager.AUTO
import com.anthony.deepl.openl.model.FailureResource
import com.anthony.deepl.openl.model.LoadingResource
import com.anthony.deepl.openl.model.SuccessResource
import com.anthony.deepl.openl.util.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main.*
import org.koin.androidx.viewmodel.ext.sharedViewModel

class TranslationFragment : Fragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    companion object {
        private const val INSTANCE_TRANSLATED_FROM_KEY = "last_translated_from"
        private const val INSTANCE_TRANSLATED_TO_KEY = "last_translated_to"
        private const val INSTANCE_TRANSLATED_SENTENCE_KEY = "last_translated_sentence"
        private const val INSTANCE_DETECTED_LANGUAGE_KEY = "detected_language"
        private const val INSTANCE_LAST_ALTERNATIVES_KEY = "last_alternatives"
    }

    private val viewModel by sharedViewModel<TranslationViewModel>()

    private var mRetrySnackBar: Snackbar? = null
    private var mTranslateFromLanguages: Array<String> = arrayOf()
    private var mTranslateToLanguages: Array<String> = arrayOf()
    private var mLastTranslatedSentence: String? = null
    private var mLastTranslatedFrom: String? = null
    private var mLastTranslatedTo: String? = null
    private var mLastAlternatives: List<String>? = null
    private var mDetectedLanguage: String? = null
    private var mTextToSpeechInitialized: Boolean = false

    private lateinit var mListener: OnFragmentInteractionListener
    private lateinit var mTranslateFromAdapter: ShrinkSpinnerAdapter<*>
    private lateinit var mTextToSpeech: TextToSpeech

    // region Overridden methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTextToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                mTextToSpeechInitialized = true
                updateTextToSpeechVisibility()
            }
        })
        mTextToSpeechInitialized = false

        // Restore instance state if needed
        savedInstanceState?.let {
            mLastTranslatedFrom = it.getString(INSTANCE_TRANSLATED_FROM_KEY, null)
            mLastTranslatedTo = it.getString(INSTANCE_TRANSLATED_TO_KEY, null)
            mLastTranslatedSentence = it.getString(INSTANCE_TRANSLATED_SENTENCE_KEY, null)
            mDetectedLanguage = it.getString(INSTANCE_DETECTED_LANGUAGE_KEY, null)
            mLastAlternatives = it.getStringArrayList(INSTANCE_LAST_ALTERNATIVES_KEY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mTextToSpeech.stop()
        mTextToSpeech.shutdown()
    }

    override fun onStart() {
        super.onStart()
        viewModel.liveTranslationResponse.observe(this, Observer { resource ->
            if (resource !is FailureResource) {
                mRetrySnackBar?.let {
                    it.dismiss()
                    mRetrySnackBar = null
                }
            }
            when (resource) {
                is FailureResource -> {
                    translate_progressbar.visibility = View.GONE
                    translated_edit_text.text = ""
                    translate_progressbar.visibility = View.GONE

                    if (mRetrySnackBar == null) {
                        view?.let { view ->
                            mRetrySnackBar = Snackbar.make(view, R.string.snack_bar_retry_label, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.snack_bar_retry_button) {
                                        updateTranslation(true)
                                        mListener.logEvent("retry_snack_bar_tapped", null)
                                    }
                            mRetrySnackBar?.show()
                        }
                        mListener.logEvent("retry_snack_bar_displayed", null)
                    }
                }
                is SuccessResource -> {
                    translated_edit_text.text = resource.data.getBestTranslation()

                    // Alternative translations
                    mLastAlternatives = resource.data.getAlternateTranslations()
                    updateAlternatives(requireContext())

                    // Detected language
                    if (translate_from_spinner.selectedItemPosition == 0) {
                        mDetectedLanguage = resource.data.sourceLanguage
                        displayDetectedLanguage()
                    }

                    // Reporting
                    val params = Bundle()
                    params.putString("translate_from", mLastTranslatedFrom)
                    params.putString("translate_to", mLastTranslatedTo)
                    mListener.logEvent("translation", params)
                }
                is LoadingResource -> {
                    translate_progressbar.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (getClipboardText() != null && to_translate_edit_text.text.isEmpty()) {
            paste_fab_button.show()
        } else {
            paste_fab_button.hide()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(INSTANCE_TRANSLATED_FROM_KEY, mLastTranslatedFrom)
        outState.putString(INSTANCE_TRANSLATED_TO_KEY, mLastTranslatedTo)
        outState.putString(INSTANCE_TRANSLATED_SENTENCE_KEY, mLastTranslatedSentence)
        outState.putString(INSTANCE_DETECTED_LANGUAGE_KEY, mDetectedLanguage)
        outState.putStringArrayList(INSTANCE_LAST_ALTERNATIVES_KEY, mLastAlternatives as ArrayList<String>?)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()

        // If state has been restored, we may need to display the last detected language
        if (mLastTranslatedFrom != null && mLastTranslatedFrom == LanguageManager.AUTO &&
                mDetectedLanguage != null && translate_from_spinner.selectedItemPosition == 0) {
            displayDetectedLanguage()
        }
        updateAlternatives(view.context)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.clear_to_translate_button -> clearTextTapped()
            R.id.mic_fab_button -> {
                val safeContext = context ?: return
                val translateFrom = mTranslateFromLanguages[translate_from_spinner.selectedItemPosition]
                mListener.onSpeechToTextTapped(LanguageManager.getLanguageValue(translateFrom, safeContext))
            }
            R.id.text_to_speech_fab_button -> onTextToSpeechTapped()
            R.id.paste_fab_button -> pasteTextFromClipboard()
            R.id.copy_to_clipboard_button -> copyTranslatedTextToClipboard()
            R.id.invert_languages_button -> invertLanguages()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        when (parent.id) {
            R.id.translate_from_spinner -> {
                if (mLastTranslatedFrom != null && position != 0 && mLastTranslatedFrom == LanguageManager.AUTO) {
                    hideDetectedLanguage()
                } else {
                    checkTranslateFromLabelVisibility()
                }
                updateTranslateToSpinner()
                mListener.logEvent("changed_translate_from_language", null)
            }
            R.id.translate_to_spinner -> {
                updateTranslation(false)
                mListener.logEvent("changed_translate_to_language", null)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    // endregion


    // region Private Methods

    private fun initViews() {
        val safeContext = context ?: return

        translate_progressbar.indeterminateDrawable.setColorFilter(
                ContextCompat.getColor(safeContext, R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.SRC_IN)

        // Spinners setup
        // Default layouts : android.R.layout.simple_spinner_item, android.R.layout.simple_spinner_dropdown_item
        mTranslateFromLanguages = LanguageManager.getLanguagesStringArray(safeContext, null, true)
        mTranslateFromAdapter = ShrinkSpinnerAdapter(safeContext, R.layout.item_language_spinner, mTranslateFromLanguages)
        mTranslateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown)
        translate_from_spinner.adapter = mTranslateFromAdapter

        // We select the last used translateTo
        val lastUsedTranslateFrom = LanguageManager.getLastUsedTranslateFrom(safeContext)
        for ((index, language) in mTranslateFromLanguages.withIndex()) {
            if (LanguageManager.getLanguageValue(language, safeContext) == lastUsedTranslateFrom) {
                translate_from_spinner.setSelection(index)
                break
            }
        }

        // Init listeners for Spinners, EditText and Buttons
        translate_from_spinner.onItemSelectedListener = this
        translate_to_spinner.onItemSelectedListener = this
        clear_to_translate_button.setOnClickListener(this)
        mic_fab_button.setOnClickListener(this)
        paste_fab_button.setOnClickListener(this)
        text_to_speech_fab_button.setOnClickListener(this)
        copy_to_clipboard_button.setOnClickListener(this)
        invert_languages_button.setOnClickListener(this)
        text_to_speech_fab_button.hide()
        copy_to_clipboard_button.hide()
        invert_languages_button.hide()
        to_translate_edit_text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val toTranslateCount = to_translate_edit_text.text.toString().replace(" ", "").length
                if (toTranslateCount > 0) {
                    clear_to_translate_button.visibility = View.VISIBLE
                    mic_fab_button.hide()
                    paste_fab_button.hide()
                } else {
                    clear_to_translate_button.visibility = View.GONE
                    mic_fab_button.show()
                    if (getClipboardText() != null) {
                        paste_fab_button.show()
                    }
                }
                if (toTranslateCount > 2) {
                    updateTranslation(false)
                } else {
                    translated_edit_text.text = ""
                    alternatives_label.visibility = View.GONE
                    alternatives_linear_layout.removeAllViews()
                    mRetrySnackBar?.let {
                        it.dismiss()
                        mRetrySnackBar = null
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        translated_edit_text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!isAdded) return
                if (count > 0) {
                    copy_to_clipboard_button.show()
                    translated_edit_text.minLines = resources.getInteger(R.integer.min_lines_with_buttons)
                } else {
                    copy_to_clipboard_button.hide()
                    translated_edit_text.minLines = resources.getInteger(R.integer.min_lines)
                }
                updateTextToSpeechVisibility()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun updateTranslateToSpinner() {
        val safeContext = context ?: return
        // We update the translateTo spinner based on translateFrom selected language
        val selectedLanguage = when (mDetectedLanguage) {
            null -> LanguageManager.getLanguageValue(translate_from_spinner.selectedItem.toString(), safeContext)
            else -> mDetectedLanguage
        }
        mTranslateToLanguages = LanguageManager.getLanguagesStringArray(safeContext, selectedLanguage, false)
        val translateToAdapter = ShrinkSpinnerAdapter(safeContext, R.layout.item_language_spinner, mTranslateToLanguages)
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown)
        translate_to_spinner.adapter = translateToAdapter

        // We hide invert button if translateFrom is AUTO but language isn't detected
        if (selectedLanguage == AUTO && mDetectedLanguage == null) {
            invert_languages_button.hide()
        } else {
            invert_languages_button.show()
        }

        // We select the last used translateTo
        val lastUsedTranslateTo = LanguageManager.getLastUsedTranslateTo(safeContext)
        for ((index, language) in mTranslateToLanguages.withIndex()) {
            if (LanguageManager.getLanguageValue(language, safeContext) == lastUsedTranslateTo) {
                translate_to_spinner.setSelection(index)
                break
            }
        }
    }


    private fun updateTranslation(retry: Boolean) {
        // If text to translate is incorrect or languages not selected, we stop
        if (to_translate_edit_text.text.toString().replace(" ", "").length <= 2 ||
                translate_from_spinner.selectedItemPosition == -1 ||
                translate_to_spinner.selectedItemPosition == -1) {
            return
        }

        // We check if fields have changed since last translation
        val safeContext = context ?: return
        val toTranslate = to_translate_edit_text.text.toString()
        var translateFrom = mTranslateFromLanguages[translate_from_spinner.selectedItemPosition]
        var translateTo = mTranslateToLanguages[translate_to_spinner.selectedItemPosition]
        translateFrom = LanguageManager.getLanguageValue(translateFrom, safeContext)
        translateTo = LanguageManager.getLanguageValue(translateTo, safeContext)
        if (toTranslate == mLastTranslatedSentence &&
                translateFrom == mLastTranslatedFrom &&
                translateTo == mLastTranslatedTo &&
                !retry) {
            return
        }

        // If languages have changed, we save it to preferences
        if (translateFrom != mLastTranslatedFrom) {
            LanguageManager.saveLastUsedTranslateFrom(safeContext, translateFrom)
        }
        if (translateTo != mLastTranslatedTo) {
            LanguageManager.saveLastUsedTranslateTo(safeContext, translateTo)
        }

        // If fields have changed, we launch a new translation
        mLastTranslatedSentence = toTranslate
        mLastTranslatedFrom = translateFrom
        mLastTranslatedTo = translateTo
        val preferredLanguages = ArrayList<String>()
        preferredLanguages.add(LanguageManager.getLastUsedTranslateFrom(safeContext))
        preferredLanguages.add(LanguageManager.getLastUsedTranslateTo(safeContext))

        viewModel.translate(toTranslate, translateFrom, translateTo, preferredLanguages)
    }

    private fun displayDetectedLanguage() {
        val safeContext = context ?: return
        updateTranslateToSpinner()
        var detectedLanguage = LanguageManager.getLanguageString(mDetectedLanguage, safeContext)
        detectedLanguage = detectedLanguage + " " + getString(R.string.detected_language_label)
        (translate_from_spinner.selectedView as TextView?)?.text = detectedLanguage
        mTranslateFromAdapter.setDetectedLanguage(detectedLanguage)
        Handler().postDelayed({ checkTranslateFromLabelVisibility() }, 50)
    }

    private fun hideDetectedLanguage() {
        mDetectedLanguage = null
        mTranslateFromAdapter.clearDetectedLanguage()
        if (translate_from_spinner.selectedItemPosition == 0) {
            (translate_from_spinner.selectedView as TextView).text = mTranslateFromLanguages[0]
        }
        Handler().postDelayed({ checkTranslateFromLabelVisibility() }, 50)
    }

    private fun updateAlternatives(context: Context) {
        alternatives_linear_layout.removeAllViews()
        alternatives_label.visibility = if (mLastAlternatives?.isNotEmpty() == true) View.VISIBLE else View.GONE
        mLastAlternatives?.let {
            val margin4dp = 4.dpToPx
            val textViewColor = ContextCompat.getColor(context, R.color.textBlackColor)
            val textViewParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textViewParams.setMargins(0, margin4dp, 0, margin4dp)
            it.forEach { alternative ->
                val textView = TextView(context)
                textView.setTextColor(textViewColor)
                textView.text = alternative
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                textView.setTextIsSelectable(true)
                textView.layoutParams = textViewParams
                alternatives_linear_layout.addView(textView)
            }
        }
    }

    private fun clearTextTapped() {
        mLastTranslatedSentence = ""
        to_translate_edit_text.setText("")
        translated_edit_text.text = ""
        alternatives_label.visibility = View.GONE
        alternatives_linear_layout.removeAllViews()
        if (mDetectedLanguage != null) {
            hideDetectedLanguage()
            updateTranslateToSpinner()
        }
        mListener.logEvent("clear_text", null)
    }

    private fun copyTranslatedTextToClipboard() {
        val translatedText = translated_edit_text.text.toString()
        hideKeyboard()
        copyToClipboard(translatedText)
        Snackbar.make(clear_to_translate_button,
                R.string.copied_to_clipboard_text,
                Snackbar.LENGTH_SHORT).show()
        mListener.logEvent("copy_to_clipboard", null)
    }

    private fun pasteTextFromClipboard() {
        val clipboardText = getClipboardText()
        if (clipboardText != null) {
            setToTranslateText(clipboardText)
            mListener.logEvent("paste_from_clipboard", null)
        } else {
            paste_fab_button.hide()
            Timber.w("Clipboard is null or primary clip is empty")
        }
    }

    private fun invertLanguages() {
        val safeContext = context ?: return
        val oldTranslateFrom = when (mDetectedLanguage) {
            null -> LanguageManager.getLanguageValue(mTranslateFromLanguages[translate_from_spinner.selectedItemPosition], safeContext)
            else -> mDetectedLanguage ?: ""
        }
        val oldTranslateTo = mTranslateToLanguages[translate_to_spinner.selectedItemPosition]

        LanguageManager.saveLastUsedTranslateTo(safeContext, oldTranslateFrom)
        for ((index, language) in mTranslateFromLanguages.withIndex()) {
            if (language == oldTranslateTo) {
                translate_from_spinner.setSelection(index)
                val translateFromValue = LanguageManager.getLanguageValue(language, safeContext)
                LanguageManager.saveLastUsedTranslateFrom(safeContext, translateFromValue)
                break
            }
        }

        ViewCompat.animate(invert_languages_button)
                .rotation(180f)
                .withLayer().setDuration(350)
                .setInterpolator(OvershootInterpolator())
                .withEndAction { invert_languages_button.rotation = 0f }
                .startDelay = 75

        mListener.logEvent("invert_languages", null)
    }

    private fun checkTranslateFromLabelVisibility() {
        val textViewLocation = IntArray(2)
        translate_from_text_view.getLocationOnScreen(textViewLocation)
        translate_from_text_view.visibility = if (textViewLocation[0] > 8.dpToPx) View.VISIBLE else View.INVISIBLE
    }

    private fun updateTextToSpeechVisibility() {
        if (mTextToSpeechInitialized && mLastTranslatedTo != null && !translated_edit_text.text.toString().isEmpty()) {
            val locale = LanguageManager.getLocaleFromLanguageValue(mLastTranslatedTo, mTextToSpeech)
            if (mTextToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                text_to_speech_fab_button.show()
                return
            }
        }
        text_to_speech_fab_button.hide()
    }

    @Suppress("DEPRECATION")
    private fun onTextToSpeechTapped() {
        if (!mTextToSpeechInitialized || mTextToSpeech.isSpeaking || translated_edit_text.text.toString().isEmpty() || mLastTranslatedTo == null) {
            return
        }
        if (mListener.currentMediaVolume > 0) {
            mTextToSpeech.language = LanguageManager.getLocaleFromLanguageValue(mLastTranslatedTo, mTextToSpeech)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTextToSpeech.speak(translated_edit_text.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                mTextToSpeech.speak(translated_edit_text.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
            }
        } else {
            Snackbar.make(clear_to_translate_button, R.string.volume_off_label, Snackbar.LENGTH_SHORT).show()
        }
        mListener.logEvent("text_to_speech", null)
    }

    // endregion


    // region Public Methods

    fun setToTranslateText(text: String?) {
        to_translate_edit_text.setText(text)
        to_translate_edit_text.setSelection(text?.length ?: 0)
    }

    // endregion

    interface OnFragmentInteractionListener {
        val currentMediaVolume: Int
        fun onSpeechToTextTapped(selectedLocale: String)
        fun logEvent(event: String, bundle: Bundle?)
    }
}
