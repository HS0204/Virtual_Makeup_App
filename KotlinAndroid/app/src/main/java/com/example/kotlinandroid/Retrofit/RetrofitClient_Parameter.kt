package com.example.kotlinandroid.Retrofit

import com.example.kotlinandroid.MainActivity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient_Parameter {
    private var retrofitClient: Retrofit?=null

    val client: Retrofit
        get() {
            if(retrofitClient == null)
                retrofitClient = Retrofit.Builder()
                    .baseUrl("http://192.168.1.102:5000")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            return retrofitClient!!
        }

}