package com.anthony.deepl.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TranslationRequestSingleLine {

    @SerializedName("jsonrpc")
    private String mJsonRpc = "2.0";

    @SerializedName("method")
    private String mMethod = "LMT_handle_jobs";

    @SerializedName("params")
    private TranslationRequestSingleLineParams mParams;

    public TranslationRequestSingleLine(String sentence, String fromLanguage, String toLanguage, List<String> userPreferredLanguages) {
        TranslationRequestSingleLineJob job = new TranslationRequestSingleLineJob();
        job.setSentence(sentence);
        List<TranslationRequestSingleLineJob> jobList = new ArrayList<>();
        jobList.add(job);

        mParams = new TranslationRequestSingleLineParams();
        mParams.setJobList(jobList);
        mParams.setRequestLanguages(new TranslationRequestLanguage(fromLanguage, toLanguage, userPreferredLanguages));
    }

}

class TranslationRequestSingleLineParams {

    @SerializedName("priority")
    private short mPriority = -1;

    @SerializedName("jobs")
    private List<TranslationRequestSingleLineJob> mJobs;

    @SerializedName("lang")
    private TranslationRequestLanguage mLanguages;

    void setJobList(List<TranslationRequestSingleLineJob> jobs) {
        mJobs = jobs;
    }

    void setRequestLanguages(TranslationRequestLanguage languages) {
        mLanguages = languages;
    }

}

class TranslationRequestSingleLineJob {

    @SerializedName("kind")
    private String mJobKind = "default";

    @SerializedName("raw_en_sentence")
    private String mSentence = "LMT_handle_jobs";

    void setSentence(String sentence) {
        mSentence = sentence;
    }

}
