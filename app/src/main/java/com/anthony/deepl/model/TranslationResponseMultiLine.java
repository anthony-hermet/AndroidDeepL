package com.anthony.deepl.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranslationResponseMultiLine {

    @SerializedName("id")
    private String mId;

    @SerializedName("jsonrpc")
    private String mJsonRpc;

    @SerializedName("result")
    private TranslationResponseMultiLineResult mResult;

    public String getSourceLanguage() {
        return mResult.getSourceLanguage();
    }

    public String getTranslation() {
        String translation = "";
        List<String> translations = mResult.getTranslations();
        for (int i = 0, size = translations != null ? translations.size() : 0; i < size; i++) {
           translation = translation.concat(translations.get(i));
           if (i + 1 < size) {
               translation = translation.concat("\n");
           }
        }
        return translation;
    }
}


class TranslationResponseMultiLineResult {

    @SerializedName("lang")
    private String mSourceLanguage;

    @SerializedName("lang_is_confident")
    private Float mSourceLanguageConfinence;

    @SerializedName("splitted_texts")
    private List<String> mTranslations;

    public String getSourceLanguage() {
        return mSourceLanguage;
    }

    public List<String> getTranslations() {
        return mTranslations;
    }

}