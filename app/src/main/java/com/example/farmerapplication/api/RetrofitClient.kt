package com.example.farmerapplication.api

import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://180zp95b-7143.inc1.devtunnels.ms/"
    //private const val BASE_URL = "http://localhost:5212/"

    private val gson by lazy {
        GsonBuilder().setLenient().create()
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()

                val originalBody = original.body
                val newBuilder = original.newBuilder()
                    .header("Accept", "application/json; charset=utf-8")
                    .header("Accept-Charset", "utf-8")
                    .header("X-Tunnel-Skip-Anti-Phishing-Page", "true")

                if (originalBody != null) {
                    val utf8MediaType = "application/json; charset=utf-8".toMediaType()
                    val bodyBytes = Buffer().also { originalBody.writeTo(it) }.readByteArray()
                    val utf8Body = bodyBytes.toRequestBody(utf8MediaType)
                    newBuilder
                        .method(original.method, utf8Body)
                        .header("Content-Type", "application/json; charset=utf-8")
                }

                val response = chain.proceed(newBuilder.build())

                // --- Force UTF-8 on the RESPONSE (this is the actual fix) ---
                val responseBody = response.body
                if (responseBody != null) {
                    val utf8MediaType = "application/json; charset=utf-8".toMediaType()
                    val rawBytes = responseBody.bytes()
                    response.newBuilder()
                        .body(rawBytes.toResponseBody(utf8MediaType))
                        .build()
                } else {
                    response
                }
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}