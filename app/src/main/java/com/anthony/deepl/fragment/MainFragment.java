package com.anthony.deepl.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

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

import static com.anthony.deepl.manager.LanguageManager.ENGLISH;
import static com.anthony.deepl.manager.LanguageManager.FRENCH;

public class MainFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private AppCompatSpinner mTranslateFromSpinner;
    private AppCompatSpinner mTranslateToSpinner;
    private EditText mToTranslateEditText;
    private EditText mTranslatedEditText;

    private DeepLService mDeepLService;

    private boolean mTranslationInProgress;
    private String mTranslateFromLanguages[];
    private String mTranslateToLanguages[];
    private String mLastTranlatedSentence;
    private String mLastTranslatedFrom;
    private String mLastTranslatedTo;

    public MainFragment() {
    }

    // region Overridden methods

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.deepl.com")
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

    // endregion


    // region Private Methods

    private void initViews(View view) {
        // Views retrieval
        mTranslateFromSpinner = view.findViewById(R.id.translate_from_spinner);
        mTranslateToSpinner = view.findViewById(R.id.translate_to_spinner);
        mToTranslateEditText = view.findViewById(R.id.to_translate_edit_text);
        mTranslatedEditText = view.findViewById(R.id.translated_edit_text);

        // Spinners setup
        // Default layouts : android.R.layout.simple_spinner_item, android.R.layout.simple_spinner_dropdown_item
        mTranslateFromLanguages = LanguageManager.getLanguagesStringArray(getContext(), null, true);
        ArrayAdapter<String> translateFromAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateFromLanguages);
        translateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateFromSpinner.setAdapter(translateFromAdapter);

        mTranslateFromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTranslateToSpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mTranslateToSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTranslation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mToTranslateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTranslation();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // Update languages available on translateTo spinner depending on selected translateFrom language
    private void updateTranslateToSpinner() {
        String selectedLanguage = LanguageManager.getLanguageValue(mTranslateFromSpinner.getSelectedItem().toString(), getContext());
        mTranslateToLanguages = LanguageManager.getLanguagesStringArray(getContext(), selectedLanguage, false);
        ArrayAdapter<String> translateToAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, mTranslateToLanguages);
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateToSpinner.setAdapter(translateToAdapter);
    }

    private void updateTranslation() {
        Log.d("TEST", String.valueOf(mTranslationInProgress));
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
        if (toTranslate.equals(mLastTranlatedSentence) &&
                translateFrom.equals(mLastTranslatedFrom) &&
                translateTo.equals(mLastTranslatedTo)) {
            Log.d("TEST", "return");
            return;
        }

        // If fields have changed, we launch a new translation
        Log.d("TEST", "translate");
        mTranslationInProgress = true;
        mLastTranlatedSentence = toTranslate;
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
                // We call it again to check if something has changed since we've launched the network call
                updateTranslation();
            }

            @Override
            public void onFailure(@NonNull Call<TranslationResponse> call, @NonNull Throwable t) {
                mTranslationInProgress = false;
                Log.d("TEST", "failure");
            }
        });
    }

    // endregion

    public interface OnFragmentInteractionListener {
    }
}
