package com.anthony.deepl.openl.model;

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

    public String getSourceLanguage() {
        return mResult.getSourceLanguage();
    }

    public String getBestTranslation(List<Integer> lineBreakPositions) {
        String bestTranslation = "";
        List<TranslationResponseTranslations> translations = mResult.getTranslations();
        if (lineBreakPositions == null) {
            lineBreakPositions = new ArrayList<>();
        }

        for (int i = 0, translationSize = translations != null ? translations.size() : 0; i < translationSize; i++) {
            String bestResultSentence = null;
            Float bestResultScore = null;
            List<TranslationResponseBeam> beams = translations.get(i).getBeams();
            for (int j = 0, size = beams != null ? beams.size() : 0; j < size; j++) {
                TranslationResponseBeam beam = beams.get(j);
                if (bestResultScore == null || bestResultScore < beam.getTotalProbability()) {
                    bestResultScore = beam.getTotalProbability();
                    bestResultSentence = beam.getTranslatedSentence();
                }
            }
            if (bestResultSentence != null) {
                bestTranslation = bestTranslation.concat(bestResultSentence);
            }
            if (i + 1 < translationSize) {
                bestTranslation = bestTranslation.concat(lineBreakPositions.contains(i) ? "\n" : " ");
            }
        }
        return bestTranslation;
    }

    public List<String> getOtherResults() {
        if (mResult.getTranslations() == null || mResult.getTranslations().size() > 1) {
            return null;
        }
        int bestResultPosition = -1;
        Float bestResultScore = null;
        List<String> otherSentences = new ArrayList<>();
        List<TranslationResponseBeam> beams = mResult.getTranslations().get(0).getBeams();

        for (int i = 0, size = beams != null ? beams.size() : 0; i < size; i++) {
            TranslationResponseBeam beam = beams.get(i);
            otherSentences.add(beam.getTranslatedSentence());
            if (bestResultScore == null || bestResultScore < beam.getTotalProbability()) {
                bestResultScore = beam.getTotalProbability();
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

    public String getSourceLanguage() {
        return mSourceLanguage;
    }

    public List<TranslationResponseTranslations> getTranslations() {
        return mTranslations;
    }

}

class TranslationResponseTranslations {

    @SerializedName("timeAfterPreprocessing")
    private Double mAfterPreprocessingTime;

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
        return mTranslationScore;
    }

    public float getTotalProbability() {
        return mTotalProbability;
    }

    public String getTranslatedSentence() {
        return mTranslatedSentence;
    }

}