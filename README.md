## 利用Kotlin和协程实现DSL样式的网络请求

本文将基于retrofit2.62、okhttp4.0、Coroutines、viewModel-ktx、LiveData-ktx力求实现一种分层清晰、整洁灵活、处理方便的网络请求。

### 技术栈

为了拥抱Kotlin，okHttp已经将okhttp全部用Kotlin重写。同时okHttp的老朋友retrofit也拥抱了Coroutines推出了retrofit2.60。

DSL方式的语法特性or代码样式在各个开源库中也露脸越来越多。比如刚刚官宣停止维护的Anko和如日中天的Flutter。关于DSL的更多介绍本文最后将给出学习链接。DSL的书写风格在灵活配置请求和处理请求上给人耳目一新、整洁灵活、清晰可读的观感。

### 本文所实现网络请求的特点

DSL方式的请求，自由处理各种start、response、error回调,或者交给BaseViewModel统一处理

回调方式请求，自由处理各种start、response、error回调,或者交给BaseViewModel统一处理

LiveData方式请求,请求直接返回LiveData

DSL方式灵活配置OkHttpClient/Retrofit

### ShowCode

#### 请求的声明如下

```kotlin
interface TestService {
    @GET("/banner/json")
    suspend fun getBanner(): WanResponse<List<Banner>>
}
```

可以看到加成了协程以后的retrofit在生命网络请求以后变得异常简单，不需要用Call或者Observable进行包装，直接返回想要的实体类就好。suspend是Kotlin的关键字，修饰方法是表示为挂起函数，只能运行在协程或者其他挂起函数中。

#### 请求的OKHttp、Retrofit的配置示例如下

```kotlin
 Request.init(context = this.applicationContext, baseUrl = "https://www.wanandroid.com") {
            okHttp {okhttpBuilder->
                //配置okhttp
                okhttpBuilder
            }

            retrofit {retrofitBuilder->
                //配置retrofit
                retrofitBuilder
            }
        }
```

如示例代码所示，通过DSL化的代码书写方式可以灵活的通过okHttp或者retrofit代码块来灵活配置okHttp和retrofit。当然用户可以直接选择不进行任何配置，基本的配置在Request.kt中已经配置完成，在使用默认配置的情况下完全可以不书写okHttp或者retrofit代码块。需要说明的是初始化过程中传入了context，是由于在Request.kt中存在关于持久化cookie的配置，cookie持久化到SP中时需要context来创建SP。还有okHttp和retrofit看似是代码块其实是带函数类型参数的方法而已，正是利用了kotlin对高阶函数、扩展函数、lambda表达式的友好支持和invoke约定才能写出如上所示的DSL化的保证可读性的整洁灵活的代码。

#### 关于DSL方式请求调用示例如下    

```kotlin
class TestViewModel : BaseViewModel() {
    private val service by lazy { Request.apiService(TestService::class.java) }
    val liveData = MutableLiveData<WanResponse<List<Banner>>>()
    fun loadDSL() {
        apiDSL<WanResponse<List<Banner>>> {
            onRequest {
                service.getBanner()
            }
            onResponse {response->
                Log.e("Thread-->onResponse", Thread.currentThread().name)
                Log.e("onResponse-->", Gson().toJson(response))
                liveData.value = response
            }
            onStart {
                Log.e("Thread-->onStart", Thread.currentThread().name)
                false
            }
            onError {
                it.printStackTrace()
                Log.e("Thread-->onError", Thread.currentThread().name)
                true
            }
        }
    }
}
```

如上可见，在onRequest中一股脑塞入请求就可以在onResponse中拿到请求结果。同时也可以在主线程的onStart中自由预处理一些逻辑，可以看到onStart代码块最后默认返回了false，false表示不拦截BaseViewModel中对网络请求开始时的处理（比如弹出统一样式的loading）。如果返回true则表示该行为完全由自己处理。同理针对onError也是一样的道理，可以自己处理错误也可以交给base处理。当然也可以不写onStart和onError完全交给base来处理相关行为，使网络请求代码更简洁。

#### 关于回调方式请求调用示例如下

```kotlin
fun loadCallback() {
    apiCallback({
        service.getBanner()
    }, {
        liveData.value = it//这里是onResponse的回调
    }, {
        true//这里是onStart的回调
    },  onError ={ exception ->
        false
    })
}
```

借助函数类型(Any) -> Any来定义请求的不同回调，比如error的回调可以定义为((Exception) -> Boolean)?。接受exception来处理异常，返回bool类型来决定是否继续交给base来继续处理。同时定义成可空类型可以默认交给base出路。但是显而易见的是这种代码书写方式并不如DSL方式的请求美观和可读性高。

#### 关于直接返回LiveData的请求调用示例如下

```kotlin
fun loadLiveData(): LiveData<Result<WanResponse<List<Banner>>>> {
        return apiLiveData(SupervisorJob() + Dispatchers.Main.immediate, timeoutInMs = 2000) {
            service.getBanner()
        }
 }
```

#### 在V层拿到LiveData后的操作如下

```kotlin
viewModel.loadLiveData().observe(this, Observer {
                when (it) {
                    is Result.Error -> {
                        hideLoading()
                    }
                    is Result.Response -> {
                        hideLoading()
                        it.response.apply {
                            showToast(Gson().toJson(this))
                        }
                    }
                    is Result.Start -> {
                        showLoading()
                    }
                    else ->{//冗余
                    }
                }
})
```

显然这种方式的请求更适合轻量化的请求，适合拿到结果直接去渲染view不经过二次数据处理的场景。因为如上图所示在V层处理start、error回调感觉不是很友好，，在reponse中隐藏loading也是比较繁琐。但好处是V层直接可以拿到包含请求数据的LiveData，操作更加便捷。

#### DSL封装示例

接下来我们以对okhttp和retrofit的请求配置来看下是怎么进行DSL封装的，不多说showcode。

```kotlin
class RequestDsl {
    internal var buidOkHttp: ((OkHttpClient.Builder) -> OkHttpClient.Builder)? = null
    internal var buidRetrofit: ((Retrofit.Builder) -> Retrofit.Builder)? = null
    fun okHttp(builder: ((OkHttpClient.Builder) -> OkHttpClient.Builder)?) {
        this.buidOkHttp = builder
    }
    fun retrofit(builder: ((Retrofit.Builder) -> Retrofit.Builder)?) {
        this.buidRetrofit = builder
    }
}
```

首先是DSL的配置类，主要有2个角色，一个是函数类型的buidOkHttp，一个是以buidOkHttp为参数的配置buidOkHttp的高阶函数okHttp。可见buidOkHttp变量是一个可空类型的输入和返回是非空的OkHttpClient.Builder类型的函数，既然是可空类型的我们在初始化调用时就可以选择配置OkHttpClient.Builder与否。既然输入返回都是OkHttpClient.Builder我们就可以拿到既定的带有初始化配置的OkHttpClient.Builder进行进一部配置，只要最后返回OkHttpClient.Builder就好，同时OkHttpClient.Builder采用了建造者模式我们可以拿到builder引用之后进行二次配置最后原样返回builder的引用。

下面是初始化方法的具体实现

```kotlin
private fun initRequest(okHttpBuilder: OkHttpClient.Builder, requestDSL: (RequestDsl.() -> Unit)? = null) {
    val dsl = if (requestDSL != null) RequestDsl().apply(requestDSL) else null
    val finalOkHttpBuilder = dsl?.buidOkHttp?.invoke(okHttpBuilder) ?: okHttpBuilder
    val retrofitBuilder = Retrofit.Builder()
        .baseUrl(this.baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(finalOkHttpBuilder.build())
    val finalRetrofitBuilder = dsl?.buidRetrofit?.invoke(retrofitBuilder) ?: retrofitBuilder
    this.retrofit = finalRetrofitBuilder.build()
}
```

这个方法就比较简单，requestDSL定义为可空类型，可以选择配置或者不进行额外配置。

此时我们再看一下比较常用的apply方法的如下定义，我们在apply方法中就进入到了泛型T的内部空间，this关键字就指代的是泛型自己 ，可以在内部调用泛型的成员。

```kotlin
public inline fun <T> T.apply(block: T.() -> Unit): T
```

相似的requestDSL也是和apply方法中的block是一样的类型。一旦选择了进行配置就可以像apply方法一样，在RequestDsl函数内部选择性的调用okHttp或者retrofit方法。那么在关于DSL方式请求调用也和配置请求一样如出一辙不再多说。

#### 协程的使用

```kotlin
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
```

整个项目中关于协程的使用就只有这一个方法，其中viewModelScope可以是在viewmodel-ktx中定义的协程作用域，来避免我们书写重复的代码。同在ViewModel.onCleared()` 被调用的时候，`viewModelScope会自动取消作用域内的所有协程。在执行请求任务request()时会切换到IO线程执行，拿到结果后通过onResponse告诉上层代码。

#### 最后关于base中的统一处理回调的示例

如下代码都是在BaseViewModel中定义的

```kotlin
protected fun <Response> apiDSL(apiDSL: ViewModelDsl<Response>.() -> Unit) {
    api<Response> {
        onRequest {
            ViewModelDsl<Response>().apply(apiDSL).request()
        }
        onResponse {
            ViewModelDsl<Response>().apply(apiDSL).onResponse?.invoke(it)
        }
        onStart {
            val override = ViewModelDsl<Response>().apply(apiDSL).onStart?.invoke()
            if (override == null || !override) {
                onApiStart()
            }
            override
        }
        onError { error ->
            val override = ViewModelDsl<Response>().apply(apiDSL).onError?.invoke(error)
            if (override == null || !override) {
                onApiError(error)
            }
            override
        }
    }
}
```

我们重点关注api请求发起时start、出错时error的处理，其中涉及的onApiStart()和onApiFinally()的定义如下

```kotlin
protected open fun onApiStart() {
    apiLoading.value = true//apiLoading: MutableLiveData<Boolean>
}
protected open fun onApiError(e: Exception?) {
    apiLoading.value = false
    apiException.value = e//apiException: MutableLiveData<Throwable>
}
```

在方法apiDSL中进行了一次DSL的嵌套，apiDSL是业务代码配置的代码。如果apiDSL没有配置onStart或者最后返回了false，那么表示还需要base进一步处理start的回调.此时就会调用base中定义的onApiStart()更新loading的liveData，然后再V层中拿到apiLoading统一弹出或关闭loadingDialog。