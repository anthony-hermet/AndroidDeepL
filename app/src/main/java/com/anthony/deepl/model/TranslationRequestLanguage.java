package com.anthony.deepl.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class TranslationRequestLanguage {

    @SerializedName("source_lang_user_selected")
    private String mSourceLanguage;

    @SerializedName("target_lang")
    private String mTargetLanguage;

    @SerializedName("user_preferred_langs")
    private List<String> mPreferredLanguages;

    public TranslationRequestLanguage(String sourceLanguage, String targetLanguage, List<String> preferredLanguages) {
        mSourceLanguage = sourceLanguage;
        mTargetLanguage = targetLanguage;
        mPreferredLanguages = preferredLanguages;
    }

}
