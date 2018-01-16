package com.anthony.deepl.model;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranslationRequestMultiLine {

    @SerializedName("jsonrpc")
    private String mJsonRpc = "2.0";

    @SerializedName("method")
    private String mMethod = "LMT_split_into_sentences";

    @SerializedName("params")
    private TranslationRequestMultiLineParams mParams;

    public TranslationRequestMultiLine(String sentence, String fromLanguage, String toLanguage, List<String> userPreferredLanguages) {
        mParams = new TranslationRequestMultiLineParams();
        mParams.setTextsList(sentence.split("\n"));
        mParams.setRequestLanguages(new TranslationRequestLanguage(fromLanguage, toLanguage, userPreferredLanguages));
    }
}


class TranslationRequestMultiLineParams {

    @SerializedName("priority")
    private short mPriority = -1;

    @SerializedName("texts")
    private String[] mTexts;

    @SerializedName("lang")
    private TranslationRequestLanguage mLanguages;

    void setTextsList(String[] texts) {
        mTexts = texts;
    }

    void setRequestLanguages(TranslationRequestLanguage languages) {
        mLanguages = languages;
    }

}