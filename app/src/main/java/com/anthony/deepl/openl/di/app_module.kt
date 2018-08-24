package com.anthony.deepl.openl.di

import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.view.translation.TranslationViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val deeplAppModule = module {

    // ViewModel for Translation View
    viewModel { TranslationViewModel(get()) }

    // Provide DeepL Data Repository
    single { createDeepLService() }

}


fun createDeepLService(): DeepLService {
    val retrofit = Retrofit.Builder()
            .baseUrl(DeepLService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    return retrofit.create(DeepLService::class.java)
}
