package com.anthony.deepl.openl.model

import com.google.gson.annotations.SerializedName

import com.anthony.deepl.openl.manager.LanguageManager

import java.text.BreakIterator
import java.util.ArrayList

data class TranslationRequest(
        private val sentence: String,
        private val fromLanguage: String,
        private val toLanguage: String,
        private val userPreferredLanguages: List<String>,
        @SerializedName("jsonrpc") private val jsonRpc: String = "2.0",
        @SerializedName("method") private val method: String = "LMT_handle_jobs") {


    @SerializedName("params")
    private val mParams: TranslationRequestParams
    val lineBreakPositions: MutableList<Int>

    init {
        val jobList = ArrayList<TranslationRequestJob>()
        val sentenceIterator = BreakIterator.getSentenceInstance(LanguageManager.getLocaleFromLanguageValue(fromLanguage, null))
        var sentenceStart: Int
        var sentenceEnd: Int
        var jobCount = 0
        lineBreakPositions = ArrayList()

        sentenceIterator.setText(sentence)
        sentenceStart = sentenceIterator.first()
        sentenceEnd = sentenceIterator.next()
        while (sentenceEnd != BreakIterator.DONE) {
            val detectedSentence = sentence.substring(sentenceStart, sentenceEnd)
            jobList.add(TranslationRequestJob(sentence = detectedSentence))
            if (detectedSentence.contains("\n")) {
                lineBreakPositions.add(jobCount)
            }
            jobCount++
            sentenceStart = sentenceEnd
            sentenceEnd = sentenceIterator.next()
        }

        mParams = TranslationRequestParams(
                jobs = jobList,
                languages = TranslationRequestLanguage(fromLanguage, toLanguage, userPreferredLanguages))
    }
}

internal data class TranslationRequestParams(
        @SerializedName("priority") val priority: Short = -1,
        @SerializedName("jobs") val jobs: List<TranslationRequestJob>?,
        @SerializedName("lang") val languages: TranslationRequestLanguage?
)

internal data class TranslationRequestJob(
        @SerializedName("kind") val jobKind: String = "default",
        @SerializedName("raw_en_sentence") val sentence: String = "LMT_handle_jobs"
)

internal data class TranslationRequestLanguage(
        @SerializedName("source_lang_user_selected") val sourceLanguage: String,
        @SerializedName("target_lang") val targetLanguage: String,
        @SerializedName("user_preferred_langs") val preferredLanguages: List<String>
)