package com.example.personalfitnessapp.ai.backend

import android.util.Log
import com.example.personalfitnessapp.ai.AiModelConfig
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private const val TAG = "AiHttp"
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

/** A non-2xx answer: the status, the body (never contains the request's key) and Retry-After. */
data class HttpError(val code: Int, val body: String, val retryAfterHeader: String?)

sealed interface HttpOutcome {
    data class Ok(val json: JsonObject) : HttpOutcome
    data class Error(val error: HttpError) : HttpOutcome
    data class Failed(val failure: BackendFailure) : HttpOutcome
}

/** The one thing backends need from HTTP; an interface so tests can capture request bodies. */
interface JsonPoster {
    val json: Json
    suspend fun postJson(url: String, headers: Map<String, String>, body: JsonObject): HttpOutcome
}

@Singleton
class AiHttp @Inject constructor() : JsonPoster {
    private val client = OkHttpClient.Builder()
        .connectTimeout(AiModelConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AiModelConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(AiModelConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override val json = Json { ignoreUnknownKeys = true }

    override suspend fun postJson(url: String, headers: Map<String, String>, body: JsonObject): HttpOutcome {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return try {
            val response = execute(request)
            if (response.code in 200..299) {
                val parsed = runCatching { json.parseToJsonElement(response.body) as JsonObject }.getOrNull()
                parsed?.let { HttpOutcome.Ok(it) }
                    ?: HttpOutcome.Failed(BackendFailure(FailureKind.BAD_RESPONSE, "Response wasn't JSON"))
            } else {
                Log.w(TAG, "HTTP ${response.code} from $url: ${response.body.take(800)}")
                HttpOutcome.Error(HttpError(response.code, response.body, response.retryAfter))
            }
        } catch (e: IOException) {
            HttpOutcome.Failed(classifyIoException(e))
        }
    }

    private data class RawResponse(val code: Int, val body: String, val retryAfter: String?)

    private suspend fun execute(request: Request): RawResponse = suspendCancellableCoroutine { cont ->
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val raw = try {
                    response.use { RawResponse(it.code, it.body.string(), it.header("Retry-After")) }
                } catch (e: IOException) {
                    if (!cont.isCancelled) cont.resumeWith(Result.failure(e))
                    return
                }
                if (!cont.isCancelled) cont.resume(raw)
            }
        })
    }
}

/** Offline is final (every provider will fail); a slow or dropped answer is worth retrying elsewhere. */
fun classifyIoException(e: IOException): BackendFailure = when (e) {
    is UnknownHostException, is ConnectException -> BackendFailure(FailureKind.NETWORK, e.javaClass.simpleName)
    is SocketTimeoutException -> BackendFailure(FailureKind.TIMEOUT, "Timed out")
    is InterruptedIOException -> BackendFailure(FailureKind.TIMEOUT, "Timed out")
    else -> BackendFailure(FailureKind.OVERLOADED, e.javaClass.simpleName + ": " + (e.message ?: ""))
}

/**
 * Seconds to wait from either a Retry-After header or, for Google, a RetryInfo
 * `retryDelay: "34s"` anywhere in the error body.
 */
fun retryAfterMillis(error: HttpError, json: Json): Long? {
    error.retryAfterHeader?.trim()?.toDoubleOrNull()?.let { return (it * 1000).toLong() }
    val root = runCatching { json.parseToJsonElement(error.body) }.getOrNull() ?: return null
    return findRetryDelay(root)
}

private fun findRetryDelay(element: JsonElement): Long? = when (element) {
    is JsonObject -> {
        val direct = (element["retryDelay"] ?: element["retry_delay"]) as? JsonPrimitive
        direct?.content?.removeSuffix("s")?.toDoubleOrNull()?.let { (it * 1000).toLong() }
            ?: element.values.firstNotNullOfOrNull(::findRetryDelay)
    }
    is JsonArray -> element.firstNotNullOfOrNull(::findRetryDelay)
    else -> null
}

/** Daily quotas reset at midnight Pacific, so a per-day 429 deserves a long cooldown. */
fun looksLikeDailyQuota(body: String): Boolean =
    Regex("per[ _-]?day|daily|\\bRPD\\b|\\bTPD\\b", RegexOption.IGNORE_CASE).containsMatchIn(body)
