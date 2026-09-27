package com.skillmcp.mentor.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class LaunchAction {
    data class UseCase(val id: String) : LaunchAction()

    data class Draft(val text: String) : LaunchAction()

    data class OpenTab(val route: String) : LaunchAction()

    data object VoiceChat : LaunchAction()
}

class LaunchIntentHolder {
    private val pending = MutableSharedFlow<LaunchAction>(extraBufferCapacity = 8)

    val actions: SharedFlow<LaunchAction> = pending.asSharedFlow()

    fun push(action: LaunchAction) {
        pending.tryEmit(action)
    }
}
