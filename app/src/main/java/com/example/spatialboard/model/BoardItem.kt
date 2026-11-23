package com.example.spatialboard.model

data class BoardItem(
    val id: String,
    val title: String,
    val roomLabel: String,
    val beaconId: String?,
    val updatedAt: String,
    val content: String,
    val mediaUri: String? = null,
    val tags: List<String> = emptyList()
)
