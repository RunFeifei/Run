package com.uestc.run.net

import retrofit2.http.GET
import java.io.Serializable

/**
 * Created by PengFeifei on 2019-12-24.
 */

interface TestService {
    @GET("/banner/json")
    suspend fun getBanner(): WanResponse<List<Banner>>
}

data class WanResponse<out T>(val errorCode: Int, val errorMsg: String, val data: T):Serializable


data class Banner(
    val desc: String,
    val id: Int,
    val imagePath: String,
    val isVisible: Int,
    val order: Int,
    val title: String,
    val type: Int,
    val url: String
)