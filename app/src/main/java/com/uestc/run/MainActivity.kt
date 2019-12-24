package com.uestc.run

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.uestc.request.handler.Request
import com.uestc.request.model.RequestViewModel
import com.uestc.run.demo.Banner
import com.uestc.run.demo.TestService
import com.uestc.run.demo.WanResponse
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Request.get().init(this, "https://www.wanandroid.com")
        test.setOnClickListener {
            TestViewModel().load()
        }
    }
}


class TestViewModel : RequestViewModel() {


    open fun load() {
        api2<WanResponse<List<Banner>>> {

            onStart {

            }

            onRequest {
                Request.apiService(TestService::class.java).getBanner()
            }

            onResponse {
                Log.e("onResponse-->", Gson().toJson(it))
            }

            onError {

            }

        }

    }

    open fun load2() {

    }


}