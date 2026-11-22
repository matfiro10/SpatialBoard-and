package com.example.spatialboard.ui.overlays

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R

sealed interface ActiveOverlay {
    val parentTabResId: Int? get() = null

    data object None : ActiveOverlay
    data object Permissions : ActiveOverlay

    data class NavigationCard(@StringRes val titleResId: Int) : ActiveOverlay {
        override val parentTabResId: Int = titleResId
    }

    data class BoardDetails(val boardId: String) : ActiveOverlay {
        override val parentTabResId: Int = R.string.nav_boards
    }

    data class BeaconDetails(val beaconId: String) : ActiveOverlay {
        override val parentTabResId: Int = R.string.nav_beacons
    }

    data class CreateBoard(val preselectedBeaconId: String? = null) : ActiveOverlay {
        override val parentTabResId: Int
            get() = if (preselectedBeaconId != null) R.string.nav_beacons else R.string.nav_add
    }

    companion object {
        val StackSaver = listSaver<List<ActiveOverlay>, Any>(
            save = { stack ->
                stack.flatMap { overlay ->
                    when (overlay) {
                        None -> listOf("none")
                        Permissions -> listOf("permissions")
                        is NavigationCard -> listOf("nav", overlay.titleResId)
                        is BoardDetails -> listOf("board_details", overlay.boardId)
                        is BeaconDetails -> listOf("beacon_details", overlay.beaconId)
                        is CreateBoard -> listOf("create_board", overlay.preselectedBeaconId.orEmpty())
                    }
                }
            },
            restore = { saved ->
                val restored = mutableListOf<ActiveOverlay>()
                var index = 0
                while (index < saved.size) {
                    when (saved[index]) {
                        "none" -> {
                            restored.add(None)
                            index += 1
                        }
                        "permissions" -> {
                            restored.add(Permissions)
                            index += 1
                        }
                        "nav" -> {
                            val resId = saved.getOrNull(index + 1) as? Int
                            if (resId != null) {
                                restored.add(NavigationCard(resId))
                            }
                            index += 2
                        }
                        "board_details" -> {
                            val boardId = saved.getOrNull(index + 1) as? String
                            if (boardId != null) {
                                restored.add(BoardDetails(boardId))
                            }
                            index += 2
                        }
                        "beacon_details" -> {
                            val beaconId = saved.getOrNull(index + 1) as? String
                            if (beaconId != null) {
                                restored.add(BeaconDetails(beaconId))
                            }
                            index += 2
                        }
                        "create_board" -> {
                            val beaconId = (saved.getOrNull(index + 1) as? String)
                                ?.takeIf { it.isNotEmpty() }
                            restored.add(CreateBoard(beaconId))
                            index += 2
                        }
                        else -> index += 1
                    }
                }
                restored
            }
        )
    }
}

@Composable
fun rememberOverlayContentMaxHeight(): Dp {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return if (isLandscape) {
        (screenHeight - 172.dp).coerceIn(110.dp, 260.dp)
    } else {
        (screenHeight * 0.48f).coerceIn(200.dp, 420.dp)
    }
}

@Composable
fun OverlayWindow(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 440.dp,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val effectiveMaxWidth = if (isLandscape) maxOf(maxWidth, 520.dp) else maxWidth
    val horizontalPadding = if (isLandscape) 20.dp else 24.dp
    val verticalPadding = if (isLandscape) 14.dp else 24.dp
    val sectionSpacing = if (isLandscape) 10.dp else 16.dp

    val shape = RoundedCornerShape(28.dp)
    val defaultPaneTitle = stringResource(R.string.overlay_pane_title)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = effectiveMaxWidth)
            .clip(shape)
            .focusGroup()
            .semantics {
                paneTitle = title ?: defaultPaneTitle
                isTraversalGroup = true
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.overlay_back),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.overlay_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}
