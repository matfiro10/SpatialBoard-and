package com.example.spatialboard.data.remote

import com.example.spatialboard.data.SpatialSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MediaUploadPayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaUploadPayload

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

class SpatialBoardApiClient(
    private val settingsStore: SpatialSettingsStore
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun baseUrlBuilder(): HttpUrl.Builder {
        val rawHost = settingsStore.serverHost.value.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .ifBlank { SpatialSettingsStore.DEFAULT_SERVER_HOST }

        val port = settingsStore.serverPort.value
            .coerceIn(SpatialSettingsStore.MIN_PORT, SpatialSettingsStore.MAX_PORT)

        return HttpUrl.Builder()
            .scheme("http")
            .host(rawHost)
            .port(port)
    }

    suspend fun checkStatus(): Result<ServerStatusResponse> = executeJsonRequest(
        Request.Builder()
            .url(baseUrlBuilder().build())
            .get()
            .build()
    )

    suspend fun getNotesForBeacon(
        uuid: String,
        major: Int,
        minor: Int
    ): Result<List<RemoteNoteDto>> {
        val url = baseUrlBuilder()
            .addPathSegments("api/notes")
            .addQueryParameter("uuid", uuid)
            .addQueryParameter("major", major.toString())
            .addQueryParameter("minor", minor.toString())
            .build()

        return executeJsonRequest<NotesListResponse>(
            Request.Builder().url(url).get().build()
        ).map { it.notes }
    }

    suspend fun createNote(
        uuid: String,
        major: Int,
        minor: Int,
        author: String,
        contentType: String,
        textContent: String?,
        media: MediaUploadPayload?
    ): Result<CreateNoteResponse> {
        val url = baseUrlBuilder()
            .addPathSegments("api/notes")
            .build()

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("beacon_uuid", uuid)
            .addFormDataPart("beacon_major", major.toString())
            .addFormDataPart("beacon_minor", minor.toString())
            .addFormDataPart("author", author.ifBlank { "Anonim" })
            .addFormDataPart("content_type", contentType)

        if (!textContent.isNullOrBlank()) {
            bodyBuilder.addFormDataPart("text_content", textContent)
        }

        if (media != null) {
            bodyBuilder.addFormDataPart(
                name = "file",
                filename = media.fileName,
                body = media.bytes.toRequestBody(media.mimeType.toMediaType())
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyBuilder.build())
            .build()

        return executeJsonRequest(request)
    }

    suspend fun updateNote(
        noteId: String,
        textContent: String
    ): Result<MutateNoteResponse> {
        val url = baseUrlBuilder()
            .addPathSegments("api/notes")
            .addPathSegment(noteId)
            .build()

        val payload = """{"text_content":${json.encodeToString(serializer<String>(), textContent)}}"""
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .put(payload)
            .build()

        return executeJsonRequest(request)
    }

    suspend fun deleteNote(noteId: String): Result<MutateNoteResponse> {
        val url = baseUrlBuilder()
            .addPathSegments("api/notes")
            .addPathSegment(noteId)
            .build()

        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        return executeJsonRequest(request)
    }

    suspend fun seedTestNotes(): Result<SeedTestResponse> {
        val url = baseUrlBuilder()
            .addPathSegments("api/notes/seed-test")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody(null))
            .build()

        return executeJsonRequest(request)
    }

    private suspend inline fun <reified T> executeJsonRequest(request: Request): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(request).await().use { response ->
                    val rawBody = response.body.string()
                    if (!response.isSuccessful) {
                        val serverError = runCatching {
                            json.decodeFromString<ApiErrorResponse>(rawBody).error
                        }.getOrNull()
                        throw IOException(serverError ?: "HTTP ${response.code}")
                    }
                    json.decodeFromString<T>(rawBody)
                }
            }
        }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
    }
}
