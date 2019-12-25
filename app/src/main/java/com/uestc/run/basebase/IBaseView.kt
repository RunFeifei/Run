package com.uestc.run.basebase

/**
 * Created by PengFeifei on 2019-07-26.
 */
interface IBaseView {

    fun showToast(message: String)

    fun hideLoading()

    fun showLoading()

    fun handleLogin()

    fun handleException(throwable: Throwable): Boolean
}