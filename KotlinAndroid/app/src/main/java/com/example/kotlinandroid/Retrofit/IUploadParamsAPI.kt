package com.example.kotlinandroid.Retrofit

import com.example.kotlinandroid.Utils.parameter
import com.example.kotlinandroid.Utils.strong
import retrofit2.Call
import retrofit2.http.*

interface IUploadParamsAPI {
    @POST("/api/parameter")
    fun uploadParameter(@Body param: HashMap<String, Int>): Call<parameter>
    //fun uploadParameter(@Field("rColor") rColor: Int,
    //                    @Field("gColor") gColor: Int,
    //                     @Field("bColor") bColor: Int,
    //                    @Field("size") size: Int): Call<parameter>

    @POST("/api/strong")
    fun uploadStrong(@Body strong: Int): Call<strong>

    @POST("/api/makeup")
    fun makeUpFace(@Body strong: Int): Call<strong>
}