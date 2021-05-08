package com.example.kotlinandroid.Retrofit

import com.example.kotlinandroid.Utils.parameter
import retrofit2.Call
import retrofit2.http.*

interface IUploadParamsAPI {
    @POST("/api/makeupP")
    fun uploadParameter(@Body param: HashMap<String, Int>): Call<parameter>
    //fun uploadParameter(@Field("rColor") rColor: Int,
    //                    @Field("gColor") gColor: Int,
    //                     @Field("bColor") bColor: Int,
    //                    @Field("size") size: Int): Call<parameter>
}