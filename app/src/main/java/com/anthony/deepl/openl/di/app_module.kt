package com.anthony.deepl.openl.di

import com.anthony.deepl.openl.backend.DeepLService
import com.anthony.deepl.openl.util.FirebaseManager
import com.anthony.deepl.openl.view.translation.TranslationViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val deeplAppModule = module {

    viewModel { TranslationViewModel(androidApplication(), get(), get()) }

    single { createDeepLService() }
    single { FirebaseManager(androidContext()) }

}


fun createDeepLService(): DeepLService {
    val retrofit = Retrofit.Builder()
            .baseUrl(DeepLService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    return retrofit.create(DeepLService::class.java)
}
