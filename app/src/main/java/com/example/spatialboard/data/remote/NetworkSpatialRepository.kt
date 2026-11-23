package com.example.spatialboard.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.paging.PagingData
import com.example.spatialboard.R
import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NetworkSpatialRepository(
    context: Context,
    private val apiClient: SpatialBoardApiClient
) : SpatialRepository {

    private val appContext = context.applicationContext
    private val localTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val boardsFlow = MutableStateFlow<List<BoardItem>>(emptyList())

    private val _syncState = MutableStateFlow<ServerSyncState>(ServerSyncState.Idle)
    val syncState: StateFlow<ServerSyncState> = _syncState.asStateFlow()

    private var lastSyncedBeacons: List<BeaconKey> = emptyList()

    private data class BeaconKey(
        val uuid: String,
        val major: Int,
        val minor: Int
    )

    override fun observeAllBoards(): Flow<List<BoardItem>> = boardsFlow.asStateFlow()

    override fun observeBoardsPaged(): Flow<PagingData<BoardItem>> =
        boardsFlow.map { PagingData.from(it) }

    override fun observeBoardById(boardId: String): Flow<BoardItem?> =
        boardsFlow
            .map { list -> list.find { it.id == boardId } }
            .distinctUntilChanged()

    override fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>> {
        val idSet = beaconIds.toSet()
        return boardsFlow.map { list ->
            list.filter { it.beaconId == null || it.beaconId in idSet }
        }
    }

    suspend fun testConnection(): Result<String> {
        _syncState.value = ServerSyncState.Syncing
        return apiClient.checkStatus()
            .map { status -> status.message }
            .onSuccess { msg ->
                _syncState.value = ServerSyncState.Connected(serverMessage = msg)
            }
            .onFailure { err ->
                _syncState.value = ServerSyncState.Error(
                    messageResId = R.string.server_error_connection,
                    detail = err.message
                )
            }
    }

    suspend fun refreshForBeacons(detectedBeacons: List<DetectedBeacon>) {
        val keys = detectedBeacons
            .map { BeaconKey(it.uuid, it.major, it.minor) }
            .distinct()

        lastSyncedBeacons = keys

        if (keys.isEmpty()) {
            boardsFlow.value = emptyList()
            return
        }

        _syncState.value = ServerSyncState.Syncing

        val results = coroutineScope {
            keys.map { key ->
                async {
                    apiClient.getNotesForBeacon(key.uuid, key.major, key.minor)
                }
            }.awaitAll()
        }

        val firstError = results.firstOrNull { it.isFailure }?.exceptionOrNull()
        val fetchedNotes = results
            .mapNotNull { it.getOrNull() }
            .flatten()
            .distinctBy { it.id }
            .map { it.toBoardItem() }

        if (firstError != null && fetchedNotes.isEmpty()) {
            _syncState.value = ServerSyncState.Error(
                messageResId = R.string.server_error_sync,
                detail = firstError.message
            )
        } else {
            boardsFlow.value = fetchedNotes
            _syncState.value = ServerSyncState.Connected(
                messageResId = R.string.server_status_synced_count,
                formatArg = fetchedNotes.size
            )
        }
    }

    private suspend fun refreshLastBeacons() {
        if (lastSyncedBeacons.isEmpty()) return
        val results = coroutineScope {
            lastSyncedBeacons.map { key ->
                async {
                    apiClient.getNotesForBeacon(key.uuid, key.major, key.minor)
                }
            }.awaitAll()
        }
        val fetchedNotes = results
            .mapNotNull { it.getOrNull() }
            .flatten()
            .distinctBy { it.id }
            .map { it.toBoardItem() }
        boardsFlow.value = fetchedNotes
    }

    override suspend fun upsertBoard(board: BoardItem) {
        val isExistingServerNote = boardsFlow.value.any { it.id == board.id }
        val combinedText = formatTextPayload(board.title, board.content)

        _syncState.value = ServerSyncState.Syncing

        if (isExistingServerNote) {
            apiClient.updateNote(board.id, combinedText)
                .onSuccess {
                    boardsFlow.update { list ->
                        list.map { if (it.id == board.id) board else it }
                    }
                    _syncState.value = ServerSyncState.Connected(
                        messageResId = R.string.server_status_note_updated
                    )
                }
                .onFailure { err ->
                    _syncState.value = ServerSyncState.Error(
                        messageResId = R.string.server_error_update,
                        detail = err.message
                    )
                }
            return
        }

        val beaconKey = parseBeaconId(board.beaconId)
        if (beaconKey == null) {
            _syncState.value = ServerSyncState.Error(
                messageResId = R.string.server_error_beacon_required
            )
            return
        }

        val mediaPayload = board.mediaUri
            ?.takeIf { it.startsWith("content://") || it.startsWith("file://") }
            ?.let { readMediaPayload(Uri.parse(it)) }

        val contentType = when {
            mediaPayload == null -> "text"
            mediaPayload.mimeType.equals("image/gif", ignoreCase = true) ||
                    mediaPayload.fileName.endsWith(".gif", ignoreCase = true) -> "gif"
            else -> "image"
        }

        apiClient.createNote(
            uuid = beaconKey.uuid,
            major = beaconKey.major,
            minor = beaconKey.minor,
            author = board.roomLabel.ifBlank { "Android" },
            contentType = contentType,
            textContent = combinedText,
            media = mediaPayload
        ).onSuccess {
            val activeKeys = (lastSyncedBeacons + beaconKey).distinct()
            lastSyncedBeacons = activeKeys
            refreshLastBeacons()
            _syncState.value = ServerSyncState.Connected(
                messageResId = R.string.server_status_note_saved
            )
        }.onFailure { err ->
            _syncState.value = ServerSyncState.Error(
                messageResId = R.string.server_error_save,
                detail = err.message
            )
        }
    }

    override suspend fun upsertBoards(boards: List<BoardItem>) {
        boards.forEach { upsertBoard(it) }
    }

    override suspend fun replaceBoards(boards: List<BoardItem>) {
        boardsFlow.value = boards
    }

    override suspend fun deleteBoard(boardId: String) {
        _syncState.value = ServerSyncState.Syncing
        apiClient.deleteNote(boardId)
            .onSuccess {
                boardsFlow.update { list -> list.filterNot { it.id == boardId } }
                _syncState.value = ServerSyncState.Connected(
                    messageResId = R.string.server_status_note_deleted
                )
            }
            .onFailure { err ->
                _syncState.value = ServerSyncState.Error(
                    messageResId = R.string.server_error_delete,
                    detail = err.message
                )
            }
    }

    private fun RemoteNoteDto.toBoardItem(): BoardItem {
        val rawText = textContent.orEmpty().trim()
        val lines = rawText.lines()
        val parsedTitle = lines.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: appContext.getString(
                R.string.server_note_default_title,
                id,
                contentType.uppercase()
            )
        val parsedContent = if (lines.size > 1) {
            lines.drop(1).joinToString("\n").trim()
        } else {
            ""
        }

        val formattedTime = formatServerTimestamp(createdAt)

        val zoneText = if (author.isNotBlank() && author != "Anonim") {
            "$author • #${beacon.major}-${beacon.minor}"
        } else {
            "Beacon #${beacon.major}-${beacon.minor}"
        }

        return BoardItem(
            id = id.toString(),
            title = parsedTitle,
            roomLabel = zoneText,
            beaconId = "${beacon.uuid}:${beacon.major}:${beacon.minor}",
            updatedAt = formattedTime,
            content = parsedContent,
            mediaUri = mediaUrl,
            tags = listOf(contentType)
        )
    }

    private fun formatServerTimestamp(rawTimestamp: String?): String {
        val cleaned = rawTimestamp?.trim().orEmpty()
        if (cleaned.isEmpty()) {
            return appContext.getString(R.string.server_source_label)
        }

        val normalizedIso = cleaned.replace(' ', 'T')
        val formatted = runCatching {
            val instant = runCatching { OffsetDateTime.parse(normalizedIso).toInstant() }
                .getOrElse {
                    LocalDateTime.parse(normalizedIso).toInstant(ZoneOffset.UTC)
                }
            instant.atZone(ZoneId.systemDefault()).format(localTimeFormatter)
        }.getOrNull()

        return formatted
            ?: cleaned.substringAfter(" ").take(5).takeIf { it.isNotBlank() }
            ?: appContext.getString(R.string.server_source_label)
    }

    private fun formatTextPayload(title: String, content: String): String {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        return when {
            cleanTitle.isEmpty() -> cleanContent
            cleanContent.isEmpty() -> cleanTitle
            else -> "$cleanTitle\n$cleanContent"
        }
    }

    private fun parseBeaconId(beaconId: String?): BeaconKey? {
        if (beaconId.isNullOrBlank()) return null
        val lastColon = beaconId.lastIndexOf(':')
        if (lastColon <= 0) return null
        val secondLastColon = beaconId.lastIndexOf(':', lastColon - 1)
        if (secondLastColon <= 0) return null

        val uuid = beaconId.substring(0, secondLastColon)
        val major = beaconId.substring(secondLastColon + 1, lastColon).toIntOrNull() ?: return null
        val minor = beaconId.substring(lastColon + 1).toIntOrNull() ?: return null
        return BeaconKey(uuid, major, minor)
    }

    private suspend fun readMediaPayload(uri: Uri): MediaUploadPayload? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = appContext.contentResolver
            var displayName: String? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = cursor.getString(idx)
                }
            }

            val rawMime = resolver.getType(uri).orEmpty().lowercase()
            val resolvedName = displayName ?: uri.lastPathSegment ?: "upload.jpg"
            val normalizedMime = when {
                rawMime in setOf("image/jpeg", "image/png", "image/gif", "image/webp") -> rawMime
                resolvedName.endsWith(".gif", ignoreCase = true) -> "image/gif"
                resolvedName.endsWith(".png", ignoreCase = true) -> "image/png"
                resolvedName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null

            MediaUploadPayload(
                fileName = resolvedName,
                mimeType = normalizedMime,
                bytes = bytes
            )
        }.getOrNull()
    }
}
