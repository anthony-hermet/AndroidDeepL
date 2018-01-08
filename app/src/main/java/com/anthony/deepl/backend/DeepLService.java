package com.anthony.deepl.backend;

import com.anthony.deepl.model.TranslationRequest;
import com.anthony.deepl.model.TranslationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DeepLService {

    @POST("/jsonrpc")
    Call<TranslationResponse> translateText(@Body TranslationRequest translationRequest);

}
