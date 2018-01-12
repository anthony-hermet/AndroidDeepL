package com.anthony.deepl.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.anthony.deepl.R;
import com.anthony.deepl.backend.DeepLService;
import com.anthony.deepl.manager.LanguageManager;
import com.anthony.deepl.model.TranslationRequest;
import com.anthony.deepl.model.TranslationResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.anthony.deepl.manager.LanguageManager.AUTO;
import static com.anthony.deepl.manager.LanguageManager.ENGLISH;
import static com.anthony.deepl.manager.LanguageManager.FRENCH;

public class MainFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private OnFragmentInteractionListener mListener;

    private AppCompatSpinner mTranslateFromSpinner;
    private AppCompatSpinner mTranslateToSpinner;
    private EditText mToTranslateEditText;
    private EditText mTranslatedEditText;
    private ImageButton mClearButton;
    private ImageButton mCopyToClipboardButton;
    private ImageButton mInvertLanguagesButton;

    private DeepLService mDeepLService;
    private String mTranslateFromLanguages[];
    private String mTranslateToLanguages[];
    private String mLastTranslatedSentence;
    private String mLastTranslatedFrom;
    private String mLastTranslatedTo;
    private boolean mTranslationInProgress;

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
                mToTranslateEditText.setText("");
                mTranslatedEditText.setText("");
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
                updateTranslateToSpinner();
                break;
            case R.id.translate_to_spinner:
                updateTranslation();
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // endregion


    // region Private Methods

    private void initViews(View view) {
        // Views retrieval
        mTranslateFromSpinner = view.findViewById(R.id.translate_from_spinner);
        mTranslateToSpinner = view.findViewById(R.id.translate_to_spinner);
        mToTranslateEditText = view.findViewById(R.id.to_translate_edit_text);
        mTranslatedEditText = view.findViewById(R.id.translated_edit_text);
        mClearButton = view.findViewById(R.id.clear_to_translate_button);
        mCopyToClipboardButton = view.findViewById(R.id.copy_to_clipboard_button);
        mInvertLanguagesButton = view.findViewById(R.id.invert_languages_button);

        // Spinners setup
        // Default layouts : android.R.layout.simple_spinner_item, android.R.layout.simple_spinner_dropdown_item
        mTranslateFromLanguages = LanguageManager.getLanguagesStringArray(getContext(), null, true);
        ArrayAdapter<String> translateFromAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateFromLanguages);
        translateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateFromSpinner.setAdapter(translateFromAdapter);

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
        mCopyToClipboardButton.setOnClickListener(this);
        mInvertLanguagesButton.setOnClickListener(this);
        mToTranslateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mClearButton.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                updateTranslation();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mTranslatedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCopyToClipboardButton.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void updateTranslateToSpinner() {
        // We update the translateTo spinner based on translateFrom selected language
        String selectedLanguage = LanguageManager.getLanguageValue(mTranslateFromSpinner.getSelectedItem().toString(), getContext());
        mTranslateToLanguages = LanguageManager.getLanguagesStringArray(getContext(), selectedLanguage, false);
        ArrayAdapter<String> translateToAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateToLanguages);
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateToSpinner.setAdapter(translateToAdapter);

        // If translateFrom selected language isn't auto, we display the invert languages button
        mInvertLanguagesButton.setVisibility(selectedLanguage.equals(AUTO) ? View.INVISIBLE : View.VISIBLE);

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
        preferredLanguages.add(FRENCH);
        preferredLanguages.add(ENGLISH);

        TranslationRequest request = new TranslationRequest(
                toTranslate,
                translateFrom,
                translateTo,
                preferredLanguages);
        Call<TranslationResponse> call = mDeepLService.translateText(request);
        call.enqueue(new Callback<TranslationResponse>() {
            @Override
            public void onResponse(@NonNull Call<TranslationResponse> call, @NonNull Response<TranslationResponse> response) {
                mTranslationInProgress = false;
                mTranslatedEditText.setText(response.body().getBestResult());
                // We call the method again to check if something has changed since we've launched the network call
                updateTranslation();

                // If AUTO is selected, we update the label with the detected language
                if (isAdded() && mTranslateFromSpinner.getSelectedItemPosition() == 0) {
                    TextView spinnerTextView = (TextView) mTranslateFromSpinner.getSelectedView();
                    String detectedLanguage = LanguageManager.getLanguageString(response.body().getSourceLanguage(), getContext());
                    detectedLanguage = detectedLanguage.concat(" ").concat(getString(R.string.detected_language_label));
                    spinnerTextView.setText(detectedLanguage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TranslationResponse> call, @NonNull Throwable t) {
                mTranslationInProgress = false;
                // TODO : Log exception into tracking tool
            }
        });
    }

    private void displayDetectedLanguage() {

    }

    private void copyTranslatedTextToClipboard() {
        String translatedText = mTranslatedEditText.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            // First we close the keyboard
            Activity mainActivity = getActivity();
            View view = mainActivity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            // Then we clip the translated text and display confirmation snackbar
            ClipData clip = ClipData.newPlainText(translatedText, translatedText);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(mClearButton,
                    R.string.copied_to_clipboard_text,
                    Snackbar.LENGTH_SHORT).show();
        }
        // TODO
        // else Timber.e("Clipboard is null and shouldn't be");
    }

    private void invertLanguages() {
        Context context = getContext();
        String oldTranslateFrom = LanguageManager.getLanguageValue(mTranslateFromLanguages[mTranslateFromSpinner.getSelectedItemPosition()], context);
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
    }

    // endregion

    public interface OnFragmentInteractionListener {
    }
}
