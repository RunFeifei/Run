package com.uestc.request.handler

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by PengFeifei on 2019-12-23.
 */
class HeaderInterceptor : Interceptor {

    private var headers = hashMapOf<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headers.forEach { (t, u) ->
            requestBuilder.addHeader(t, u)
        }
        return chain.proceed(requestBuilder.build())
    }

    open fun put(key: String, value: String): HeaderInterceptor {
        headers[key] = value
        return this
    }

    open fun put(headers: HashMap<String, String>): HeaderInterceptor {
        this.headers.putAll(headers)
        return this
    }
}