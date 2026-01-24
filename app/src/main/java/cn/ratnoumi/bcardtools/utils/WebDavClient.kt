package cn.ratnoumi.bcardtools.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object WebDavClient {
    private val client = OkHttpClient()

    fun checkConnection(url: String, user: String, pass: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", null) // WebDAV specific method to check existence/properties
            .header("Authorization", Credentials.basic(user, pass))
            .header("Depth", "0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun uploadFile(url: String, user: String, pass: String, filename: String, content: String): Boolean {
        val targetUrl = if (url.endsWith("/")) "$url$filename" else "$url/$filename"
        val body = content.toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(targetUrl)
            .put(body)
            .header("Authorization", Credentials.basic(user, pass))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful || response.code == 201 || response.code == 204
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun downloadFile(url: String, user: String, pass: String, filename: String): String? {
        val targetUrl = if (url.endsWith("/")) "$url$filename" else "$url/$filename"
        
        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Authorization", Credentials.basic(user, pass))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.string()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
