package com.anthony.deepl.openl.backend

import com.anthony.deepl.openl.model.TranslationRequest
import com.anthony.deepl.openl.model.TranslationResponse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeepLService {

    companion object {
        const val BASE_URL = "https://www2.deepl.com"
    }

    @Headers("accept-language: en,de,fr,es,it,nl,pl")
    @POST("/jsonrpc")
    fun translateText(@Body translationRequest: TranslationRequest): Call<TranslationResponse>

}
