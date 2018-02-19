package com.anthony.deepl.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.anthony.deepl.R;
import com.anthony.deepl.adapter.ShrinkSpinnerAdapter;
import com.anthony.deepl.backend.DeepLService;
import com.anthony.deepl.manager.LanguageManager;
import com.anthony.deepl.model.TranslationRequest;
import com.anthony.deepl.model.TranslationResponse;
import com.anthony.deepl.util.AndroidUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static com.anthony.deepl.manager.LanguageManager.AUTO;

public class MainFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private OnFragmentInteractionListener mListener;

    private AppCompatSpinner mTranslateFromSpinner;
    private AppCompatSpinner mTranslateToSpinner;
    private EditText mToTranslateEditText;
    private TextView mTranslateFromTextView;
    private TextView mTranslatedTextView;
    private ProgressBar mTranslateProgressbar;
    private ImageButton mClearButton;
    private FloatingActionButton mMicFab;
    private FloatingActionButton mPasteFab;
    private FloatingActionButton mSpeakerFab;
    private FloatingActionButton mCopyToClipboardFab;
    private FloatingActionButton mInvertLanguagesFab;
    private TextView mAlternativesLabel;
    private LinearLayout mAlternativesLinearLayout;
    private Snackbar mRetrySnackBar;

    private ShrinkSpinnerAdapter mTranslateFromAdapter;
    private DeepLService mDeepLService;
    private ClipboardManager mClipboardManager;
    private TextToSpeech mTextToSpeech;
    private String mTranslateFromLanguages[];
    private String mTranslateToLanguages[];
    private String mLastTranslatedSentence;
    private String mLastTranslatedFrom;
    private String mLastTranslatedTo;
    private String mDetectedLanguage;
    private boolean mTranslationInProgress;
    private boolean mTextToSpeechInitialized;

    public MainFragment() {
    }

    // region Overridden methods

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DeepLService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mDeepLService = retrofit.create(DeepLService.class);
        mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mTextToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    mTextToSpeechInitialized = true;
                }
            }
        });
        mTextToSpeechInitialized = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mClipboardManager != null &&
                mClipboardManager.hasPrimaryClip() &&
                mClipboardManager.getPrimaryClip().getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                mToTranslateEditText.getText().length() <= 0) {
            mPasteFab.show();
        }
        else {
            mPasteFab.hide();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.clear_to_translate_button:
                clearTextTapped();
                break;
            case R.id.mic_fab_button:
                String translateFrom = mTranslateFromLanguages[mTranslateFromSpinner.getSelectedItemPosition()];
                mListener.onSpeechToTextTapped(LanguageManager.getLanguageValue(translateFrom, getContext()));
                break;
            case R.id.text_to_speech_fab_button:
                onTextToSpeechTapped();
                break;
            case R.id.paste_fab_button:
                pasteTextFromClipboard();
                break;
            case R.id.copy_to_clipboard_button:
                copyTranslatedTextToClipboard();
                break;
            case R.id.invert_languages_button:
                invertLanguages();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int parentId = parent.getId();
        switch (parentId) {
            case R.id.translate_from_spinner:
                hideDetectedLanguage();
                updateTranslateToSpinner();
                mListener.logEvent("changed_translate_from_language", null);
                break;
            case R.id.translate_to_spinner:
                updateTranslation();
                mListener.logEvent("changed_translate_to_language", null);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // endregion


    // region Private Methods

    private void initViews(View view) {
        // Views retrieval
        mTranslateFromSpinner = view.findViewById(R.id.translate_from_spinner);
        mTranslateToSpinner = view.findViewById(R.id.translate_to_spinner);
        mToTranslateEditText = view.findViewById(R.id.to_translate_edit_text);
        mTranslateFromTextView = view.findViewById(R.id.translate_from_text_view);
        mTranslatedTextView = view.findViewById(R.id.translated_edit_text);
        mTranslateProgressbar = view.findViewById(R.id.translate_progressbar);
        mClearButton = view.findViewById(R.id.clear_to_translate_button);
        mMicFab = view.findViewById(R.id.mic_fab_button);
        mPasteFab = view.findViewById(R.id.paste_fab_button);
        mSpeakerFab = view.findViewById(R.id.text_to_speech_fab_button);
        mCopyToClipboardFab = view.findViewById(R.id.copy_to_clipboard_button);
        mInvertLanguagesFab = view.findViewById(R.id.invert_languages_button);
        mAlternativesLabel = view.findViewById(R.id.alternatives_label);
        mAlternativesLinearLayout = view.findViewById(R.id.alternatives_linear_layout);

        mTranslateProgressbar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.SRC_IN);

        // Spinners setup
        // Default layouts : android.R.layout.simple_spinner_item, android.R.layout.simple_spinner_dropdown_item
        mTranslateFromLanguages = LanguageManager.getLanguagesStringArray(getContext(), null, true);
        mTranslateFromAdapter = new ShrinkSpinnerAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateFromLanguages);
        mTranslateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateFromSpinner.setAdapter(mTranslateFromAdapter);

        // We select the last used translateTo
        Context context = getContext();
        String lastUsedTranslateFrom = LanguageManager.getLastUsedTranslateFrom(getContext());
        for (int i = 0, size = mTranslateFromLanguages.length; i < size; i++) {
            if (LanguageManager.getLanguageValue(mTranslateFromLanguages[i], context).equals(lastUsedTranslateFrom)) {
                mTranslateFromSpinner.setSelection(i);
                break;
            }
        }

        // Init listeners for Spinners, EditText and Buttons
        mTranslateFromSpinner.setOnItemSelectedListener(this);
        mTranslateToSpinner.setOnItemSelectedListener(this);
        mClearButton.setOnClickListener(this);
        mMicFab.setOnClickListener(this);
        mPasteFab.setOnClickListener(this);
        mSpeakerFab.setOnClickListener(this);
        mCopyToClipboardFab.setOnClickListener(this);
        mInvertLanguagesFab.setOnClickListener(this);
        mSpeakerFab.hide();
        mCopyToClipboardFab.hide();
        mInvertLanguagesFab.hide();
        mToTranslateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int toTranslateCount = mToTranslateEditText.getText().toString().replace(" ", "").length();
                if (toTranslateCount > 0) {
                    mClearButton.setVisibility(View.VISIBLE);
                    mMicFab.hide();
                    mPasteFab.hide();
                } else {
                    mClearButton.setVisibility(View.GONE);
                    mMicFab.show();
                    if (mClipboardManager != null &&
                            mClipboardManager.hasPrimaryClip() &&
                            mClipboardManager.getPrimaryClip().getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        mPasteFab.show();
                    }
                }
                if (toTranslateCount > 2) {
                    updateTranslation();
                } else {
                    mTranslatedTextView.setText("");
                    mAlternativesLabel.setVisibility(View.GONE);
                    mAlternativesLinearLayout.removeAllViews();
                    if (mRetrySnackBar != null) {
                        mRetrySnackBar.dismiss();
                        mRetrySnackBar = null;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mTranslatedTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    Locale locale = LanguageManager.getLocaleFromLanguageValue(mLastTranslatedTo);
                    if (mTextToSpeechInitialized && mTextToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                        mSpeakerFab.show();
                    }
                    mCopyToClipboardFab.show();
                    mTranslatedTextView.setMinLines(3);
                } else {
                    mSpeakerFab.hide();
                    mCopyToClipboardFab.hide();
                    mTranslatedTextView.setMinLines(4);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateTranslateToSpinner() {
        // We update the translateTo spinner based on translateFrom selected language
        String selectedLanguage = mDetectedLanguage != null ?
                mDetectedLanguage :
                LanguageManager.getLanguageValue(mTranslateFromSpinner.getSelectedItem().toString(), getContext());
        mTranslateToLanguages = LanguageManager.getLanguagesStringArray(getContext(), selectedLanguage, false);
        ShrinkSpinnerAdapter<String> translateToAdapter = new ShrinkSpinnerAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateToLanguages);
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateToSpinner.setAdapter(translateToAdapter);

        // We hide invert button if translateFrom is AUTO but language isn't detected
        if (selectedLanguage.equals(AUTO) && mDetectedLanguage == null) {
            mInvertLanguagesFab.hide();
        } else {
            mInvertLanguagesFab.show();
        }

        // We select the last used translateTo
        Context context = getContext();
        String lastUsedTranslateTo = LanguageManager.getLastUsedTranslateTo(getContext());
        for (int i = 0, size = mTranslateToLanguages.length; i < size; i++) {
            if (LanguageManager.getLanguageValue(mTranslateToLanguages[i], context).equals(lastUsedTranslateTo)) {
                mTranslateToSpinner.setSelection(i);
                break;
            }
        }
    }

    private void updateTranslation() {
        // If a translation is in progress, we return directly
        if (mTranslationInProgress ||
                mTranslateToLanguages == null ||
                mToTranslateEditText.getText().toString().replace(" ", "").length() <= 2) {
            return;
        }

        // We check if fields have changed since last translation
        Context context = getContext();
        String toTranslate = mToTranslateEditText.getText().toString();
        String translateFrom = mTranslateFromLanguages[mTranslateFromSpinner.getSelectedItemPosition()];
        String translateTo = mTranslateToLanguages[mTranslateToSpinner.getSelectedItemPosition()];
        translateFrom = LanguageManager.getLanguageValue(translateFrom, context);
        translateTo = LanguageManager.getLanguageValue(translateTo, context);
        if (toTranslate.equals(mLastTranslatedSentence) &&
                translateFrom.equals(mLastTranslatedFrom) &&
                translateTo.equals(mLastTranslatedTo)) {
            return;
        }

        // If languages have changed, we save it to preferences
        if (!translateFrom.equals(mLastTranslatedFrom)) {
            LanguageManager.saveLastUsedTranslateFrom(context, translateFrom);
        }
        if (!translateTo.equals(mLastTranslatedTo)) {
            LanguageManager.saveLastUsedTranslateTo(context, translateTo);
        }

        // If fields have changed, we launch a new translation
        mTranslationInProgress = true;
        mLastTranslatedSentence = toTranslate;
        mLastTranslatedFrom = translateFrom;
        mLastTranslatedTo = translateTo;
        List<String> preferredLanguages = new ArrayList<>();
        preferredLanguages.add(LanguageManager.getLastUsedTranslateFrom(context));
        preferredLanguages.add(LanguageManager.getLastUsedTranslateTo(context));

        TranslationRequest request = new TranslationRequest(
                toTranslate,
                translateFrom,
                translateTo,
                preferredLanguages);
        mTranslateProgressbar.setVisibility(View.VISIBLE);
        Call<TranslationResponse> call = mDeepLService.translateText(request);
        call.enqueue(new Callback<TranslationResponse>() {
            @Override
            public void onResponse(@NonNull Call<TranslationResponse> call, @NonNull Response<TranslationResponse> response) {
                Context context = getContext();
                if (context == null) return;
                TranslationResponse translationResponse = response.body();
                if (translationResponse == null) {
                    onFailure(call, new Exception("Translation response body is null"));
                    return;
                }

                // Main translation
                if (mRetrySnackBar != null) {
                    mRetrySnackBar.dismiss();
                    mRetrySnackBar = null;
                }
                mTranslateProgressbar.setVisibility(View.GONE);
                mTranslationInProgress = false;
                mTranslatedTextView.setText(translationResponse.getBestTranslation());

                // Alternative translations
                List<String> alternatives = translationResponse.getOtherResults();
                mAlternativesLinearLayout.removeAllViews();
                mAlternativesLabel.setVisibility((alternatives != null && alternatives.size() > 0) ? View.VISIBLE : View.GONE);
                if (alternatives != null) {
                    int margin4dp = (int) AndroidUtils.convertDpToPixel(4, context);
                    for (int i = 0, size = alternatives.size(); i < size; i++) {
                        TextView textView = new TextView(context);
                        textView.setTextColor(ContextCompat.getColor(context, R.color.textBlackColor));
                        textView.setText(alternatives.get(i));
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                        textView.setTextIsSelectable(true);
                        LayoutParams textViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        textViewParams.setMargins(0, margin4dp, 0, margin4dp);
                        textView.setLayoutParams(textViewParams);
                        mAlternativesLinearLayout.addView(textView);
                    }
                }

                // Reporting
                Bundle params = new Bundle();
                params.putString("translate_from", mLastTranslatedFrom);
                params.putString("translate_to", mLastTranslatedTo);
                mListener.logEvent("translation", params);

                // We call the method again to check if something has changed since we've launched the network call
                updateTranslation();

                // If AUTO is selected, we update the label with the detected language and the translateTo spinner
                if (isAdded() && mTranslateFromSpinner.getSelectedItemPosition() == 0) {
                    mDetectedLanguage = translationResponse.getSourceLanguage();
                    displayDetectedLanguage();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TranslationResponse> call, @NonNull Throwable t) {
                mTranslateProgressbar.setVisibility(View.GONE);
                mLastTranslatedSentence = "";
                mTranslationInProgress = false;
                mTranslatedTextView.setText("");

                if (mRetrySnackBar == null) {
                    View view = getView();
                    if (view != null) {
                        mListener.logEvent("retry_snack_bar_displayed", null);
                        mRetrySnackBar = Snackbar.make(view, R.string.snack_bar_retry_label, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.snack_bar_retry_button, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mRetrySnackBar != null) {
                                            mRetrySnackBar.dismiss();
                                            mRetrySnackBar = null;
                                        }
                                        updateTranslation();
                                        mListener.logEvent("retry_snack_bar_tapped", null);
                                    }
                                });
                        mRetrySnackBar.show();
                    }
                }
                Timber.e(t);
            }
        });
    }

    private void displayDetectedLanguage() {
        updateTranslateToSpinner();
        String detectedLanguage = LanguageManager.getLanguageString(mDetectedLanguage, getContext());
        detectedLanguage = detectedLanguage.concat(" ").concat(getString(R.string.detected_language_label));
        TextView spinnerTextView = (TextView) mTranslateFromSpinner.getSelectedView();
        spinnerTextView.setText(detectedLanguage);
        mTranslateFromAdapter.setDetectedLanguage(detectedLanguage);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkTranslateFromLabelVisibility();
            }
        }, 50);
    }

    private void hideDetectedLanguage() {
        mDetectedLanguage = null;
        mTranslateFromAdapter.clearDetectedLanguage();
        if (mTranslateFromSpinner.getSelectedItemPosition() == 0) {
            TextView spinnerTextView = (TextView) mTranslateFromSpinner.getSelectedView();
            if (spinnerTextView != null) {
                spinnerTextView.setText(mTranslateFromLanguages[0]);
            }
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkTranslateFromLabelVisibility();
            }
        }, 50);
    }

    private void clearTextTapped() {
        mLastTranslatedSentence = "";
        mToTranslateEditText.setText("");
        mTranslatedTextView.setText("");
        mAlternativesLabel.setVisibility(View.GONE);
        mAlternativesLinearLayout.removeAllViews();
        if (mDetectedLanguage != null) {
            hideDetectedLanguage();
            updateTranslateToSpinner();
        }
        mListener.logEvent("clear_text", null);
    }

    private void copyTranslatedTextToClipboard() {
        String translatedText = mTranslatedTextView.getText().toString();
        if (mClipboardManager != null) {
            // First we close the keyboard
            Activity mainActivity = getActivity();
            View view = mainActivity.getCurrentFocus();
            InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (view != null && imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            // Then we clip the translated text and display confirmation Snackbar
            ClipData clip = ClipData.newPlainText(translatedText, translatedText);
            mClipboardManager.setPrimaryClip(clip);
            Snackbar.make(mClearButton,
                    R.string.copied_to_clipboard_text,
                    Snackbar.LENGTH_SHORT).show();
            mListener.logEvent("copy_to_clipboard", null);
        } else {
            Timber.w("Clipboard is null and shouldn't be");
        }
    }

    private void pasteTextFromClipboard() {
        if (mClipboardManager != null &&
                mClipboardManager.hasPrimaryClip() &&
                mClipboardManager.getPrimaryClip().getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            setToTranslateText(mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
            mListener.logEvent("paste_from_clipboard", null);
        } else {
            mPasteFab.hide();
            Timber.w(mClipboardManager != null ? "Clipboard primary clip is empty, paste fab should be hidden" : "Clipboard is null and shouldn't be");
        }
    }

    private void invertLanguages() {
        Context context = getContext();
        String oldTranslateFrom = mDetectedLanguage != null ?
                mDetectedLanguage :
                LanguageManager.getLanguageValue(mTranslateFromLanguages[mTranslateFromSpinner.getSelectedItemPosition()], context);
        String oldTranslateTo = mTranslateToLanguages[mTranslateToSpinner.getSelectedItemPosition()];

        LanguageManager.saveLastUsedTranslateTo(context, oldTranslateFrom);
        for (int i = 0, size = mTranslateFromLanguages.length; i < size; i++) {
            if (mTranslateFromLanguages[i].equals(oldTranslateTo)) {
                mTranslateFromSpinner.setSelection(i);
                String translateFromValue = LanguageManager.getLanguageValue(mTranslateFromLanguages[i], context);
                LanguageManager.saveLastUsedTranslateFrom(context, translateFromValue);
                break;
            }
        }

        final OvershootInterpolator interpolator = new OvershootInterpolator();
        ViewCompat.animate(mInvertLanguagesFab).
                rotation(180f).
                withLayer().
                setDuration(350).
                setInterpolator(interpolator).
                withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mInvertLanguagesFab.setRotation(0);
                    }
                }).
                setStartDelay(75);

        mListener.logEvent("invert_languages", null);
    }

    private void checkTranslateFromLabelVisibility() {
        Context context = getContext();
        if (context != null) {
            int[] textViewLocation = new int[2];
            int eightDpToPixelValue = (int) AndroidUtils.convertDpToPixel(8, context);
            mTranslateFromTextView.getLocationOnScreen(textViewLocation);
            mTranslateFromTextView.setVisibility(textViewLocation[0] > eightDpToPixelValue ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void onTextToSpeechTapped() {
        if (!mTextToSpeechInitialized || mTextToSpeech.isSpeaking() || mTranslatedTextView.getText().toString().isEmpty() || mLastTranslatedTo == null) {
            return;
        }
        if (mListener.getCurrentMediaVolume() > 0) {
            mTextToSpeech.setLanguage(LanguageManager.getLocaleFromLanguageValue(mLastTranslatedTo));
            mTextToSpeech.speak(mTranslatedTextView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null );
        }
        else {
            Snackbar.make(mClearButton, R.string.volume_off_label, Snackbar.LENGTH_SHORT).show();
        }
        mListener.logEvent("text_to_speech", null);
    }

    // endregion


    // region Public Methods

    public void setToTranslateText(String text) {
        mToTranslateEditText.setText(text);
        mToTranslateEditText.setSelection(text != null ? text.length() : 0);
    }

    // endregion


    public interface OnFragmentInteractionListener {
        void onSpeechToTextTapped(String selectedLocale);
        int getCurrentMediaVolume();
        void logEvent(String event, Bundle bundle);
    }
}
