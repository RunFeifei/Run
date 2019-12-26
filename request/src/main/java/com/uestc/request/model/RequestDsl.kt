package com.uestc.request.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by PengFeifei on 2019-12-24.
 *
 * https://juejin.im/post/5d4d17e5f265da039401f6ea
 */

internal inline fun <Response> api(viewModelScope: CoroutineScope, apiDSL: ViewModelDsl<Response>.() -> Unit) {
    ViewModelDsl<Response>().apply(apiDSL).launch(viewModelScope)
}

class ViewModelDsl<Response> {

    internal lateinit var request: suspend () -> Response

    private var onStart: (() -> Unit)? = null

    private var onResponse: ((Response) -> Unit)? = null

    private var onError: ((Exception) -> Unit)? = null

    private var onFinally: (() -> Unit)? = null


    infix fun onStart(onStart: (() -> Unit)?) {
        this.onStart = onStart
    }

    infix fun onRequest(request: suspend () -> Response) {
        this.request = request
    }

    infix fun onResponse(onResponse: ((Response) -> Unit)?) {
        this.onResponse = onResponse
    }

    infix fun onError(onError: ((Exception) -> Unit)?) {
        this.onError = onError
    }

    infix fun onFinally(onFinally: (() -> Unit)?) {
        this.onFinally = onFinally
    }


    internal fun launch(viewModelScope: CoroutineScope) {
        viewModelScope.launch(context = Dispatchers.Main) {
            onStart?.invoke()
            try {
                val response = withContext(Dispatchers.IO) {
                    request()
                }
                onResponse?.invoke(response)
            } catch (e: Exception) {
                e.printStackTrace()
                onError?.invoke(e)
            } finally {
                onFinally?.invoke()
            }
        }
    }


}


class LiveDatalDsl<Response> {

    internal lateinit var request: suspend () -> Response

    infix fun onRequest(request: suspend () -> Response) {
        this.request = request
    }
}