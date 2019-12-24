package com.uestc.request.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Created by PengFeifei on 2019-12-24.
 */
open class RequestViewModel : ViewModel(), OnRequest {

    protected fun <Response> api1(request: suspend () -> Response) {
        api<Response>(viewModelScope) {

            onStart {
                Log.e("Thread-->", Thread.currentThread().name)
                this@RequestViewModel.onStart()
            }

            onRequest {
                Log.e("Thread-->", Thread.currentThread().name)
                request.invoke()
            }

            onResponse {
                Log.e("Thread-->", Thread.currentThread().name)
            }

            onError {
                Log.e("Thread-->", Thread.currentThread().name)
                this@RequestViewModel.onError(it)
            }

            onFinally {
                Log.e("Thread-->", Thread.currentThread().name)
                this@RequestViewModel.onFinally()
            }


        }
    }

    protected fun <Response> api2(apiDSL: APIdsl<Response>.() -> Unit) {
        api(viewModelScope, apiDSL)
    }


    override fun onStart() {

    }

    override fun onError(exception: Exception) {
    }

    override fun onFinally() {
    }
}