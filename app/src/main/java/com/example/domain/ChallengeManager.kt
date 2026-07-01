package com.example.domain

import android.content.Context
import com.example.BuildConfig
import com.example.model.Challenge
import com.example.model.ChallengeCategory
import com.example.network.Content
import com.example.network.GeminiApiClient
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChallengeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("daily_challenge_cache", Context.MODE_PRIVATE)
    private val prefetchScope = CoroutineScope(Dispatchers.IO)
    private var isPrefetching = false

    suspend fun getRandomChallenge(excludeId: String?, preferredCategory: ChallengeCategory, language: String): Challenge = withContext(Dispatchers.IO) {
        val cacheKey = "${preferredCategory.name}_$language"
        val cachedStr = prefs.getString(cacheKey, "") ?: ""
        val cachedChallenges = if (cachedStr.isEmpty()) mutableListOf() else cachedStr.split("|||").filter { it.isNotBlank() }.toMutableList()

        val usedKey = "used_challenges_$language"
        val usedStr = prefs.getString(usedKey, "") ?: ""
        var usedSet = if (usedStr.isEmpty()) mutableSetOf<String>() else usedStr.split("|||").filter { it.isNotBlank() }.toMutableSet()

        if (cachedChallenges.isEmpty()) {
            val newChallenges = fetchFromGemini(preferredCategory, language)
            val filteredNew = newChallenges.filter { !usedSet.contains(it) }
            
            if (filteredNew.isNotEmpty()) {
                cachedChallenges.addAll(filteredNew)
            } else {
                // Fallback de emergencia si falla la API o todos los nuevos ya se usaron
                val fallbacksEs = when (preferredCategory) {
                    ChallengeCategory.FITNESS -> listOf(
                        "Haz 10 sentadillas o flexiones.",
                        "Sal a caminar 5 minutos sin rumbo fijo.",
                        "Estira los brazos y el cuello por 3 minutos.",
                        "Haz una plancha (plank) de 30 segundos.",
                        "Sube y baja las escaleras durante 2 minutos."
                    )
                    ChallengeCategory.PRODUCTIVITY -> listOf(
                        "Ordena un pequeño cajón de tu habitación o escritorio.",
                        "Escribe en papel tus 3 prioridades principales de hoy.",
                        "Planifica la comida o cena de mañana.",
                        "Tira a la basura o recicla 3 objetos que ya no necesites.",
                        "Dedica 5 minutos a limpiar la pantalla y teclado de tu ordenador (si está apagado)."
                    )
                    ChallengeCategory.CREATIVITY -> listOf(
                        "Dibuja un boceto rápido de lo que tengas enfrente.",
                        "Escribe un pequeño poema de 4 líneas sobre tu día.",
                        "Canta o tararea una canción inventada por 1 minuto.",
                        "Toma una foto de algo ordinario desde un ángulo inusual.",
                        "Dobla una hoja de papel e intenta hacer un avión u origami diferente."
                    )
                    ChallengeCategory.MINDFULNESS -> listOf(
                        "Haz 5 respiraciones profundas con los ojos cerrados.",
                        "Siéntate en silencio absoluto durante 2 minutos.",
                        "Escribe 3 cosas por las que estás agradecido hoy.",
                        "Bebe un vaso de agua despacio, notando la temperatura.",
                        "Cierra los ojos y presta atención a los sonidos más lejanos que escuches."
                    )
                    else -> listOf(
                        "Bebe un vaso de agua extra.",
                        "Haz 10 sentadillas o flexiones.",
                        "Sal a caminar 5 minutos sin rumbo fijo.",
                        "Estira los brazos y el cuello por 3 minutos.",
                        "Haz una plancha (plank) de 30 segundos.",
                        "Ordena un pequeño cajón de tu habitación o escritorio.",
                        "Escribe en papel tus 3 prioridades principales de hoy.",
                        "Tira a la basura o recicla 3 objetos que ya no necesites.",
                        "Dibuja un boceto rápido de lo que tengas enfrente.",
                        "Canta o tararea una canción inventada por 1 minuto.",
                        "Haz 5 respiraciones profundas con los ojos cerrados.",
                        "Siéntate en silencio absoluto durante 2 minutos.",
                        "Escribe 3 cosas por las que estás agradecido hoy.",
                        "Llama a un amigo cercano."
                    )
                }

                val fallbacksEn = when (preferredCategory) {
                    ChallengeCategory.FITNESS -> listOf(
                        "Do 10 squats or pushups.",
                        "Take a 5-minute random walk outside.",
                        "Stretch your arms and neck for 3 minutes.",
                        "Hold a plank for 30 seconds.",
                        "Walk up and down the stairs for 2 minutes."
                    )
                    ChallengeCategory.PRODUCTIVITY -> listOf(
                        "Organize a small drawer or your desk.",
                        "Write down on paper your top 3 priorities for today.",
                        "Plan your meals for tomorrow.",
                        "Throw away or recycle 3 items you no longer need.",
                        "Spend 5 minutes cleaning your workspace."
                    )
                    ChallengeCategory.CREATIVITY -> listOf(
                        "Draw a quick sketch of whatever is in front of you.",
                        "Write a short 4-line poem about your day.",
                        "Hum or sing a made-up song for 1 minute.",
                        "Take a photo of something ordinary from an unusual angle.",
                        "Fold a piece of paper and make a paper airplane."
                    )
                    ChallengeCategory.MINDFULNESS -> listOf(
                        "Take 5 deep breaths with your eyes closed.",
                        "Sit in absolute silence for 2 minutes.",
                        "Write down 3 things you are grateful for today.",
                        "Drink a glass of water slowly, noticing its temperature.",
                        "Close your eyes and listen to the farthest sounds you can hear."
                    )
                    else -> listOf(
                        "Drink an extra glass of water.",
                        "Do 10 squats or pushups.",
                        "Take a 5-minute random walk outside.",
                        "Stretch your arms and neck for 3 minutes.",
                        "Hold a plank for 30 seconds.",
                        "Organize a small drawer or your desk.",
                        "Write down on paper your top 3 priorities for today.",
                        "Throw away or recycle 3 items you no longer need.",
                        "Draw a quick sketch of whatever is in front of you.",
                        "Hum or sing a made-up song for 1 minute.",
                        "Take 5 deep breaths with your eyes closed.",
                        "Sit in absolute silence for 2 minutes.",
                        "Write down 3 things you are grateful for today.",
                        "Call a close friend."
                    )
                }
                
                val fallbackList = if (language == "en") fallbacksEn.shuffled() else fallbacksEs.shuffled()
                var filteredFallbacks = fallbackList.filter { !usedSet.contains(it) }
                
                // Si nos quedamos sin fallbacks únicos, reiniciamos el historial
                if (filteredFallbacks.isEmpty()) {
                    usedSet.clear()
                    filteredFallbacks = fallbackList
                }
                
                cachedChallenges.addAll(filteredFallbacks)
            }
        }

        val text = cachedChallenges.removeAt(0)
        
        usedSet.add(text)
        prefs.edit().putString(usedKey, usedSet.joinToString("|||")).apply()
        
        prefs.edit().putString(cacheKey, cachedChallenges.joinToString("|||")).apply()

        if (cachedChallenges.size < 4 && !isPrefetching) {
            prefetchScope.launch {
                prefetchChallenges(preferredCategory, language, cacheKey)
            }
        }

        return@withContext createChallengeObj(text, preferredCategory)
    }

    private suspend fun prefetchChallenges(preferredCategory: ChallengeCategory, language: String, cacheKey: String) {
        isPrefetching = true
        try {
            val newChallenges = fetchFromGemini(preferredCategory, language)
            
            val usedKey = "used_challenges_$language"
            val usedStr = prefs.getString(usedKey, "") ?: ""
            val usedSet = if (usedStr.isEmpty()) mutableSetOf<String>() else usedStr.split("|||").filter { it.isNotBlank() }.toMutableSet()
            
            val filteredNew = newChallenges.filter { !usedSet.contains(it) }
            
            if (filteredNew.isNotEmpty()) {
                val currentStr = prefs.getString(cacheKey, "") ?: ""
                val currentCache = if (currentStr.isEmpty()) mutableListOf() else currentStr.split("|||").filter { it.isNotBlank() }.toMutableList()
                
                // Add only unique new challenges that are not already in cache
                for (c in filteredNew) {
                    if (!currentCache.contains(c)) {
                        currentCache.add(c)
                    }
                }
                
                prefs.edit().putString(cacheKey, currentCache.joinToString("|||")).apply()
            }
        } finally {
            isPrefetching = false
        }
    }

    private suspend fun fetchFromGemini(preferredCategory: ChallengeCategory, language: String): MutableList<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return mutableListOf()
        }

        val categoryStr = if (preferredCategory == ChallengeCategory.ANY) {
            "cualquier temática interesante que te parezca adecuada"
        } else {
            preferredCategory.promptName
        }
        
        val langInstruction = if (language == "en") "Respond in English." else "Responde en Español."

        val prompt = """
            Eres un generador muy creativo para la app 'Daily Challenge'.
            Genera 10 retos diarios simples, accionables y COMPLETAMENTE DIFERENTES ENTRE SÍ para que el usuario los complete hoy en el mundo físico.
            La temática debe ser sobre: $categoryStr.
            
            Reglas críticas:
            - Deben ser extremadamente originales, poco comunes y sorprendentes. Evita los clichés de siempre.
            - Nunca repitas retos anteriores, siempre genera ideas totalmente nuevas. No repitas patrones.
            - Deben ser cosas que se puedan hacer en menos de 30 minutos.
            - Fomenta la desconexión total: nada de teléfonos, ordenadores o pantallas.
            - $langInstruction
            - Devuelve ÚNICAMENTE los textos de los retos, uno por línea, separados por un salto de línea, sin numeración ni viñetas.
            - Cada texto debe ser corto (máximo 2 oraciones).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.9f)
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            
            if (!text.isNullOrEmpty()) {
                val lines = text.split("\n")
                    .map { it.trim().removePrefix("-").removePrefix("*").replace(Regex("^[0-9]+\\.\\s*"), "").trim() }
                    .filter { it.isNotEmpty() }
                
                if (lines.isNotEmpty()) {
                    return lines.toMutableList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return mutableListOf()
    }

    private fun createChallengeObj(text: String, preferredCategory: ChallengeCategory): Challenge {
        val resolvedCategory = if (preferredCategory == ChallengeCategory.ANY) {
            ChallengeCategory.entries.filter { it != ChallengeCategory.ANY }.random()
        } else {
            preferredCategory
        }
        return Challenge(
            id = UUID.randomUUID().toString(),
            text = text.replace("\"", ""),
            category = resolvedCategory
        )
    }
}
