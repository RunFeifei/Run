package com.uestc.run

import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.uestc.request.handler.Request
import com.uestc.run.basebase.BaseActivity
import com.uestc.run.basebase.BaseViewModel
import com.uestc.run.net.Banner
import com.uestc.run.net.TestService
import com.uestc.run.net.WanResponse
import com.uestc.run.widget.get
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity<TestViewModel>() {

    override fun initViewModel(): TestViewModel {
        return get(TestViewModel::class.java)
    }

    override fun layoutId(): Int {
        return R.layout.activity_main
    }

    override fun initPage(savedInstanceState: Bundle?) {
        Request.get().init(this, "https://www.wanandroid.com")
        test.setOnClickListener {
            viewModel.loadDSL()
        }
        test2.setOnClickListener {
            viewModel.loadCommon()
        }

    }
}


class TestViewModel : BaseViewModel() {


    open fun loadDSL() {
        apiDSL<WanResponse<List<Banner>>> {

            onStart {
                Log.e("Thread-->onStart", Thread.currentThread().name)
            }

            onRequest {
                Log.e("Thread-->onRequest", Thread.currentThread().name)
                Request.apiService(TestService::class.java).getBanner()
            }

            onResponse {
                Log.e("Thread-->onResponse", Thread.currentThread().name)
                Log.e("onResponse-->", Gson().toJson(it))
            }

            onError {
                Log.e("Thread-->onError", Thread.currentThread().name)
            }

        }

    }

    open fun loadCommon() {
        api({
            Log.e("Thread-->onRequest", Thread.currentThread().name)
            Request.apiService(TestService::class.java).getBanner()
        }, {
            Log.e("Thread-->onResponse", Thread.currentThread().name)
            Log.e("onResponse-->", Gson().toJson(it))
        })

    }


}