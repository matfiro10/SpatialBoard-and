package com.example.spatialboard.data.remote

import androidx.annotation.StringRes
import com.example.spatialboard.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerStatusResponse(
    val status: String,
    val message: String
)

@Serializable
data class NotesListResponse(
    val count: Int,
    val notes: List<RemoteNoteDto>
)

@Serializable
data class RemoteNoteDto(
    val id: Int,
    val beacon: RemoteBeaconDto,
    val author: String = "Anonim",
    @SerialName("content_type") val contentType: String,
    @SerialName("text_content") val textContent: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RemoteBeaconDto(
    val uuid: String,
    val major: Int,
    val minor: Int
)

@Serializable
data class CreateNoteResponse(
    val message: String,
    @SerialName("note_id") val noteId: Int,
    @SerialName("media_url") val mediaUrl: String? = null
)

@Serializable
data class MutateNoteResponse(
    val message: String,
    @SerialName("note_id") val noteId: String
)

@Serializable
data class SeedTestResponse(
    val message: String,
    @SerialName("created_ids") val createdIds: List<Int> = emptyList()
)

@Serializable
data class ApiErrorResponse(
    val error: String? = null
)

sealed interface ServerSyncState {
    data object Idle : ServerSyncState

    data object Syncing : ServerSyncState

    data class Connected(
        @StringRes val messageResId: Int = R.string.server_status_connected,
        val formatArg: Int? = null,
        val serverMessage: String? = null
    ) : ServerSyncState

    data class Error(
        @StringRes val messageResId: Int = R.string.server_error_connection,
        val detail: String? = null
    ) : ServerSyncState
}
