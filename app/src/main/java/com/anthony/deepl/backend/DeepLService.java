package com.anthony.deepl.backend;

import com.anthony.deepl.model.TranslationRequestMultiLine;
import com.anthony.deepl.model.TranslationRequestSingleLine;
import com.anthony.deepl.model.TranslationResponseMultiLine;
import com.anthony.deepl.model.TranslationResponseSingleLine;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DeepLService {

    String BASE_URL = "https://www.deepl.com";

    @POST("/jsonrpc")
    Call<TranslationResponseSingleLine> translateSingleLineText(@Body TranslationRequestSingleLine translationRequest);

    @POST("/jsonrpc")
    Call<TranslationResponseMultiLine> translateMultiLineText(@Body TranslationRequestMultiLine translationRequest);

}
