package com.example.spatialboard.ui.overlays

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R

data class BoardPreviewItem(
    val id: String,
    val title: String,
    val roomLabel: String
)

data class BeaconPreviewItem(
    val id: String,
    val name: String,
    val zoneLabel: String,
    val distanceMeters: Float
)

@Composable
fun BoardsOverlayContent(
    onBoardClick: (BoardPreviewItem) -> Unit
) {
    val boards = remember {
        listOf(
            BoardPreviewItem("1", "Harmonogram zajęć", "Sala 204 • Laboratorium IoT"),
            BoardPreviewItem("2", "Instrukcja stanowiska", "Sala 112 • Robotyka"),
            BoardPreviewItem("3", "Notatki projektowe", "Strefa wspólna • Piętro 1")
        )
    }

    OverlayList(
        items = boards,
        emptyText = stringResource(R.string.boards_empty),
        key = { it.id }
    ) { board ->
        OverlayListItem(
            icon = Icons.Rounded.Dashboard,
            title = board.title,
            subtitle = board.roomLabel,
            onClick = { onBoardClick(board) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
fun BeaconsOverlayContent() {
    val beacons = remember {
        listOf(
            BeaconPreviewItem("b1", "Beacon-204-A", "Laboratorium IoT", 1.4f),
            BeaconPreviewItem("b2", "Beacon-112-B", "Pracownia sieciowa", 4.8f)
        )
    }
    var selectedBeaconId by rememberSaveable { mutableStateOf<String?>(null) }

    OverlayList(
        items = beacons,
        emptyText = stringResource(R.string.beacons_empty),
        key = { it.id }
    ) { beacon ->
        OverlayListItem(
            icon = Icons.Rounded.BluetoothSearching,
            title = beacon.name,
            subtitle = beacon.zoneLabel,
            selected = beacon.id == selectedBeaconId,
            onClick = {
                selectedBeaconId = if (selectedBeaconId == beacon.id) null else beacon.id
            },
            trailingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.beacons_distance_format, beacon.distanceMeters),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        )
    }
}
