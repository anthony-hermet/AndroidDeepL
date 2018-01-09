package com.anthony.deepl.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.anthony.deepl.R;
import com.anthony.deepl.manager.LanguageManager;

public class MainFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private AppCompatSpinner mTranslateFromSpinner;
    private AppCompatSpinner mTranslateToSpinner;

    public MainFragment() {}

    // region Overridden methods

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Spinners setup
        String translateFromLanguages[] = LanguageManager.getLanguagesStringArray(getContext(), null, true);
        // Default layouts : android.R.layout.simple_spinner_item, android.R.layout.simple_spinner_dropdown_item
        ArrayAdapter<String> translateFromAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, translateFromLanguages);
        translateFromAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateFromSpinner.setAdapter(translateFromAdapter);

        mTranslateFromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTranslateToSpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Update languages available on translateTo spinner depending on selected translateFrom language
    private void updateTranslateToSpinner() {
        String selectedLanguage = LanguageManager.getLanguageValue(mTranslateFromSpinner.getSelectedItem().toString(), getContext());
        String translateToLanguages[] = LanguageManager.getLanguagesStringArray(getContext(), selectedLanguage, false);
        ArrayAdapter<String> translateToAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, translateToLanguages);
        translateToAdapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        mTranslateToSpinner.setAdapter(translateToAdapter);
    }

    // endregion

    public interface OnFragmentInteractionListener {
    }
}
