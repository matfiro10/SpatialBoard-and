package com.example.spatialboard.ui.overlays

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.example.spatialboard.R

@Composable
fun BoardsOverlayContent(
    boards: LazyPagingItems<BoardItem>,
    onBoardClick: (BoardItem) -> Unit
) {
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
fun BeaconsOverlayContent(
    beacons: List<BeaconItem>,
    onBeaconClick: (BeaconItem) -> Unit
) {
    OverlayList(
        items = beacons,
        emptyText = stringResource(R.string.beacons_empty),
        key = { it.id }
    ) { beacon ->
        OverlayListItem(
            icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
            title = beacon.name,
            subtitle = beacon.zoneLabel,
            onClick = { onBeaconClick(beacon) },
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
