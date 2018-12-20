package com.anthony.deepl.openl.view.translation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class TranslationViewModel(private val deepLService: DeepLService) : ViewModel() {

    val liveTranslationResponse = MutableLiveData<ResultResource<TranslationResponse>>()

    fun translate(toTranslate: String, translateFrom: String, translateTo: String, preferredLanguages: List<String>) {
        liveTranslationResponse.postValue(LoadingResource())
        val request = TranslationRequest(
                toTranslate,
                translateFrom,
                translateTo,
                preferredLanguages,
                "2.0",
                "LMT_handle_jobs")
        val call = deepLService.translateText(request)
        call.enqueue(object : Callback<TranslationResponse> {
            override fun onFailure(call: Call<TranslationResponse>?, t: Throwable?) {
                Timber.e(t.toString())
                liveTranslationResponse.postValue(FailureResource(t, t?.message))
            }

            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                val translationResponse = response.body()
                if (translationResponse == null) {
                    onFailure(call, Exception("Translation response body is null"))
                    liveTranslationResponse.value = FailureResource(null, null)
                    return
                }
                translationResponse.lineBreakPositions = request.lineBreakPositions
                liveTranslationResponse.value = SuccessResource(translationResponse)
            }
        })
    }
}