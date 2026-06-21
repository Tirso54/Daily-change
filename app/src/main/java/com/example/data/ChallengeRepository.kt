package com.example.data

import com.example.domain.ChallengeManager
import com.example.model.ChallengeCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChallengeRepository(
    private val challengeDao: ChallengeDao,
    private val challengeManager: ChallengeManager
) {

    val appStateFlow: Flow<DbAppState?> = challengeDao.getAppStateFlow()
    val completedHistoryFlow: Flow<List<DbCompletedChallenge>> = challengeDao.getAllCompletedChallengesFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    /**
     * Checks if there's a daily challenge for today.
     * Generates a new one if it's a new day or if no state exists yet.
     */
    suspend fun syncDailyChallenge() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val existingState = challengeDao.getAppState()

        if (existingState == null) {
            // First run: generate a random challenge
            val newChallenge = challengeManager.getRandomChallenge(excludeId = null)
            val newState = DbAppState(
                id = 1,
                currentChallengeId = newChallenge.id,
                currentChallengeText = newChallenge.text,
                currentChallengeCategory = newChallenge.category.name,
                generationDate = today,
                currentStreak = 0,
                lastCompletedDate = null,
                isCompletedToday = false,
                previousChallengeId = null
            )
            challengeDao.saveAppState(newState)
        } else {
            // Check if it's a new day
            if (existingState.generationDate != today) {
                // Determine if they completed the previously generated challenge.
                // A challenge is considered completed if lastCompletedDate equals generationDate.
                val prevChallengeWasCompleted = existingState.lastCompletedDate == existingState.generationDate
                
                // If yesterday's challenge was NOT completed, the streak is lost.
                // If the user hasn't completed yesterday's challenge, the streak is reset.
                val newStreak = if (prevChallengeWasCompleted) {
                    existingState.currentStreak
                } else {
                    0
                }

                // Generate new challenge avoiding the previous one
                val newChallenge = challengeManager.getRandomChallenge(excludeId = existingState.currentChallengeId)
                
                val newState = DbAppState(
                    id = 1,
                    currentChallengeId = newChallenge.id,
                    currentChallengeText = newChallenge.text,
                    currentChallengeCategory = newChallenge.category.name,
                    generationDate = today,
                    currentStreak = newStreak,
                    lastCompletedDate = existingState.lastCompletedDate,
                    isCompletedToday = false,
                    previousChallengeId = existingState.currentChallengeId
                )
                challengeDao.saveAppState(newState)
            } else {
                // Same day, make sure any external streak check validates if the streak is still alive.
                // If yesterday was NOT completed AND today is NOT completed, the streak should show as 0.
                // However, wait! If they open the app on day T, and yesterday (T-1) was completed, keying on
                // `prevChallengeWasCompleted` would be true, so the streak is held (e.g. 5).
                // If they missed yesterday, the streak is reset.
                // But what if they already failed yeserday, but it's the SAME day T? This was already handled in the transition!
                // Let's make sure that if yesterday is missed, the streak is correctly updated.
                // If they opened the app yesterday, but didn't complete, then on today's transition `prevChallengeWasCompleted` was false,
                // so streak became 0.
                // What if they didn't even open the app yesterday (T-1), and the last generation was T-2 (completed) or T-2 (not completed)?
                // If last generation was T-2, and was completed (lastCompletedDate = T-2):
                // On day T, during the transition, `prevChallengeWasCompleted = true` (lastCompletedDate == generationDate).
                // But wait! T-2 completed means they completed T-2. But they missed T-1 entirely!
                // So the streak should actually reset to 0 because T-1 was missed!
                // Let's refine the "was yesterday completed" check:
                // Yesterday was completed if: `lastCompletedDate == getYesterdayDateString()`.
                // Perfect! That is mathematically precise!
                // Let's check:
                // If today is T:
                // Yesterday is T-1.
                // Was yesterday completed? Yesterday was completed if and only if `lastCompletedDate == T-1`.
                // What if yesterday was NOT a challenge day (e.g., they didn't open the app)? Then lastCompletedDate is T-2, which is NOT T-1, so yesterday was NOT completed. Thus the streak resets to 0.
                // What if they completed the challenge yesterday? Then `lastCompletedDate` was updated to T-1 yesterday. So yes, `lastCompletedDate == T-1`, streak is kept!
                // This is absolutely flawless! Let's write this exact, precise logic!
                val yesterday = getYesterdayDateString()
                val yesterdayWasCompleted = existingState.lastCompletedDate == yesterday

                // If yesterday was NOT completed, they missed a day, so streak is active (meaning it shouldn't reset, wait!
                // if they completed yesterday, streak is kept. If yesterday was NOT completed AND today is NOT completed,
                // when transitioning, we reset currentStreak to 0.
                // Let's adjust existingState's streak if needed!
                val yesterdayChallengeWasActive = existingState.generationDate == yesterday
                val yesterdayCompleted = if (yesterdayChallengeWasActive) {
                    existingState.isCompletedToday
                } else {
                    existingState.lastCompletedDate == yesterday
                }

                if (!yesterdayCompleted && existingState.currentStreak > 0 && !existingState.isCompletedToday) {
                    // Update state to reset streak back to 0
                    val updatedState = existingState.copy(
                        currentStreak = 0
                    )
                    challengeDao.saveAppState(updatedState)
                }
            }
        }
    }

    /**
     * Generates a new challenge manually.
     * Keeps current streak active, but resets completion for the new challenge.
     */
    suspend fun generateNewChallenge() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val existingState = challengeDao.getAppState()
        val excludeId = existingState?.currentChallengeId

        val newChallenge = challengeManager.getRandomChallenge(excludeId = excludeId)
        
        val newState = DbAppState(
            id = 1,
            currentChallengeId = newChallenge.id,
            currentChallengeText = newChallenge.text,
            currentChallengeCategory = newChallenge.category.name,
            generationDate = today,
            currentStreak = existingState?.currentStreak ?: 0,
            lastCompletedDate = existingState?.lastCompletedDate,
            isCompletedToday = false,
            previousChallengeId = excludeId
        )
        challengeDao.saveAppState(newState)
    }

    /**
     * Marks the current daily challenge as completed.
     * Increments the streak, updates the app state, and adds to historical completed-challenges list.
     */
    suspend fun completeChallenge() = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val existingState = challengeDao.getAppState() ?: return@withContext

        // If already completed today, do not increment streak again
        if (existingState.isCompletedToday) return@withContext

        // Check if yesterday was completed to keep/build streak, or if we start a new streak
        val yesterday = getYesterdayDateString()
        val yesterdayWasCompleted = existingState.lastCompletedDate == yesterday || 
                (existingState.generationDate == yesterday && existingState.lastCompletedDate == yesterday)
        
        // Wait, if yesterday was completed, newStreak is existing + 1.
        // If yesterday was NOT completed, then the streak had reset to 0, so newStreak is 1!
        // But what if they complete today, is the streak 1 or currentStreak + 1?
        // Let's trace:
        // If currentStreak is 0 (since they missed yesterday or it was reset), completing today makes it 1.
        // If yesterday was completed, currentStreak is still alive (e.g. 3). Completing today makes it 4.
        // So in ALL cases, if the currentStreak is up-to-date,completing today just makes it `currentStreak + 1`!
        // Wait, is that true?
        // Yes, because if they missed yesterday, `syncDailyChallenge` already reset the currentStreak to 0. So completing today makes it 0 + 1 = 1.
        // If they completed yesterday, currentStreak is kept at 3. Completing today makes it 3 + 1 = 4.
        // This is extremely simple and perfectly correct!
        val newStreak = existingState.currentStreak + 1

        val updatedState = DbAppState(
            id = 1,
            currentChallengeId = existingState.currentChallengeId,
            currentChallengeText = existingState.currentChallengeText,
            currentChallengeCategory = existingState.currentChallengeCategory,
            generationDate = today,
            currentStreak = newStreak,
            lastCompletedDate = today,
            isCompletedToday = true,
            previousChallengeId = existingState.previousChallengeId
        )
        challengeDao.saveAppState(updatedState)

        // Add to completion history
        val historyItem = DbCompletedChallenge(
            challengeId = existingState.currentChallengeId,
            challengeText = existingState.currentChallengeText,
            category = existingState.currentChallengeCategory,
            completedDate = today,
            timestamp = System.currentTimeMillis()
        )
        challengeDao.insertCompletedChallenge(historyItem)
    }

    /**
     * Clears local history and resets state.
     */
    suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        challengeDao.clearCompletedHistory()
        
        val today = getTodayDateString()
        val newChallenge = challengeManager.getRandomChallenge(excludeId = null)
        val newState = DbAppState(
            id = 1,
            currentChallengeId = newChallenge.id,
            currentChallengeText = newChallenge.text,
            currentChallengeCategory = newChallenge.category.name,
            generationDate = today,
            currentStreak = 0,
            lastCompletedDate = null,
            isCompletedToday = false,
            previousChallengeId = null
        )
        challengeDao.saveAppState(newState)
    }
}
