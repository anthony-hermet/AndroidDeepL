package com.anthony.deepl.openl.di

import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.view.translation.TranslationViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val deeplAppModule = applicationContext {

    // ViewModel for Translation View
    viewModel { TranslationViewModel(get()) }

    // Provide DeepL Data Repository
    bean { createDeepLService() }

}


fun createDeepLService(): DeepLService {
    val retrofit = Retrofit.Builder()
            .baseUrl(DeepLService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    return retrofit.create(DeepLService::class.java)
}
