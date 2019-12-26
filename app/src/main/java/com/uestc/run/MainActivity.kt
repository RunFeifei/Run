package com.uestc.run

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
        test3.setOnClickListener {
            viewModel.loadLiveData()
        }

    }

    override fun initLivedata(viewModel: TestViewModel) {
        viewModel.liveData.observe(this, Observer {
            Toast.makeText(this, Gson().toJson(it), Toast.LENGTH_SHORT).show()
        })
    }
}


class TestViewModel : BaseViewModel() {

    val liveData = MutableLiveData<WanResponse<List<Banner>>>()


    open fun loadDSL() {
        apiDSL<WanResponse<List<Banner>>> {

            onStart {
                apiLoading.value = true
                Log.e("Thread-->onStart", Thread.currentThread().name)
            }

            onRequest {
                Log.e("Thread-->onRequest", Thread.currentThread().name)
                Request.apiService(TestService::class.java).getBanner()
            }

            onResponse {
                apiLoading.value = false
                Log.e("Thread-->onResponse", Thread.currentThread().name)
                Log.e("onResponse-->", Gson().toJson(it))
                liveData.value = it
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
            liveData.value = it
        })

    }

    open fun loadLiveData():LiveData<WanResponse<List<Banner>>> {
       return apiLiveData(Request.apiService(TestService::class.java).getBanner())
    }


}