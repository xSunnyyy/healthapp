package com.sunny.healthapp.ui.screens.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.domain.model.ReadinessSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadinessState(val loading: Boolean = true, val readiness: ReadinessSummary? = null)

class ReadinessViewModel(private val repo: HealthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReadinessState())
    val state: StateFlow<ReadinessState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = ReadinessState(loading = true)
        val r = runCatching { repo.readiness() }.getOrNull()
        _state.value = ReadinessState(loading = false, readiness = r)
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReadinessViewModel(app.repository) as T
        }
    }
}
