package com.anthony.deepl.openl.model

import com.anthony.deepl.openl.manager.LanguageManager
import com.squareup.moshi.Json

import java.text.BreakIterator
import java.util.ArrayList

data class TranslationRequest(
        private val sentence: String,
        private val fromLanguage: String,
        private val toLanguage: String,
        private val userPreferredLanguages: List<String>,
        @field:Json(name="jsonrpc") private val jsonRpc: String = "2.0",
        @field:Json(name="method") private val method: String = "LMT_handle_jobs") {


    @field:Json(name = "params") private val mParams: TranslationRequestParams
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
        @field:Json(name="priority") val priority: Short = -1,
        @field:Json(name="jobs") val jobs: List<TranslationRequestJob>?,
        @field:Json(name="lang") val languages: TranslationRequestLanguage?
)

internal data class TranslationRequestJob(
        @field:Json(name="kind") val jobKind: String = "default",
        @field:Json(name="raw_en_sentence") val sentence: String = "LMT_handle_jobs"
)

internal data class TranslationRequestLanguage(
        @field:Json(name="source_lang_user_selected") val sourceLanguage: String,
        @field:Json(name="target_lang") val targetLanguage: String,
        @field:Json(name="user_preferred_langs") val preferredLanguages: List<String>
)