package com.example.spatialboard.ui.overlays

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.spatialboard.R
import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.model.BoardItem
import com.example.spatialboard.ui.components.createSpatialImageLoader
import com.example.spatialboard.ui.components.rememberOverlayContentMaxHeight

@Composable
fun BoardDetailsOverlayContent(
    board: BoardItem,
    onSave: (BoardItem) -> Unit,
    modifier: Modifier = Modifier,
    nearbyBeacons: List<DetectedBeacon> = emptyList()
) {
    val context = LocalContext.current
    val maxContentHeight = rememberOverlayContentMaxHeight()
    val updatedNowLabel = stringResource(R.string.board_updated_just_now)

    var isEditing by rememberSaveable(board.id) { mutableStateOf(false) }
    var draftTitle by rememberSaveable(board.id, isEditing) { mutableStateOf(board.title) }
    var draftRoom by rememberSaveable(board.id, isEditing) { mutableStateOf(board.roomLabel) }
    var draftContent by rememberSaveable(board.id, isEditing) { mutableStateOf(board.content) }
    var draftMediaUri by rememberSaveable(board.id, isEditing) { mutableStateOf(board.mediaUri) }
    var draftBeaconId by rememberSaveable(board.id, isEditing) { mutableStateOf(board.beaconId) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            draftMediaUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditing) {
            item(key = "edit_media") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentMedia = draftMediaUri
                    if (currentMedia != null) {
                        BoardMediaPreview(mediaUri = currentMedia)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.board_media_change))
                            }

                            OutlinedButton(
                                onClick = { draftMediaUri = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.board_media_remove),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.board_media_add),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item(key = "edit_fields") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text(stringResource(R.string.board_field_title)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = draftRoom,
                        onValueChange = { draftRoom = it },
                        label = { Text(stringResource(R.string.board_field_room)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        label = { Text(stringResource(R.string.board_field_content)) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.board_field_beacon),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val boundBeaconId = draftBeaconId
                        if (boundBeaconId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = nearbyBeacons.firstOrNull { it.id == boundBeaconId }?.name
                                        ?: boundBeaconId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { draftBeaconId = null }) {
                                    Text(stringResource(R.string.board_beacon_unbind))
                                }
                            }
                        } else if (nearbyBeacons.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                nearbyBeacons.take(4).forEach { beacon ->
                                    Surface(
                                        onClick = { draftBeaconId = beacon.id },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${beacon.name} (${beacon.rssi} dBm)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.board_beacon_none_nearby),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "edit_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            onSave(
                                board.copy(
                                    title = draftTitle.trim(),
                                    roomLabel = draftRoom.trim().ifBlank { board.roomLabel },
                                    content = draftContent.trim(),
                                    mediaUri = draftMediaUri,
                                    beaconId = draftBeaconId,
                                    updatedAt = updatedNowLabel
                                )
                            )
                            isEditing = false
                        },
                        enabled = draftTitle.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        } else {
            item(key = "board_meta") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = board.roomLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.board_updated_format, board.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (board.beaconId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = nearbyBeacons.firstOrNull { it.id == board.beaconId }?.name
                                    ?: board.beaconId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (board.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            board.tags.forEach { tag ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (board.mediaUri != null) {
                item(key = "board_media") {
                    BoardMediaPreview(mediaUri = board.mediaUri)
                }
            }

            item(key = "board_content") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = board.content.ifBlank { stringResource(R.string.board_content_empty) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (board.content.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item(key = "board_view_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(onClick = { isEditing = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardMediaPreview(
    mediaUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { createSpatialImageLoader(context) }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUri)
                    .memoryCacheKey(mediaUri)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = stringResource(R.string.board_media_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
            )
        }
    }
}
