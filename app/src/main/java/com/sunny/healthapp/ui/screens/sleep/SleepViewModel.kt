package com.sunny.healthapp.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.domain.model.SleepSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SleepState(val loading: Boolean = true, val sleep: SleepSummary? = null)

class SleepViewModel(private val repo: HealthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = SleepState(loading = true)
        val sleep = runCatching { repo.lastNightSleep() }.getOrNull()
        _state.value = SleepState(loading = false, sleep = sleep)
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SleepViewModel(app.repository) as T
        }
    }
}
