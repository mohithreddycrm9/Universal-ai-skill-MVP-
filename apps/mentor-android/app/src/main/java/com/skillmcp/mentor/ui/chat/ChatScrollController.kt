package com.skillmcp.mentor.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Single owner of chat auto-scroll:
 * - follows new content only while [followBottom] is true (user is at the bottom);
 * - a user drag away from the bottom stops following; settling at the bottom resumes it;
 * - pins the *bottom* of the last item (long streaming replies stay anchored to their newest line);
 * - throttles streaming updates to one scroll per [THROTTLE_MS].
 */
@Stable
class ChatScrollController(val listState: LazyListState) {
    var followBottom by mutableStateOf(true)
    private var userDragging by mutableStateOf(false)

    /** Show the jump-down button only when the user left the bottom and there is more below. */
    val showJumpToLatest: Boolean
        get() = !followBottom && listState.canScrollForward

    suspend fun trackUserScroll() {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> userDragging = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> userDragging = false
            }
        }
    }

    suspend fun trackFollowState() {
        snapshotFlow { Triple(userDragging, listState.isScrollInProgress, listState.canScrollForward) }
            .distinctUntilChanged()
            .collect { (dragging, scrolling, canScrollForward) ->
                when {
                    dragging && canScrollForward -> followBottom = false
                    !scrolling && !canScrollForward -> followBottom = true
                }
            }
    }

    /** Distance (px) from the bottom edge of the last item to the end of the viewport, if visible. */
    private fun remainingToBottomPx(): Float? {
        val info = listState.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return null
        if (last.index != info.totalItemsCount - 1) return null
        val end = info.viewportEndOffset - info.afterContentPadding
        return (last.offset + last.size - end).toFloat().coerceAtLeast(0f)
    }

    suspend fun scrollToBottom(animate: Boolean) {
        val lastIndex = listState.layoutInfo.totalItemsCount - 1
        if (lastIndex < 0) return
        val remaining = remainingToBottomPx()
        if (remaining == null) {
            if (animate) listState.animateScrollToItem(lastIndex) else listState.scrollToItem(lastIndex)
            remainingToBottomPx()?.let { if (it > 0f) listState.scrollBy(it) }
        } else if (remaining > 0f) {
            if (animate) {
                listState.animateScrollBy(remaining, tween(THROTTLE_MS.toInt(), easing = LinearEasing))
            } else {
                listState.scrollBy(remaining)
            }
        }
    }

    /** Stop following (used when restoring a saved mid-thread position). */
    fun pauseFollowing() {
        followBottom = false
    }

    companion object {
        const val THROTTLE_MS = 90L
    }
}

@Composable
fun rememberChatScrollController(listState: LazyListState): ChatScrollController =
    remember(listState) { ChatScrollController(listState) }

/**
 * Runs the one auto-scroll effect. [contentVersion] changes whenever the list grows or the stream
 * appends text; scrolling happens at most once per [ChatScrollController.THROTTLE_MS].
 */
@Composable
fun ChatAutoScrollEffect(
    controller: ChatScrollController,
    reduceMotion: Boolean,
    contentVersion: () -> Any,
) {
    LaunchedEffect(controller) { controller.trackUserScroll() }
    LaunchedEffect(controller) { controller.trackFollowState() }
    LaunchedEffect(controller, reduceMotion) {
        var first = true
        snapshotFlow { contentVersion() }
            .conflate()
            .collect {
                if (controller.followBottom) {
                    controller.scrollToBottom(animate = !reduceMotion && !first)
                    first = false
                    delay(ChatScrollController.THROTTLE_MS)
                }
            }
    }
}
