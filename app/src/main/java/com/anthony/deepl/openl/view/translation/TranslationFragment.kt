package com.anthony.deepl.openl.view.translation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
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

    private val viewModel by sharedViewModel<TranslationViewModel>()

    private var retrySnackBar: Snackbar? = null
    private var translateFromLanguages: Array<String> = arrayOf()
    private var translateToLanguages: Array<String> = arrayOf()
    private var textToSpeechInitialized: Boolean = false

    private lateinit var listener: OnFragmentInteractionListener
    private lateinit var translateFromAdapter: ShrinkSpinnerAdapter<*>
    private lateinit var textToSpeech: TextToSpeech

    // region Overridden methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeechInitialized = true
                updateTextToSpeechVisibility()
            }
        })
        textToSpeechInitialized = false
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getLiveTranslationResponse().observe(this, Observer { resource ->
            if (resource !is FailureResource) {
                retrySnackBar?.let {
                    it.dismiss()
                    retrySnackBar = null
                }
            }
            when (resource) {
                is FailureResource -> {
                    translate_progressbar.visibility = View.GONE
                    translated_edit_text.text = ""

                    if (retrySnackBar == null) {
                        view?.let { view ->
                            retrySnackBar = Snackbar.make(view, R.string.snack_bar_retry_label, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.snack_bar_retry_button) {
                                        updateTranslation(true)
                                        viewModel.logEvent("retry_snack_bar_tapped", null)
                                    }
                            retrySnackBar?.show()
                        }
                        viewModel.logEvent("retry_snack_bar_displayed", null)
                    }
                }
                is SuccessResource -> {
                    translated_edit_text.text = resource.data.response.getBestTranslation()
                    updateAlternatives(resource.data.response.getAlternateTranslations().orEmpty())
                    if (translate_from_spinner.selectedItemPosition == 0 && resource.data.response.sourceLanguage != null) {
                        displayDetectedLanguage(resource.data.response.sourceLanguage!!)
                    }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.clear_to_translate_button -> clearTextTapped()
            R.id.mic_fab_button -> {
                val safeContext = context ?: return
                val translateFrom = translateFromLanguages[translate_from_spinner.selectedItemPosition]
                listener.onSpeechToTextTapped(LanguageManager.getLanguageValue(translateFrom, safeContext))
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
                if (position != 0) {
                    hideDetectedLanguage()
                } else {
                    checkTranslateFromLabelVisibility()
                }
                updateTranslateToSpinner()
                viewModel.logEvent("changed_translate_from_language", null)
            }
            R.id.translate_to_spinner -> {
                updateTranslation(false)
                viewModel.logEvent("changed_translate_to_language", null)
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
        translateFromLanguages = LanguageManager.getLanguagesStringArray(safeContext, null, true)
        translateFromAdapter = ShrinkSpinnerAdapter(safeContext, R.layout.item_language_spinner, translateFromLanguages)
        translateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown)
        translate_from_spinner.adapter = translateFromAdapter

        // We select the last used translateTo
        val lastUsedTranslateFrom = LanguageManager.getLastUsedTranslateFrom(safeContext)
        for ((index, language) in translateFromLanguages.withIndex()) {
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
        to_translate_edit_text.onTextChanged { s ->
            val toTranslateCount = s.replace(" ", "").length
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
                retrySnackBar?.let {
                    it.dismiss()
                    retrySnackBar = null
                }
            }
        }

        translated_edit_text.onTextChanged { s ->
            if (!isAdded) return@onTextChanged
            if (s.isNotEmpty()) {
                copy_to_clipboard_button.show()
                translated_edit_text.minLines = resources.getInteger(R.integer.min_lines_with_buttons)
            } else {
                copy_to_clipboard_button.hide()
                translated_edit_text.minLines = resources.getInteger(R.integer.min_lines)
            }
            updateTextToSpeechVisibility()
        }
    }

    private fun updateTranslateToSpinner() {
        val safeContext = context ?: return
        // We update the translateTo spinner based on translateFrom selected language
        val selectedLanguage = when (viewModel.detectedLanguage) {
            null -> LanguageManager.getLanguageValue(translate_from_spinner.selectedItem.toString(), safeContext)
            else -> viewModel.detectedLanguage
        }
        translateToLanguages = LanguageManager.getLanguagesStringArray(safeContext, selectedLanguage, false)
        val translateToAdapter = ShrinkSpinnerAdapter(safeContext, R.layout.item_language_spinner, translateToLanguages)
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown)
        translate_to_spinner.adapter = translateToAdapter

        // We hide invert button if translateFrom is AUTO but language isn't detected
        if (selectedLanguage == AUTO && viewModel.detectedLanguage == null) {
            invert_languages_button.hide()
        } else {
            invert_languages_button.show()
        }

        // We select the last used translateTo
        val lastUsedTranslateTo = LanguageManager.getLastUsedTranslateTo(safeContext)
        for ((index, language) in translateToLanguages.withIndex()) {
            if (LanguageManager.getLanguageValue(language, safeContext) == lastUsedTranslateTo) {
                translate_to_spinner.setSelection(index)
                break
            }
        }
    }

    private fun updateTranslation(retry: Boolean) {
        val safeContext = context ?: return

        // If text to translate is incorrect or languages not selected, we stop
        if (to_translate_edit_text.text.toString().replace(" ", "").length <= 2 ||
                translate_from_spinner.selectedItemPosition == -1 ||
                translate_to_spinner.selectedItemPosition == -1) {
            return
        }
        val toTranslate = to_translate_edit_text.text.toString()
        var translateFrom = translateFromLanguages[translate_from_spinner.selectedItemPosition]
        var translateTo = translateToLanguages[translate_to_spinner.selectedItemPosition]
        translateFrom = LanguageManager.getLanguageValue(translateFrom, safeContext)
        translateTo = LanguageManager.getLanguageValue(translateTo, safeContext)
        viewModel.requestTranslation(toTranslate, translateFrom, translateTo, retry)
    }

    private fun displayDetectedLanguage(detectedLanguageValue: String) {
        val safeContext = context ?: return
        updateTranslateToSpinner()
        val detectedLanguage = LanguageManager.getLanguageString(detectedLanguageValue, safeContext) + " " + getString(R.string.detected_language_label)
        (translate_from_spinner.selectedView as TextView?)?.text = detectedLanguage
        translateFromAdapter.setDetectedLanguage(detectedLanguage)
        Handler().postDelayed({ checkTranslateFromLabelVisibility() }, 50)
    }

    private fun hideDetectedLanguage() {
        viewModel.detectedLanguage = null
        translateFromAdapter.clearDetectedLanguage()
        if (translate_from_spinner.selectedItemPosition == 0) {
            (translate_from_spinner.selectedView as TextView).text = translateFromLanguages[0]
        }
        Handler().postDelayed({ checkTranslateFromLabelVisibility() }, 50)
    }

    private fun updateAlternatives(alternatives: List<String>) {
        if (!isAdded) return
        alternatives_linear_layout.removeAllViews()
        alternatives_label.visibility = if (alternatives.isNotEmpty()) View.VISIBLE else View.GONE
        alternatives.let {
            val margin4dp = 4.dpToPx
            val textViewColor = ContextCompat.getColor(requireContext(), R.color.textBlackColor)
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
        viewModel.clearLastResponse()
        to_translate_edit_text.setText("")
        translated_edit_text.text = ""
        alternatives_label.visibility = View.GONE
        alternatives_linear_layout.removeAllViews()
        if (viewModel.detectedLanguage != null) {
            hideDetectedLanguage()
            updateTranslateToSpinner()
        }
        viewModel.logEvent("clear_text", null)
    }

    private fun copyTranslatedTextToClipboard() {
        val translatedText = translated_edit_text.text.toString()
        hideKeyboard()
        copyToClipboard(translatedText)
        Snackbar.make(clear_to_translate_button,
                R.string.copied_to_clipboard_text,
                Snackbar.LENGTH_SHORT).show()
        viewModel.logEvent("copy_to_clipboard", null)
    }

    private fun pasteTextFromClipboard() {
        val clipboardText = getClipboardText()
        if (clipboardText != null) {
            setToTranslateText(clipboardText)
            viewModel.logEvent("paste_from_clipboard", null)
        } else {
            paste_fab_button.hide()
            Timber.w("Clipboard is null or primary clip is empty")
        }
    }

    private fun invertLanguages() {
        val safeContext = context ?: return
        val oldTranslateFrom = when (viewModel.detectedLanguage) {
            null -> LanguageManager.getLanguageValue(translateFromLanguages[translate_from_spinner.selectedItemPosition], safeContext)
            else -> viewModel.detectedLanguage ?: ""
        }
        val oldTranslateTo = translateToLanguages[translate_to_spinner.selectedItemPosition]

        LanguageManager.saveLastUsedTranslateTo(safeContext, oldTranslateFrom)
        for ((index, language) in translateFromLanguages.withIndex()) {
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

        viewModel.logEvent("invert_languages", null)
    }

    private fun checkTranslateFromLabelVisibility() {
        val textViewLocation = IntArray(2)
        translate_from_text_view.getLocationOnScreen(textViewLocation)
        translate_from_text_view.visibility = if (textViewLocation[0] > 8.dpToPx) View.VISIBLE else View.INVISIBLE
    }

    private fun updateTextToSpeechVisibility() {
        val lastTranslatedTo = viewModel.getLastTranslation()?.request?.toLanguage
        if (textToSpeechInitialized && lastTranslatedTo != null && !translated_edit_text.text.toString().isEmpty()) {
            val locale = LanguageManager.getLocaleFromLanguageValue(lastTranslatedTo, textToSpeech)
            if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                text_to_speech_fab_button.show()
                return
            }
        }
        text_to_speech_fab_button.hide()
    }

    @Suppress("DEPRECATION")
    private fun onTextToSpeechTapped() {
        val lastTranslatedTo = viewModel.getLastTranslation()?.request?.toLanguage
        if (!textToSpeechInitialized || textToSpeech.isSpeaking || translated_edit_text.text.toString().isEmpty() || lastTranslatedTo == null) {
            return
        }
        if (listener.currentMediaVolume > 0) {
            textToSpeech.language = LanguageManager.getLocaleFromLanguageValue(lastTranslatedTo, textToSpeech)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(translated_edit_text.text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                textToSpeech.speak(translated_edit_text.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
            }
        } else {
            Snackbar.make(clear_to_translate_button, R.string.volume_off_label, Snackbar.LENGTH_SHORT).show()
        }
        viewModel.logEvent("text_to_speech", null)
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
    }
}
