package com.anthony.deepl.openl.model;

import com.google.gson.annotations.SerializedName;

import com.anthony.deepl.openl.manager.LanguageManager;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public class TranslationRequest {

    @SerializedName("jsonrpc")
    private String mJsonRpc = "2.0";

    @SerializedName("method")
    private String mMethod = "LMT_handle_jobs";

    @SerializedName("params")
    private TranslationRequestParams mParams;

    private List<Integer> mLineBreakPositions;

    public TranslationRequest(String sentence, String fromLanguage, String toLanguage, List<String> userPreferredLanguages) {
        List<TranslationRequestJob> jobList = new ArrayList<>();
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(LanguageManager.getLocaleFromLanguageValue(fromLanguage, null));
        int sentenceStart, sentenceEnd;
        int jobCount = 0;
        mLineBreakPositions = new ArrayList<>();

        sentenceIterator.setText(sentence);
        for (sentenceStart = sentenceIterator.first(), sentenceEnd = sentenceIterator.next(); sentenceEnd != BreakIterator.DONE; sentenceStart = sentenceEnd, sentenceEnd = sentenceIterator.next()) {
            TranslationRequestJob job = new TranslationRequestJob();
            String detectedSentence = sentence.substring(sentenceStart, sentenceEnd);
            job.setSentence(detectedSentence);
            jobList.add(job);
            if (detectedSentence.contains("\n")) {
                mLineBreakPositions.add(jobCount);
            }
            jobCount++;
        }

        mParams = new TranslationRequestParams();
        mParams.setJobList(jobList);
        mParams.setRequestLanguages(new TranslationRequestLanguage(fromLanguage, toLanguage, userPreferredLanguages));
    }

    public List<Integer> getLineBreakPositions() {
        return mLineBreakPositions;
    }
}

class TranslationRequestParams {

    @SerializedName("priority")
    private short mPriority = -1;

    @SerializedName("jobs")
    private List<TranslationRequestJob> mJobs;

    @SerializedName("lang")
    private TranslationRequestLanguage mLanguages;

    void setJobList(List<TranslationRequestJob> jobs) {
        mJobs = jobs;
    }

    void setRequestLanguages(TranslationRequestLanguage languages) {
        mLanguages = languages;
    }

}

class TranslationRequestJob {

    @SerializedName("kind")
    private String mJobKind = "default";

    @SerializedName("raw_en_sentence")
    private String mSentence = "LMT_handle_jobs";

    void setSentence(String sentence) {
        mSentence = sentence;
    }

}

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