package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChallengeRepository
import com.example.data.DbAppState
import com.example.data.DbCompletedChallenge
import com.example.domain.ChallengeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChallengeViewModel(
    private val repository: ChallengeRepository
) : ViewModel() {

    // Expose reactive database state
    val appState: StateFlow<DbAppState?> = repository.appStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val completedHistory: StateFlow<List<DbCompletedChallenge>> = repository.completedHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically sync challenge on startup
        syncDaily()
    }

    fun syncDaily() {
        viewModelScope.launch {
            try {
                repository.syncDailyChallenge()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateNewChallenge() {
        viewModelScope.launch {
            try {
                repository.generateNewChallenge()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeChallenge() {
        viewModelScope.launch {
            try {
                repository.completeChallenge()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            try {
                repository.resetAllProgress()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Factory pattern to instantiate ChallengeViewModel with custom repository parameters
    class Factory(private val repository: ChallengeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChallengeViewModel::class.java)) {
                return ChallengeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
