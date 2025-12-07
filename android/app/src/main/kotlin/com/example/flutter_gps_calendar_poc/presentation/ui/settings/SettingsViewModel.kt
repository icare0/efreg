package com.example.flutter_gps_calendar_poc.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flutter_gps_calendar_poc.data.gamification.GamificationManager
import com.example.flutter_gps_calendar_poc.domain.model.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for settings and statistics screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    gamificationManager: GamificationManager
) : ViewModel() {

    /**
     * User stats for display.
     */
    val stats: StateFlow<UserStats?> = gamificationManager.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
