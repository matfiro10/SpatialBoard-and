package com.example.spatialboard.ui.overlays

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class SelectedMediaInfo(
    val uri: Uri,
    val displayName: String,
    val isGif: Boolean
)

@Composable
fun CreateBoardOverlayContent(
    availableBeacons: List<BeaconItem>,
    preselectedBeaconId: String?,
    onCreateBoard: (BoardItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialBeacon = remember(preselectedBeaconId, availableBeacons) {
        availableBeacons.find { it.id == preselectedBeaconId }
    }

    var title by remember { mutableStateOf("") }
    var selectedBeaconId by remember(preselectedBeaconId) {
        mutableStateOf(preselectedBeaconId)
    }
    var roomLabel by remember(initialBeacon) {
        mutableStateOf(initialBeacon?.zoneLabel.orEmpty())
    }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<SelectedMediaInfo?>(null) }

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
            selectedMedia = resolveMediaInfo(context, uri)
        }
    }

    val maxContentHeight = rememberOverlayContentMaxHeight()
    val isFormValid = title.isNotBlank() && roomLabel.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.create_board_beacon_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "no_beacon") {
                FilterChip(
                    selected = selectedBeaconId == null,
                    onClick = { selectedBeaconId = null },
                    label = { Text(stringResource(R.string.create_board_no_beacon)) }
                )
            }

            items(
                items = availableBeacons,
                key = { it.id }
            ) { beacon ->
                val isSelected = selectedBeaconId == beacon.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedBeaconId = beacon.id
                        if (roomLabel.isBlank()) {
                            roomLabel = beacon.zoneLabel
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Rounded.Check
                            } else {
                                Icons.AutoMirrored.Rounded.BluetoothSearching
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "${beacon.name} (${String.format(Locale.US, "%.1fm", beacon.distanceMeters)})"
                        )
                    }
                )
            }
        }

        if (selectedBeaconId != null && availableBeacons.none { it.id == selectedBeaconId }) {
            Text(
                text = stringResource(R.string.create_board_assigned_beacon_id, selectedBeaconId.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.create_board_title_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = roomLabel,
            onValueChange = { roomLabel = it },
            label = { Text(stringResource(R.string.create_board_room_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text(stringResource(R.string.create_board_content_hint)) },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        val currentMedia = selectedMedia
        if (currentMedia == null) {
            OutlinedButton(
                onClick = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.create_board_add_media))
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (currentMedia.isGif) Icons.Rounded.Gif else Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentMedia.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                if (currentMedia.isGif) {
                                    R.string.create_board_media_type_gif
                                } else {
                                    R.string.create_board_media_type_image
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { selectedMedia = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.create_board_remove_media),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text(stringResource(R.string.create_board_tags_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val parsedTags = tagsInput
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                onCreateBoard(
                    BoardItem(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        roomLabel = roomLabel.trim(),
                        beaconId = selectedBeaconId,
                        updatedAt = timestamp,
                        content = content.trim(),
                        mediaUri = selectedMedia?.uri?.toString(),
                        tags = parsedTags
                    )
                )
            },
            enabled = isFormValid,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.create_board_save))
        }
    }
}

private fun resolveMediaInfo(context: Context, uri: Uri): SelectedMediaInfo {
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    var displayName: String? = null

    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
    }

    val resolvedName = displayName
        ?: uri.lastPathSegment
        ?: "media_attachment"

    val isGif = mimeType.equals("image/gif", ignoreCase = true) ||
            resolvedName.endsWith(".gif", ignoreCase = true)

    return SelectedMediaInfo(
        uri = uri,
        displayName = resolvedName,
        isGif = isGif
    )
}
