package com.skillmcp.mentor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skillmcp.mentor.skills.finder.OfficialSkillFinder
import com.skillmcp.mentor.skills.finder.SkillFinderSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SkillFinderUiState(
    val snapshot: SkillFinderSnapshot? = null,
    val refreshing: Boolean = false,
)

class SkillFinderViewModel(private val finder: OfficialSkillFinder) : ViewModel() {
    private val state = MutableStateFlow(SkillFinderUiState())
    val uiState: StateFlow<SkillFinderUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            state.value = state.value.copy(snapshot = finder.cached(), refreshing = true)
            state.value = SkillFinderUiState(snapshot = finder.refresh(), refreshing = false)
        }
    }

    fun refresh() {
        if (state.value.refreshing) return
        viewModelScope.launch {
            state.value = state.value.copy(refreshing = true)
            state.value = SkillFinderUiState(snapshot = finder.refresh(force = true), refreshing = false)
        }
    }

    class Factory(private val finder: OfficialSkillFinder) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SkillFinderViewModel(finder) as T
    }
}
