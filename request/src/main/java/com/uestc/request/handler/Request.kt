package com.uestc.request.handler

import android.content.Context
import com.uestc.request.BuildConfig
import com.uestc.request.cookie.CookieJar
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession

/**
 * Created by PengFeifei on 2019-12-23.
 */
class Request private constructor() {


    companion object {
        private var instance: Request? = null
            get() {
                if (field == null) {
                    field = Request()
                }
                return field
            }

        open fun get(): Request {
            return instance!!
        }

        open fun <Service> apiService(service: Class<Service>): Service {
            return get().retrofit.create(service)
        }
    }


    private lateinit var retrofit: Retrofit

    internal lateinit var appContext: Context
    internal lateinit var baseUrl: String

    //todo 监测变化 重置
    private lateinit var headers: HeaderInterceptor

    open fun init(context: Context, baseUrl: String, headers: HashMap<String, String>? = null) {
        this.appContext = context.applicationContext
        this.baseUrl = baseUrl
        this.headers = HeaderInterceptor()
        headers?.apply {
            this@Request.headers.put(this)
        }
        init()
    }

    private fun init() {
        initRetrofit(getOkHttp())
    }

    //TODO client的配置DSL化
    private fun getOkHttp(): OkHttpClient {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(
                        null,
                        arrayOf(XTrustManager()),
                        SecureRandom()
                )
                val sslSocketFactory = sslContext.socketFactory
                builder.sslSocketFactory(sslSocketFactory, XTrustManager())
                builder.hostnameVerifier { _: String?, _: SSLSession? -> true }
                builder.addNetworkInterceptor(LoggingInterceptor())
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return builder
                .cache(Cache(appContext.cacheDir, 10 * 1024 * 1024L))
                .addInterceptor(headers)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cookieJar(CookieJar.getInstance())
                .build()
    }

    private fun initRetrofit(okHttp: OkHttpClient) {
        retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttp)
                .build()
    }

    open fun put(key: String, value: String) {
        headers.put(key, value)
    }

}