package com.uestc.run.basebase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uestc.request.model.RequestViewModel


/**
 * Created by PengFeifei on 2018/10/27.
 */

open class BaseViewModel : RequestViewModel() {

    internal val toastLiveData = MutableLiveData<String>()


    private fun showLoading() {
        apiLoading.postValue(true)
    }

    private fun hideLoading() {
        apiLoading.postValue(false)
    }

    fun showToast(message: String) {
        toastLiveData.postValue(message)
    }

    protected fun onException(throwable: Throwable) {
        apiException.postValue(throwable)
    }

    protected fun onSubscribe() {
        showLoading()
    }

    protected fun onFinally() {
        hideLoading()
    }



}
