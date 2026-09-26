package com.skillmcp.mentor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class LaunchAction {
    data class UseCase(val id: String) : LaunchAction()

    data class Draft(val text: String) : LaunchAction()

    data class OpenTab(val route: String) : LaunchAction()
}

class LaunchIntentHolder {
    private val pending = MutableStateFlow<LaunchAction?>(null)

    val actions: StateFlow<LaunchAction?> = pending

    fun push(action: LaunchAction) {
        pending.value = action
    }

    fun consume(): LaunchAction? {
        val value = pending.value
        pending.value = null
        return value
    }
}
