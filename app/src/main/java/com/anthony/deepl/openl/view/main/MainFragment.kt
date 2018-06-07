package com.anthony.deepl.openl.view.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
import android.widget.LinearLayout.LayoutParams

import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.manager.LanguageManager
import com.anthony.deepl.openl.model.TranslationRequest
import com.anthony.deepl.openl.model.TranslationResponse
import com.anthony.deepl.openl.R
import com.anthony.deepl.openl.util.AndroidUtils

import java.util.ArrayList

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

import com.anthony.deepl.openl.manager.LanguageManager.AUTO
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    companion object {
        private const val INSTANCE_TRANSLATED_FROM_KEY = "last_translated_from"
        private const val INSTANCE_TRANSLATED_TO_KEY = "last_translated_to"
        private const val INSTANCE_TRANSLATED_SENTENCE_KEY = "last_translated_sentence"
        private const val INSTANCE_DETECTED_LANGUAGE_KEY = "detected_language"
        private const val INSTANCE_LAST_ALTERNATIVES_KEY = "last_alternatives"
    }

    private var mRetrySnackBar: Snackbar? = null
    private var mClipboardManager: ClipboardManager? = null
    private var mTranslateFromLanguages: Array<String> = arrayOf()
    private var mTranslateToLanguages: Array<String> = arrayOf()
    private var mLastTranslatedSentence: String? = null
    private var mLastTranslatedFrom: String? = null
    private var mLastTranslatedTo: String? = null
    private var mLastAlternatives: List<String>? = null
    private var mDetectedLanguage: String? = null
    private var mTranslationInProgress: Boolean = false
    private var mTextToSpeechInitialized: Boolean = false

    private lateinit var mListener: OnFragmentInteractionListener
    private lateinit var mTranslateFromAdapter: ShrinkSpinnerAdapter<*>
    private lateinit var mTextToSpeech: TextToSpeech

    private val mDeepLService: DeepLService by lazy {
        val retrofit = Retrofit.Builder()
                .baseUrl(DeepLService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        retrofit.create(DeepLService::class.java)
    }

    // region Overridden methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mClipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

    override fun onResume() {
        super.onResume()
        if (AndroidUtils.getClipboardText(mClipboardManager) != null && to_translate_edit_text.text.isEmpty()) {
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
        initViews()

        // If state has been restored, we may need to display the last detected language
        if (mLastTranslatedFrom != null && mLastTranslatedFrom == LanguageManager.AUTO &&
                mDetectedLanguage != null && translate_from_spinner.selectedItemPosition == 0) {
            displayDetectedLanguage()
        }
        updateAlternatives(inflater.context)

        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onAttach(context: Context?) {
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

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
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
                updateTranslation()
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
                    if (AndroidUtils.getClipboardText(mClipboardManager) != null) {
                        paste_fab_button.show()
                    }
                }
                if (toTranslateCount > 2) {
                    updateTranslation()
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

    private fun updateTranslation() {
        // If a translation is in progress, we return directly
        if (mTranslationInProgress || to_translate_edit_text.text.toString().replace(" ", "").length <= 2) {
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
                translateTo == mLastTranslatedTo) {
            return
        }

        // If languages have changed, we save it to preferences
        if (translateFrom != mLastTranslatedFrom) {
            LanguageManager.saveLastUsedTranslateFrom(context!!, translateFrom)
        }
        if (translateTo != mLastTranslatedTo) {
            LanguageManager.saveLastUsedTranslateTo(context!!, translateTo)
        }

        // If fields have changed, we launch a new translation
        mTranslationInProgress = true
        mLastTranslatedSentence = toTranslate
        mLastTranslatedFrom = translateFrom
        mLastTranslatedTo = translateTo
        val preferredLanguages = ArrayList<String>()
        preferredLanguages.add(LanguageManager.getLastUsedTranslateFrom(context!!))
        preferredLanguages.add(LanguageManager.getLastUsedTranslateTo(safeContext))

        val request = TranslationRequest(
                toTranslate,
                translateFrom,
                translateTo,
                preferredLanguages,
                "2.0",
                "LMT_handle_jobs")
        translate_progressbar.visibility = View.VISIBLE
        val call = mDeepLService.translateText(request)
        call.enqueue(object : Callback<TranslationResponse> {
            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                val translationResponse = response.body()
                if (translationResponse == null) {
                    onFailure(call, Exception("Translation response body is null"))
                    return
                }

                // Main translation
                mRetrySnackBar?.let {
                    it.dismiss()
                    mRetrySnackBar = null
                }
                translate_progressbar.visibility = View.GONE
                mTranslationInProgress = false
                translated_edit_text.text = translationResponse.getBestTranslation(request.lineBreakPositions)

                // Alternative translations
                mLastAlternatives = translationResponse.getAlternateTranslations()
                updateAlternatives(safeContext)

                // Reporting
                val params = Bundle()
                params.putString("translate_from", mLastTranslatedFrom)
                params.putString("translate_to", mLastTranslatedTo)
                mListener.logEvent("translation", params)

                // We call the method again to check if something has changed since we've launched the network call
                updateTranslation()

                // If AUTO is selected, we update the label with the detected language and the translateTo spinner
                if (isAdded && translate_from_spinner.selectedItemPosition == 0) {
                    mDetectedLanguage = translationResponse.sourceLanguage
                    displayDetectedLanguage()
                } else {
                    mDetectedLanguage = null
                }
            }

            override fun onFailure(call: Call<TranslationResponse>, t: Throwable) {
                translate_progressbar.visibility = View.GONE
                mLastTranslatedSentence = ""
                mTranslationInProgress = false
                translated_edit_text.text = ""

                if (mRetrySnackBar == null) {
                    view?.let {
                        mListener.logEvent("retry_snack_bar_displayed", null)
                        mRetrySnackBar = Snackbar.make(it, R.string.snack_bar_retry_label, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.snack_bar_retry_button) {
                                    mRetrySnackBar?.let {
                                        it.dismiss()
                                        mRetrySnackBar = null
                                    }
                                    updateTranslation()
                                    mListener.logEvent("retry_snack_bar_tapped", null)
                                }
                        mRetrySnackBar?.show()
                    }
                }
                Timber.e(t)
            }
        })
    }

    private fun displayDetectedLanguage() {
        val safeContext = context ?: return
        updateTranslateToSpinner()
        var detectedLanguage = LanguageManager.getLanguageString(mDetectedLanguage, safeContext)
        detectedLanguage = detectedLanguage + " " + getString(R.string.detected_language_label)
        (translate_from_spinner.selectedView as TextView).text = detectedLanguage
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
            val margin4dp = AndroidUtils.convertDpToPixel(4f, context).toInt()
            val textViewColor = ContextCompat.getColor(context, R.color.textBlackColor)
            val textViewParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textViewParams.setMargins(0, margin4dp, 0, margin4dp)
            it.forEach {
                val textView = TextView(context)
                textView.setTextColor(textViewColor)
                textView.text = it
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
        if (mClipboardManager != null) {
            // First we close the keyboard
            val view = activity?.currentFocus
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }

            // Then we clip the translated text and display confirmation Snackbar
            val clip = ClipData.newPlainText(translatedText, translatedText)
            mClipboardManager?.primaryClip = clip
            Snackbar.make(clear_to_translate_button,
                    R.string.copied_to_clipboard_text,
                    Snackbar.LENGTH_SHORT).show()
            mListener.logEvent("copy_to_clipboard", null)
        } else {
            Timber.w("Clipboard is null and shouldn't be")
        }
    }

    private fun pasteTextFromClipboard() {
        val clipboardText = AndroidUtils.getClipboardText(mClipboardManager)
        if (clipboardText != null) {
            setToTranslateText(clipboardText)
            mListener.logEvent("paste_from_clipboard", null)
        } else {
            paste_fab_button.hide()
            Timber.w(if (mClipboardManager != null) "Clipboard primary clip is empty, paste fab should be hidden" else "Clipboard is null and shouldn't be")
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

        val interpolator = OvershootInterpolator()
        ViewCompat.animate(invert_languages_button).rotation(180f).withLayer().setDuration(350).setInterpolator(interpolator).withEndAction { invert_languages_button.rotation = 0f }.startDelay = 75

        mListener.logEvent("invert_languages", null)
    }

    private fun checkTranslateFromLabelVisibility() {
        val safeContext = context ?: return
        val textViewLocation = IntArray(2)
        val eightDpToPixelValue = AndroidUtils.convertDpToPixel(8f, safeContext).toInt()
        translate_from_text_view.getLocationOnScreen(textViewLocation)
        translate_from_text_view.visibility = if (textViewLocation[0] > eightDpToPixelValue) View.VISIBLE else View.INVISIBLE
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

    private fun onTextToSpeechTapped() {
        if (!mTextToSpeechInitialized || mTextToSpeech.isSpeaking || translated_edit_text.text.toString().isEmpty() || mLastTranslatedTo == null) {
            return
        }
        if (mListener.currentMediaVolume > 0) {
            mTextToSpeech.language = LanguageManager.getLocaleFromLanguageValue(mLastTranslatedTo, mTextToSpeech)
            mTextToSpeech.speak(translated_edit_text.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
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
