package com.uestc.request.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Created by PengFeifei on 2019-12-24.
 *
 * https://developer.android.google.cn/topic/libraries/architecture/coroutines#livedata
 */
open class RequestViewModel : ViewModel() {

    open val apiException: MutableLiveData<Throwable> = MutableLiveData()
    open val apiLoading: MutableLiveData<Boolean> = MutableLiveData()


    @JvmOverloads
    protected fun <Response> api(
        request: suspend () -> Response,
        onResponse: ((Response) -> Unit),
        onStart: (() -> Boolean)? = null,
        onError: (() -> Boolean)? = null,
        onFinally: (() -> Boolean)? = null
    ) {

        apiDSL<Response> {

            onRequest {
                request.invoke()
            }

            onResponse {
                onResponse.invoke(it)
            }

            onStart {
                val override = onStart?.invoke()
                if (override == null || !override) {
                    apiLoading.value = true
                }
            }

            onError {
                val override = onError?.invoke()
                if (override == null || !override) {
                    apiException.value = it
                }
            }

            onFinally {
                val override = onFinally?.invoke()
                if (override == null || !override) {
                    apiLoading.value = false
                }
            }
        }
    }

    protected fun <Response> apiDSL(apiDSL: ViewModelDsl<Response>.() -> Unit) {
        api(viewModelScope, apiDSL)
    }

    protected fun <Response> apiLiveData(
        context: CoroutineContext = EmptyCoroutineContext,
        timeoutInMs: Long = 5000L,
        apiDSL: LiveDatalDsl<Response>.() -> Unit
    ): LiveData<Result<Response>> {

        return androidx.lifecycle.liveData(context, timeoutInMs) {
            emit(Result.Start())
            Log.e("Thread-->start", Thread.currentThread().name)
            try {
                emit(withContext(Dispatchers.IO) {
                    Log.e("Thread-->request", Thread.currentThread().name)
                    Result.Response(LiveDatalDsl<Response>().apply(apiDSL).request())
                })
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Result.Error(e))
            } finally {
                Log.e("Thread-->finally", Thread.currentThread().name)
                emit(Result.Finally())
            }
        }
    }
}


sealed class Result<T> {
    class Start<T> : Result<T>()
    class Finally<T> : Result<T>()
    data class Response<T>(val response: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
}
