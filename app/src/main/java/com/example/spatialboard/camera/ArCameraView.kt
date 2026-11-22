package com.example.spatialboard.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.ui.overlays.BoardItem
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberViewNodeManager

private class PlacedArBoard(
    val boardId: String,
    val anchorNode: AnchorNode,
    var viewNode: ViewNode,
    var renderedBoard: BoardItem
)

private class ArFrameHolder {
    var frame: Frame? = null
    var cameraPosition: Position = Position(0f, 0f, 0f)
    var draggingBoardId: String? = null
    var lastDragX: Float = 0f
    var lastDragY: Float = 0f
}

@Composable
fun ArCameraView(
    modifier: Modifier = Modifier,
    availableBoards: List<BoardItem> = emptyList(),
    pendingBoard: BoardItem? = null,
    isOverlayOpen: Boolean = false,
    onSelectPendingBoard: (BoardItem?) -> Unit = {},
    onBoardClick: (String) -> Unit = {}
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val viewNodeManager = rememberViewNodeManager()

    val placedBoards = remember { mutableStateListOf<PlacedArBoard>() }
    val frameHolder = remember { ArFrameHolder() }

    var selectedBoardIndex by remember { mutableIntStateOf(0) }
    var hasDetectedPlanes by remember { mutableStateOf(false) }
    var sessionError by remember { mutableStateOf<String?>(null) }

    fun findBoardIdFromNode(node: Node?): String? {
        return generateSequence(node) { it.parent }
            .firstNotNullOfOrNull { current ->
                current.name?.takeIf { it.startsWith(BOARD_NODE_PREFIX) }
                    ?.removePrefix(BOARD_NODE_PREFIX)
            }
    }

    fun createBoardViewNode(
        board: BoardItem,
        nodePosition: Position = Position(0f, 0.05f, 0f),
        nodeScale: Scale = Scale(0.22f)
    ): ViewNode {
        return ViewNode(
            engine = engine,
            windowManager = viewNodeManager,
            materialLoader = materialLoader,
            unlit = true,
            cameraPositionProvider = { frameHolder.cameraPosition }
        ) {
            SpatialBoardTheme {
                ArBoardCard(board = board)
            }
        }.apply {
            name = "$BOARD_NODE_PREFIX${board.id}"
            position = nodePosition
            scale = nodeScale
            isEditable = true
            isPositionEditable = false
            isScaleEditable = true
            editableScaleRange = 0.08f..1.20f
        }
    }

    LaunchedEffect(availableBoards, pendingBoard) {
        pendingBoard?.let { board ->
            val idx = availableBoards.indexOfFirst { it.id == board.id }
            if (idx >= 0) {
                selectedBoardIndex = idx
            }
        }

        val latestById = buildMap {
            availableBoards.forEach { put(it.id, it) }
            pendingBoard?.let { put(it.id, it) }
        }

        placedBoards.forEach { placed ->
            val updatedBoard = latestById[placed.boardId] ?: return@forEach
            if (updatedBoard != placed.renderedBoard) {
                val savedPosition = placed.viewNode.position
                val savedScale = placed.viewNode.scale

                placed.anchorNode.removeChildNode(placed.viewNode)
                placed.viewNode.destroy()

                val refreshedViewNode = createBoardViewNode(
                    board = updatedBoard,
                    nodePosition = savedPosition,
                    nodeScale = savedScale
                )
                placed.anchorNode.addChildNode(refreshedViewNode)
                placed.viewNode = refreshedViewNode
                placed.renderedBoard = updatedBoard
            }
        }
    }

    val nearestBeaconBoardId = availableBoards.firstOrNull { it.beaconId != null }?.id
    LaunchedEffect(nearestBeaconBoardId) {
        if (nearestBeaconBoardId != null && pendingBoard == null) {
            selectedBoardIndex = 0
        }
    }

    val safeIndex = if (availableBoards.isEmpty()) {
        0
    } else {
        selectedBoardIndex.coerceIn(0, availableBoards.lastIndex)
    }

    val activeBoardToPlace = pendingBoard ?: availableBoards.getOrNull(safeIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            planeRenderer = !isOverlayOpen && activeBoardToPlace != null,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
            },
            onSessionUpdated = { session, updatedFrame ->
                frameHolder.frame = updatedFrame
                val translation = updatedFrame.camera.pose.translation
                frameHolder.cameraPosition = Position(translation[0], translation[1], translation[2])

                val planesNowDetected = session.getAllTrackables(Plane::class.java)
                    .any { it.trackingState == TrackingState.TRACKING }
                if (hasDetectedPlanes != planesNowDetected) {
                    hasDetectedPlanes = planesNowDetected
                }
            },
            onSessionFailed = { exception ->
                sessionError = exception.localizedMessage ?: exception.javaClass.simpleName
            },
            onGestureListener = rememberOnGestureListener(
                onMoveBegin = { _, motionEvent, hitNode ->
                    if (!isOverlayOpen) {
                        val boardId = findBoardIdFromNode(hitNode)
                        frameHolder.draggingBoardId = boardId
                        if (boardId != null) {
                            frameHolder.lastDragX = motionEvent.x
                            frameHolder.lastDragY = motionEvent.y
                        }
                    }
                },
                onMove = { _, motionEvent, _ ->
                    if (isOverlayOpen) return@rememberOnGestureListener
                    val draggedId = frameHolder.draggingBoardId ?: return@rememberOnGestureListener
                    val placed = placedBoards.firstOrNull { it.boardId == draggedId }
                        ?: return@rememberOnGestureListener
                    val frame = frameHolder.frame ?: return@rememberOnGestureListener

                    val dx = motionEvent.x - frameHolder.lastDragX
                    val dy = motionEvent.y - frameHolder.lastDragY
                    frameHolder.lastDragX = motionEvent.x
                    frameHolder.lastDragY = motionEvent.y

                    val hitResult = frame.hitTest(motionEvent).firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane &&
                                trackable.trackingState == TrackingState.TRACKING &&
                                trackable.isPoseInPolygon(hit.hitPose)
                    }

                    if (hitResult != null) {
                        runCatching { hitResult.createAnchor() }.getOrNull()?.let { newAnchor ->
                            placed.anchorNode.anchor?.detach()
                            placed.anchorNode.anchor = newAnchor
                            placed.viewNode.position = Position(0f, 0.05f, 0f)
                        }
                    } else {
                        val cameraPose = frame.camera.displayOrientedPose
                        val right = cameraPose.xAxis
                        val up = cameraPose.yAxis
                        val factor = 0.0012f
                        val worldDelta = floatArrayOf(
                            (right[0] * dx - up[0] * dy) * factor,
                            (right[1] * dx - up[1] * dy) * factor,
                            (right[2] * dx - up[2] * dy) * factor
                        )
                        val localDelta = placed.anchorNode.anchor?.pose?.inverse()?.rotateVector(worldDelta)
                            ?: worldDelta
                        val currentPos = placed.viewNode.position
                        placed.viewNode.position = Position(
                            x = currentPos.x + localDelta[0],
                            y = currentPos.y + localDelta[1],
                            z = currentPos.z + localDelta[2]
                        )
                    }
                },
                onMoveEnd = { _, _, _ ->
                    frameHolder.draggingBoardId = null
                },
                onSingleTapConfirmed = { motionEvent, hitNode ->
                    if (isOverlayOpen) return@rememberOnGestureListener

                    val tappedBoardId = findBoardIdFromNode(hitNode)
                    if (tappedBoardId != null) {
                        onBoardClick(tappedBoardId)
                        return@rememberOnGestureListener
                    }

                    val boardToAnchor = activeBoardToPlace ?: return@rememberOnGestureListener
                    val frame = frameHolder.frame ?: return@rememberOnGestureListener

                    val hitResult = frame.hitTest(motionEvent).firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane &&
                                trackable.trackingState == TrackingState.TRACKING &&
                                trackable.isPoseInPolygon(hit.hitPose)
                    } ?: return@rememberOnGestureListener

                    val anchor = runCatching { hitResult.createAnchor() }.getOrNull()
                        ?: return@rememberOnGestureListener

                    val existingIndex = placedBoards.indexOfFirst { it.boardId == boardToAnchor.id }
                    if (existingIndex >= 0) {
                        val existing = placedBoards.removeAt(existingIndex)
                        existing.anchorNode.removeChildNode(existing.viewNode)
                        existing.viewNode.destroy()
                        existing.anchorNode.anchor?.detach()
                        existing.anchorNode.destroy()
                    }

                    val viewNode = createBoardViewNode(board = boardToAnchor)
                    val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
                        name = "$BOARD_NODE_PREFIX${boardToAnchor.id}"
                        isEditable = false
                        addChildNode(viewNode)
                    }

                    placedBoards.add(
                        PlacedArBoard(
                            boardId = boardToAnchor.id,
                            anchorNode = anchorNode,
                            viewNode = viewNode,
                            renderedBoard = boardToAnchor
                        )
                    )

                    if (pendingBoard?.id == boardToAnchor.id) {
                        onSelectPendingBoard(null)
                    }
                }
            )
        ) {
            placedBoards.forEach { placed ->
                key(placed.anchorNode) {
                    Node(
                        apply = {
                            if (placed.anchorNode.parent != this) {
                                addChildNode(placed.anchorNode)
                            }
                        }
                    )
                }
            }
        }

        val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

        if (!isOverlayOpen && sessionError == null && (availableBoards.isNotEmpty() || placedBoards.isNotEmpty())) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset + 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                ),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (availableBoards.isNotEmpty()) {
                                onSelectPendingBoard(null)
                                selectedBoardIndex =
                                    (safeIndex - 1 + availableBoards.size) % availableBoards.size
                            }
                        },
                        enabled = availableBoards.size > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.ar_prev_board)
                        )
                    }

                    Icon(
                        imageVector = if (activeBoardToPlace?.beaconId != null) {
                            Icons.AutoMirrored.Rounded.BluetoothSearching
                        } else {
                            Icons.Rounded.ViewInAr
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        val titleLabel = if (activeBoardToPlace != null) {
                            if (availableBoards.size > 1) {
                                "${activeBoardToPlace.title} (${safeIndex + 1}/${availableBoards.size})"
                            } else {
                                activeBoardToPlace.title
                            }
                        } else {
                            stringResource(R.string.ar_placed_count, placedBoards.size)
                        }

                        Text(
                            text = titleLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (activeBoardToPlace != null) {
                            Text(
                                text = stringResource(
                                    if (!hasDetectedPlanes) {
                                        R.string.ar_scan_surface_hint
                                    } else {
                                        R.string.ar_tap_to_place_hint
                                    },
                                    activeBoardToPlace.roomLabel
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (availableBoards.isNotEmpty()) {
                                onSelectPendingBoard(null)
                                selectedBoardIndex = (safeIndex + 1) % availableBoards.size
                            }
                        },
                        enabled = availableBoards.size > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.ar_next_board)
                        )
                    }

                    if (placedBoards.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                placedBoards.forEach { placed ->
                                    placed.anchorNode.removeChildNode(placed.viewNode)
                                    placed.viewNode.destroy()
                                    placed.anchorNode.anchor?.detach()
                                    placed.anchorNode.destroy()
                                }
                                placedBoards.clear()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.ar_clear_anchors),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        sessionError?.let { errorMessage ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.camera_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private const val BOARD_NODE_PREFIX = "board_"
