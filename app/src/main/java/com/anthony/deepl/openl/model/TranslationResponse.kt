package com.anthony.deepl.openl.model

import com.squareup.moshi.Json
import java.util.ArrayList

data class TranslationResponse (
        @field:Json(name="id") val id: String?,
        @field:Json(name="jsonrpc") val jsonRpc: String?) {

    @field:Json(name="result")
    private val mResult: TranslationResponseResult? = null
    var lineBreakPositions: MutableList<Int> = mutableListOf()

    val sourceLanguage: String?
        get() = mResult?.sourceLanguage

    fun getBestTranslation(): String {
        var bestTranslation = ""

        mResult?.translations?.forEachIndexed { index, sentence ->
            var bestResultSentence: String? = null
            var bestResultScore: Float = -1F
            sentence.beams?.forEach {
                if (bestResultScore == -1F || bestResultScore < it.totalProbability) {
                    bestResultScore = it.totalProbability
                    bestResultSentence = it.translatedSentence
                }
            }
            if (bestResultSentence != null) {
                bestTranslation += bestResultSentence
                bestTranslation += if (lineBreakPositions.contains(index) == true) "\n" else " "
            }
        }

        return bestTranslation
    }

    fun getAlternateTranslations(): List<String>? {
        if (mResult?.translations?.size ?: 0 > 1) {
            return null
        }
        var bestResultPosition = -1
        var bestResultScore: Float = -1F
        val otherSentences = ArrayList<String>()

        mResult?.translations?.get(0)?.beams?.forEachIndexed { index, beam ->
            otherSentences.add(beam.translatedSentence)
            if (bestResultScore == -1F || bestResultScore < beam.totalProbability) {
                bestResultScore = beam.totalProbability
                bestResultPosition = index
            }
        }

        if (bestResultPosition != -1 && otherSentences.size > bestResultPosition) {
            otherSentences.removeAt(bestResultPosition)
        }

        return otherSentences
    }
}

internal data class TranslationResponseResult(
        @field:Json(name="source_lang") val sourceLanguage: String?,
        @field:Json(name="source_lang_is_confident") val sourceLanguageConfidence: Float?,
        @field:Json(name="target_lang") val targetLanguage: String?,
        @field:Json(name="translations") val translations: List<TranslationResponseTranslations>?
)

internal data class TranslationResponseTranslations(
        @field:Json(name="timeAfterPreprocessing") val afterPreprocessingTime: Double?,
        @field:Json(name="timeReceivedFromEndpoint") val receivedFromEndpointTime: String?,
        @field:Json(name="timeSentToEndpoint") val sendToEndpointTime: Float?,
        @field:Json(name="total_time_endpoint") val mTotalEndpointTime: Float?,
        @field:Json(name="beams") val beams: List<TranslationResponseBeam>?
)

internal data class TranslationResponseBeam(
        @field:Json(name="num_symbols") val translationLength: Int,
        @field:Json(name="postprocessed_sentence") val translatedSentence: String,
        @field:Json(name="score") val translationScore: Float?,
        @field:Json(name="totalLogProb") val totalProbability: Float
)