package com.anthony.deepl.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TranslationResponse {

    @SerializedName("id")
    private String mId;

    @SerializedName("jsonrpc")
    private String mJsonRpc;

    @SerializedName("result")
    private TranslationResponseResult mResult;

    public String getBestResult() {
        String bestResultSentence = null;
        Float bestResultScore = null;
        List<TranslationResponseBeam> beams =
                (mResult.getTranslations() != null && mResult.getTranslations().size() > 0) ?
                        mResult.getTranslations().get(0).getBeams() :
                        null;
        for (int i = 0, size = beams != null ? beams.size() : 0; i < size; i++) {
            TranslationResponseBeam beam = beams.get(i);
            if (bestResultScore == null || bestResultScore < beam.getTranslationScore()) {
                bestResultScore = beam.getTranslationScore();
                bestResultSentence = beam.getTranslatedSentence();
            }
        }
        return bestResultSentence;
    }

    public List<String> getOtherResults() {
        int bestResultPosition = -1;
        Float bestResultScore = null;
        List<String> otherSentences = new ArrayList<>();
        List<TranslationResponseBeam> beams =
                (mResult.getTranslations() != null && mResult.getTranslations().size() > 0) ?
                        mResult.getTranslations().get(0).getBeams() :
                        null;

        for (int i = 0, size = beams != null ? beams.size() : 0; i < size; i++) {
            TranslationResponseBeam beam = beams.get(i);
            otherSentences.add(beam.getTranslatedSentence());
            if (bestResultScore == null || bestResultScore < beam.getTranslationScore()) {
                bestResultScore = beam.getTranslationScore();
                bestResultPosition = i;
            }
        }

        if (bestResultPosition != -1 && otherSentences.size() > bestResultPosition) {
            otherSentences.remove(bestResultPosition);
        }

        return otherSentences;
    }

}

class TranslationResponseResult {

    @SerializedName("source_lang")
    private String mSourceLanguage;

    @SerializedName("source_lang_is_confident")
    private Float mSourceLanguageConfinence;

    @SerializedName("target_lang")
    private String mTargetLanguage;

    @SerializedName("translations")
    private List<TranslationResponseTranslations> mTranslations;

    public List<TranslationResponseTranslations> getTranslations() {
        return mTranslations;
    }

}

class TranslationResponseTranslations {

    @SerializedName("timeAfterPreprocessing")
    private int mAfterPreprocessingTime;

    @SerializedName("timeReceivedFromEndpoint")
    private String mReceivedFromEndpointTime;

    @SerializedName("timeSentToEndpoint")
    private Float mSendToEndpointTime;

    @SerializedName("total_time_endpoint")
    private Float mTotalEndpointTime;

    @SerializedName("beams")
    private List<TranslationResponseBeam> mBeams;

    public List<TranslationResponseBeam> getBeams() {
        return mBeams;
    }

}

class TranslationResponseBeam {

    @SerializedName("num_symbols")
    private int mTranslationLength;

    @SerializedName("postprocessed_sentence")
    private String mTranslatedSentence;

    @SerializedName("score")
    private Float mTranslationScore;

    @SerializedName("totalLogProb")
    private Float mTotalProbability;

    public float getTranslationScore() {
        return mTranslationScore >= 0 ? mTranslationScore : mTranslationScore * -1;
    }

    public String getTranslatedSentence() {
        return mTranslatedSentence;
    }

}