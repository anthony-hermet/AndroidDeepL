package com.anthony.deepl.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        String translateToLanguages[] = LanguageManager.getLanguagesStringArray(getContext(), LanguageManager.getSavedTranslateFrom(), true);
        // Default layout : android.R.layout.simple_spinner_item
        ArrayAdapter<String> translateFromAdapter = new ArrayAdapter<>(getContext(), R.layout.item_language_spinner, translateFromLanguages);
        ArrayAdapter<String> translateToAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, translateToLanguages);
        // Default layout : android.R.layout.simple_spinner_dropdown_item
        translateFromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        translateToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTranslateFromSpinner.setAdapter(translateFromAdapter);
        mTranslateToSpinner.setAdapter(translateToAdapter);
    }

    // endregion

    public interface OnFragmentInteractionListener {
    }
}
