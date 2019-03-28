package com.anthony.deepl.openl.model

sealed class ResultResource<out T : Any>

data class SuccessResource<out T : Any>(val data: T) : ResultResource<T>()

data class FailureResource(val error: Throwable?, val message: String?) : ResultResource<Nothing>()

data class LoadingResource(val message: String? = null) : ResultResource<Nothing>()

data class IdleResource(val message: String? = null) : ResultResource<Nothing>()