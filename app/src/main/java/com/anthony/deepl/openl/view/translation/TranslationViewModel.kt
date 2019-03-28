package com.anthony.deepl.openl.view.translation

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.manager.LanguageManager
import com.anthony.deepl.openl.model.*
import com.anthony.deepl.openl.util.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

class TranslationViewModel(private val app: Application,
                           private val deepLService: DeepLService,
                           private val firebaseManager: FirebaseManager) : AndroidViewModel(app), CoroutineScope {

    companion object {
        private const val REQUEST_DELAY_MS = 800L
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val liveTranslationResponse = MutableLiveData<ResultResource<CompletedTranslation>>()
    fun getLiveTranslationResponse(): LiveData<ResultResource<CompletedTranslation>> = liveTranslationResponse

    var lastRequest: TranslationRequest? = null
    var detectedLanguage: String? = null
    private var currentRequest: Job? = null

    init {
        liveTranslationResponse.postValue(IdleResource())
        firebaseManager.fetchRemoteConfigValues()
    }

    fun getLastTranslation(): CompletedTranslation? {
        if (liveTranslationResponse.value == null || liveTranslationResponse.value !is SuccessResource) {
            return null
        }
        return (liveTranslationResponse.value as SuccessResource).data
    }

    fun requestTranslation(toTranslate: String, translateFrom: String, translateTo: String, retry: Boolean) {
        val lastTranslation = getLastTranslation()

        // We check if fields have changed since last translation
        if (toTranslate == lastTranslation?.request?.sentence &&
                translateFrom == lastTranslation.request.fromLanguage &&
                translateTo == lastTranslation.request.toLanguage &&
                !retry) {
            return
        }

        // If languages have changed, we save it to preferences
        if (lastTranslation == null || translateFrom != lastTranslation.request.fromLanguage) {
            LanguageManager.saveLastUsedTranslateFrom(app, translateFrom)
        }
        if (lastTranslation == null || translateTo != lastTranslation.request.toLanguage) {
            LanguageManager.saveLastUsedTranslateTo(app, translateTo)
        }

        val preferredLanguages = ArrayList<String>()
        preferredLanguages.add(LanguageManager.getLastUsedTranslateFrom(app))
        preferredLanguages.add(LanguageManager.getLastUsedTranslateTo(app))
        val request = TranslationRequest(
                toTranslate,
                translateFrom,
                translateTo,
                preferredLanguages,
                "2.0",
                "LMT_handle_jobs")
        if (lastRequest != request) {
            lastRequest = request
            currentRequest?.cancel()
            currentRequest = launch {
                Thread.sleep(REQUEST_DELAY_MS)
                sendTranslationRequest(request)
            }
        }
    }

    fun logEvent(event: String, bundle: Bundle?) {
        firebaseManager.logEvent(event, bundle)
    }

    fun clearLastResponse() {
        liveTranslationResponse.value = IdleResource()
    }

    private fun sendTranslationRequest(request: TranslationRequest) {
        liveTranslationResponse.postValue(LoadingResource())

        val call = deepLService.translateText(request)
        call.enqueue(object : Callback<TranslationResponse> {
            override fun onFailure(call: Call<TranslationResponse>?, t: Throwable?) {
                Timber.e(t.toString())
                liveTranslationResponse.value = FailureResource(t, t?.message)
            }

            override fun onResponse(call: Call<TranslationResponse>, response: Response<TranslationResponse>) {
                val translationResponse = response.body()
                if (translationResponse == null) {
                    onFailure(call, Exception("Translation response body is null"))
                    return
                }

                translationResponse.lineBreakPositions = request.lineBreakPositions
                liveTranslationResponse.value = SuccessResource(CompletedTranslation(request, translationResponse))

                // Reporting
                val params = Bundle()
                params.putString("translate_from", request.fromLanguage)
                params.putString("translate_to", request.toLanguage)
                logEvent("translation", params)
            }
        })
    }
}