package com.anthony.deepl.openl.backend;

import com.anthony.deepl.openl.model.TranslationRequest;
import com.anthony.deepl.openl.model.TranslationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DeepLService {

    String BASE_URL = "https://www.deepl.com";

    @Headers("accept-language: en,de,fr,es,it,nl,pl")
    @POST("/jsonrpc")
    Call<TranslationResponse> translateText(@Body TranslationRequest translationRequest);

}
