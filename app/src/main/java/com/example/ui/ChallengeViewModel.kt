package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChallengeRepository
import com.example.data.DbAppState
import com.example.data.DbCompletedChallenge
import com.example.data.UserPreferences
import com.example.model.ChallengeCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChallengeViewModel(
    private val repository: ChallengeRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

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

    val themeSetting: StateFlow<String> = userPreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
        
    val languageSetting: StateFlow<String> = userPreferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "es")
        
    val categorySetting: StateFlow<String> = userPreferences.categoryFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "ANY")

    var isGenerating = kotlinx.coroutines.flow.MutableStateFlow(false)

    init {
        // We defer syncDaily until we have access to preferences
        viewModelScope.launch {
            syncDaily()
        }
    }

    private fun getPreferredCategory(): ChallengeCategory {
        val catName = categorySetting.value
        return try {
            ChallengeCategory.valueOf(catName)
        } catch (e: Exception) {
            ChallengeCategory.ANY
        }
    }

    private fun getLanguage(): String {
        val lang = languageSetting.value
        return if (lang == "SYSTEM" || lang.isEmpty()) "es" else lang
    }

    fun syncDaily() {
        viewModelScope.launch {
            isGenerating.value = true
            try {
                repository.syncDailyChallenge(getPreferredCategory(), getLanguage())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun generateNewChallenge() {
        viewModelScope.launch {
            isGenerating.value = true
            try {
                repository.generateNewChallenge(getPreferredCategory(), getLanguage())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating.value = false
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
            isGenerating.value = true
            try {
                repository.resetAllProgress(getPreferredCategory(), getLanguage())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch { userPreferences.setTheme(theme) }
    fun setLanguage(lang: String) = viewModelScope.launch { userPreferences.setLanguage(lang) }
    fun setCategory(cat: String) = viewModelScope.launch { userPreferences.setCategory(cat) }

    class Factory(
        private val repository: ChallengeRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChallengeViewModel::class.java)) {
                return ChallengeViewModel(repository, userPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
