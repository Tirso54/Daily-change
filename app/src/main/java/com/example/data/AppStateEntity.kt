package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class DbAppState(
    @PrimaryKey val id: Int = 1,
    val currentChallengeId: String,
    val currentChallengeText: String,
    val currentChallengeCategory: String,
    val generationDate: String, // format "yyyy-MM-dd"
    val currentStreak: Int,
    val lastCompletedDate: String?, // format "yyyy-MM-dd" or null
    val isCompletedToday: Boolean,
    val previousChallengeId: String? = null
)
