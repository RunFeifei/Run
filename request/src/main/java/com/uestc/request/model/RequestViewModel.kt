package com.uestc.request.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Created by PengFeifei on 2019-12-24.
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

        api<Response>(viewModelScope) {

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

    protected fun <Response> apiDSL(apiDSL: APIdsl<Response>.() -> Unit) {
        api(viewModelScope, apiDSL)
    }
}