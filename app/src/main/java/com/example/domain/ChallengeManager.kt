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

        if (cachedChallenges.isEmpty()) {
            val newChallenges = fetchFromGemini(preferredCategory, language)
            if (newChallenges.isNotEmpty()) {
                cachedChallenges.addAll(newChallenges)
            } else {
                // Fallback de emergencia si falla la API
                val fallbacksEs = listOf(
                    "Bebe un vaso de agua extra.",
                    "Haz 5 respiraciones profundas con los ojos cerrados.",
                    "Haz 10 sentadillas o flexiones.",
                    "Sal a caminar 5 minutos sin rumbo fijo.",
                    "Ordena un pequeño cajón de tu habitación.",
                    "Siéntate en silencio durante 2 minutos.",
                    "Estira los brazos y el cuello por 3 minutos.",
                    "Escribe 3 cosas por las que estás agradecido hoy.",
                    "Llama a un amigo o familiar que hace tiempo no ves.",
                    "Lee 10 páginas de un libro."
                )
                val fallbacksEn = listOf(
                    "Drink an extra glass of water.",
                    "Take 5 deep breaths with your eyes closed.",
                    "Do 10 squats or pushups.",
                    "Take a 5-minute random walk outside.",
                    "Organize a small drawer in your room.",
                    "Sit in silence for 2 minutes.",
                    "Stretch your arms and neck for 3 minutes.",
                    "Write down 3 things you are grateful for today.",
                    "Call a friend or family member you haven't spoken to in a while.",
                    "Read 10 pages of a book."
                )
                cachedChallenges.addAll(if (language == "en") fallbacksEn.shuffled() else fallbacksEs.shuffled())
            }
        }

        val text = cachedChallenges.removeAt(0)
        
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
            if (newChallenges.isNotEmpty()) {
                val currentStr = prefs.getString(cacheKey, "") ?: ""
                val currentCache = if (currentStr.isEmpty()) mutableListOf() else currentStr.split("|||").filter { it.isNotBlank() }.toMutableList()
                
                // Add only unique new challenges that are not already in cache
                for (c in newChallenges) {
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
