package com.sunny.healthapp.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.domain.model.DailySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActivityState(
    val loading: Boolean = true,
    val today: DailySummary? = null,
    val recent: List<DailySummary> = emptyList(),
)

class ActivityViewModel(private val repo: HealthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = ActivityState(loading = true)
        val today = LocalDate.now()
        val days = (0..6).map { today.minusDays(it.toLong()) }
        val summaries = days.map { runCatching { repo.dailySummary(it) }.getOrNull() }
            .filterNotNull()
        _state.value = ActivityState(
            loading = false,
            today = summaries.firstOrNull(),
            recent = summaries,
        )
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ActivityViewModel(app.repository) as T
        }
    }
}
