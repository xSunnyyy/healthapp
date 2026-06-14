package com.sunny.healthapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val loading: Boolean = true,
    val daily: DailySummary? = null,
    val sleep: SleepSummary? = null,
    val readiness: ReadinessSummary? = null,
)

class HomeViewModel(private val repo: HealthRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val today = LocalDate.now()
            val daily = runCatching { repo.dailySummary(today) }.getOrNull()
            val sleep = runCatching { repo.lastNightSleep() }.getOrNull()
            val readiness = runCatching { repo.readiness() }.getOrNull()
            _state.value = HomeState(
                loading = false,
                daily = daily,
                sleep = sleep,
                readiness = readiness,
            )
        }
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app.repository) as T
        }
    }
}
