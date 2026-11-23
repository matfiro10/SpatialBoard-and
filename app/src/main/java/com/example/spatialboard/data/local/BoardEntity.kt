package com.example.spatialboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.spatialboard.model.BoardItem

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val roomLabel: String,
    val beaconId: String?,
    val updatedAt: String,
    val content: String,
    val mediaUri: String?,
    val tags: List<String>,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

fun BoardEntity.toDomain(): BoardItem = BoardItem(
    id = id,
    title = title,
    roomLabel = roomLabel,
    beaconId = beaconId,
    updatedAt = updatedAt,
    content = content,
    mediaUri = mediaUri,
    tags = tags
)

fun BoardItem.toEntity(updatedAtEpoch: Long = System.currentTimeMillis()): BoardEntity = BoardEntity(
    id = id,
    title = title,
    roomLabel = roomLabel,
    beaconId = beaconId,
    updatedAt = updatedAt,
    content = content,
    mediaUri = mediaUri,
    tags = tags,
    updatedAtEpoch = updatedAtEpoch
)
