package com.anthony.deepl.openl.model

import com.google.gson.annotations.SerializedName

import java.util.ArrayList

data class TranslationResponse (
        @SerializedName("id") val id: String?,
        @SerializedName("jsonrpc") val jsonRpc: String?) {

    @SerializedName("result")
    private val mResult: TranslationResponseResult? = null

    val sourceLanguage: String?
        get() = mResult?.sourceLanguage

    fun getBestTranslation(lineBreakPositions: List<Int>?): String {
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
                bestTranslation += if (lineBreakPositions?.contains(index) == true) "\n" else " "
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
        @SerializedName("source_lang") val sourceLanguage: String?,
        @SerializedName("source_lang_is_confident") val sourceLanguageConfidence: Float?,
        @SerializedName("target_lang") val targetLanguage: String?,
        @SerializedName("translations") val translations: List<TranslationResponseTranslations>?
)

internal data class TranslationResponseTranslations(
        @SerializedName("timeAfterPreprocessing") val afterPreprocessingTime: Double?,
        @SerializedName("timeReceivedFromEndpoint") val receivedFromEndpointTime: String?,
        @SerializedName("timeSentToEndpoint") val sendToEndpointTime: Float?,
        @SerializedName("total_time_endpoint") val mTotalEndpointTime: Float?,
        @SerializedName("beams") val beams: List<TranslationResponseBeam>?
)

internal data class TranslationResponseBeam(
        @SerializedName("num_symbols") val translationLength: Int,
        @SerializedName("postprocessed_sentence") val translatedSentence: String,
        @SerializedName("score") val translationScore: Float?,
        @SerializedName("totalLogProb") val totalProbability: Float
)