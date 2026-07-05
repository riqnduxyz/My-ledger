package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class SyncManager {
    private val TAG = "SyncManager"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://kvdb.io/")
        .client(okHttpClient)
        .build()

    private val kvdbService = retrofit.create(KvdbService::class.java)

    /**
     * Calls kvdb.io to create a new private bucket.
     * On success, returns the generated Bucket ID (which acts as the Sync Code).
     */
    suspend fun generateSyncCode(): String {
        return try {
            val response = kvdbService.createBucket()
            if (response.isSuccessful) {
                val bodyString = response.body()?.string()?.trim() ?: ""
                if (bodyString.isNotEmpty()) {
                    Log.d(TAG, "Generated bucket ID: $bodyString")
                    bodyString
                } else {
                    throw Exception("Empty response body from bucket creation")
                }
            } else {
                val errStr = response.errorBody()?.string() ?: ""
                Log.e(TAG, "Bucket creation failed: ${response.code()} $errStr")
                throw Exception("Server returned code ${response.code()}: $errStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sync code", e)
            throw e
        }
    }

    /**
     * Uploads the local ledger payload to the kvdb bucket.
     */
    suspend fun pushData(syncCode: String, payload: SyncPayload): Boolean {
        return try {
            val jsonString = payloadAdapter.toJson(payload)
            val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
            val response = kvdbService.setValue(syncCode, "ledger", requestBody)
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully pushed data to sync code: $syncCode")
                true
            } else {
                val errStr = response.errorBody()?.string() ?: ""
                Log.e(TAG, "Failed to push data: ${response.code()} $errStr")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing data", e)
            false
        }
    }

    /**
     * Downloads the ledger payload from the kvdb bucket.
     */
    suspend fun pullData(syncCode: String): SyncPayload? {
        return try {
            val response = kvdbService.getValue(syncCode, "ledger")
            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: ""
                if (jsonString.isNotEmpty()) {
                    Log.d(TAG, "Successfully pulled data from sync code: $syncCode")
                    payloadAdapter.fromJson(jsonString)
                } else {
                    Log.w(TAG, "Pulled data is empty string")
                    null
                }
            } else {
                val errStr = response.errorBody()?.string() ?: ""
                Log.e(TAG, "Failed to pull data: ${response.code()} $errStr")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling data", e)
            null
        }
    }
}
