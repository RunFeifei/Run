package com.uestc.request.model

import java.lang.Exception

/**
 * Created by PengFeifei on 2019-12-24.
 */
interface OnRequest {
    fun onStart()
    fun onError(e:Exception)
    fun onFinally()
}