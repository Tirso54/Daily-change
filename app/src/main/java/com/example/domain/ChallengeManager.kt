package com.example.domain

import com.example.BuildConfig
import com.example.model.Challenge
import com.example.model.ChallengeCategory
import com.example.network.Content
import com.example.network.GeminiApiClient
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class ChallengeManager {

    suspend fun getRandomChallenge(excludeId: String?, preferredCategory: ChallengeCategory, language: String): Challenge = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackChallenge(preferredCategory)
        }

        val categoryStr = if (preferredCategory == ChallengeCategory.ANY) {
            "cualquier temática interesante que te parezca adecuada"
        } else {
            preferredCategory.promptName
        }
        
        val langInstruction = if (language == "en") "Respond in English." else "Responde en Español."

        val prompt = """
            Eres el generador de retos de la app 'Daily Challenge'.
            Genera UN (1) solo reto diario simple y accionable para que el usuario lo complete hoy en la vida real (mundo físico).
            El reto debe ser sobre: $categoryStr.
            
            Reglas:
            - Debe ser algo que se pueda hacer en menos de 30 minutos.
            - Fomenta la desconexión: evita por completo retos que requieran usar el teléfono móvil, ordenador o pantallas (por ejemplo, nada de "escribe a un amigo por mensaje" o "busca en internet"). En su lugar, sugiere actividades manuales, físicas, de introspección, interacción cara a cara o al aire libre.
            - $langInstruction
            - Devuelve ÚNICAMENTE el texto del reto sin comillas, introducciones, ni saludos.
            - El texto debe ser corto (máximo 2 oraciones).
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
                val resolvedCategory = if (preferredCategory == ChallengeCategory.ANY) {
                    ChallengeCategory.entries.filter { it != ChallengeCategory.ANY }.random()
                } else {
                    preferredCategory
                }
                return@withContext Challenge(
                    id = UUID.randomUUID().toString(),
                    text = text.replace("\"", ""),
                    category = resolvedCategory
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext getFallbackChallenge(preferredCategory)
    }
    
    private fun getFallbackChallenge(pref: ChallengeCategory): Challenge {
        val randomCat = if (pref == ChallengeCategory.ANY) ChallengeCategory.entries.filter { it != ChallengeCategory.ANY }.random() else pref
        return Challenge(
            id = UUID.randomUUID().toString(),
            text = "Bebe un vaso extra de agua hoy.",
            category = randomCat
        )
    }
}
