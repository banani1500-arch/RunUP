package com.runup.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthServices {

    private val baseUrl = "https://bsaldana.difadi.net/api"

    suspend fun login(email: String, password: String): Map<String, Any> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/login")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()

                connection.outputStream.use { it.write(body.toByteArray()) }

                val statusCode = connection.responseCode
                val responseBody = if (statusCode == 200)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                mapOf("status" to statusCode, "body" to responseBody)
            } catch (e: Exception) {
                mapOf("status" to 0, "message" to "Error de conexión: ${e.message}")
            }
        }

    suspend fun register(username: String, email: String, password: String): Map<String, Any> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/register")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val body = JSONObject().apply {
                    put("name", username)
                    put("email", email)
                    put("password", password)
                }.toString()

                connection.outputStream.use { it.write(body.toByteArray()) }

                val statusCode = connection.responseCode
                val responseBody = if (statusCode == 201)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                mapOf("status" to statusCode, "body" to responseBody)
            } catch (e: Exception) {
                mapOf("status" to 0, "message" to "Error de conexión: ${e.message}")
            }
        }

    suspend fun sendFcmToken(token: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/fcm-token")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            val body = JSONObject().put("fcm_token", token).toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            connection.responseCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}