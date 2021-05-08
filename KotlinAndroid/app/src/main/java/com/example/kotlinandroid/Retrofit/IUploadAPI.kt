package com.example.kotlinandroid.Retrofit

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface IUploadAPI : IDownloadAPI {
    @Multipart
    @POST("/api/upload")
    fun uploadFile(@Part file:MultipartBody.Part): Call<String>
}