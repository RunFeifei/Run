package com.uestc.request.handler

import android.content.Context
import com.uestc.request.BuildConfig
import com.uestc.request.cookie.CookieJar
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession

/**
 * Created by PengFeifei on 2019-12-23.
 */
object Request {

    private lateinit var retrofit: Retrofit
    internal lateinit var appContext: Context
    private lateinit var baseUrl: String

    //todo 监测变化 重置
    private lateinit var headers: HeaderInterceptor

    @JvmOverloads
    fun init(context: Context, baseUrl: String, requestDSL: (RequestDsl.() -> Unit)? = null) {
        this.appContext = context.applicationContext
        this.baseUrl = baseUrl
        this.headers = HeaderInterceptor()
        init(requestDSL)
    }

    private fun init(requestDSL: (RequestDsl.() -> Unit)? = null) {
        initRetrofit(getOkHttp(), requestDSL)
    }

    private fun getOkHttp(): OkHttpClient.Builder {
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
    }

    private fun initRetrofit(okHttpBuilder: OkHttpClient.Builder, requestDSL: (RequestDsl.() -> Unit)? = null) {
        val dsl = if (requestDSL != null) RequestDsl().apply(requestDSL) else null
        val finalOkHttpBuilder = dsl?.buidOkHttp?.invoke(okHttpBuilder) ?: okHttpBuilder
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(finalOkHttpBuilder.build())
        val finalRetrofitBuilder = dsl?.buidRetrofit?.invoke(retrofitBuilder) ?: retrofitBuilder
        this.retrofit = finalRetrofitBuilder.build()
    }

    fun <Service> apiService(service: Class<Service>): Service {
        return retrofit.create(service)
    }

    fun put(key: String, value: String) {
        headers.put(key, value)
    }

}

class RequestDsl {

    internal var buidOkHttp: ((OkHttpClient.Builder) -> OkHttpClient.Builder)? = null

    internal var buidRetrofit: ((Retrofit.Builder) -> Retrofit.Builder)? = null

    infix fun okHttp(builder: ((OkHttpClient.Builder) -> OkHttpClient.Builder)?) {
        this.buidOkHttp = builder
    }

    infix fun retrofit(builder: ((Retrofit.Builder) -> Retrofit.Builder)?) {
        this.buidRetrofit = builder
    }

}