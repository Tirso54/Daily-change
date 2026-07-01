package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme")
    private val LANG_KEY = stringPreferencesKey("language")
    private val CATEGORY_KEY = stringPreferencesKey("category")

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "SYSTEM"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANG_KEY] ?: "SYSTEM"
    }

    val categoryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CATEGORY_KEY] ?: "ANY"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANG_KEY] = language
        }
    }

    suspend fun setCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[CATEGORY_KEY] = category
        }
    }
}
