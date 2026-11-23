package com.example.spatialboard.ui.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.spatialboard.ui.components.rememberOverlayContentMaxHeight

@Composable
fun <T> OverlayList(
    items: List<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    OverlayListContainer(
        isEmpty = items.isEmpty(),
        emptyText = emptyText,
        modifier = modifier
    ) {
        items(
            items = items,
            key = key
        ) { item ->
            itemContent(item)
        }
    }
}

@Composable
fun <T : Any> OverlayList(
    items: LazyPagingItems<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val isInitialLoading = items.loadState.refresh is LoadState.Loading && items.itemCount == 0

    OverlayListContainer(
        isEmpty = items.itemCount == 0,
        emptyText = emptyText,
        isLoading = isInitialLoading,
        modifier = modifier
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey(key)
        ) { index ->
            val item = items[index]
            if (item != null) {
                itemContent(item)
            }
        }

        if (items.loadState.append is LoadState.Loading) {
            item(key = "paging_append_loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun OverlayListContainer(
    isEmpty: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    val maxListHeight = rememberOverlayContentMaxHeight()

    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        isEmpty -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
fun OverlayListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(width = 1.dp, color = borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            trailingContent?.invoke()
        }
    }
}
