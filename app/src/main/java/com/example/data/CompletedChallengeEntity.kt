package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_challenges")
data class DbCompletedChallenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val challengeId: String,
    val challengeText: String,
    val category: String,
    val completedDate: String, // format "yyyy-MM-dd"
    val timestamp: Long = System.currentTimeMillis()
)
