package com.example.domain

import com.example.model.Challenge
import com.example.model.ChallengeCategory
import kotlin.random.Random

class ChallengeManager {

    val challengesList = listOf(
        // SALUD (8 retos)
        Challenge(
            id = "salud_1",
            text = "Camina 10 minutos a paso ligero al aire libre.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_2",
            text = "Bebe un vaso de agua antes de cada comida importante de hoy.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_3",
            text = "Realiza 10 flexiones de pecho o sentadillas para activar tu cuerpo.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_4",
            text = "Realiza 5 minutos de estiramientos suaves del cuello y espalda.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_5",
            text = "Evita los azúcares añadidos o ultraprocesados en tus comidas de hoy.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_6",
            text = "Sustituye tu snack de media tarde por una fruta fresca de temporada.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_7",
            text = "Ve a dormir 15 minutos antes de lo habitual para optimizar tu descanso.",
            category = ChallengeCategory.SALUD
        ),
        Challenge(
            id = "salud_8",
            text = "Haz 3 respiraciones diafragmáticas profundas cada vez que mires el reloj.",
            category = ChallengeCategory.SALUD
        ),

        // PRODUCTIVIDAD (8 retos)
        Challenge(
            id = "prod_1",
            text = "Ordena y desinfecta tu escritorio o tu espacio principal de trabajo.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_2",
            text = "Define tus 3 objetivos más importantes del día y complétalos primero.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_3",
            text = "Usa la técnica Pomodoro (25 minutos de enfoque total, 5 de descanso) dos veces hoy.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_4",
            text = "Organiza y depura la bandeja de entrada de tu correo electrónico principal.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_5",
            text = "Planifica tu ropa, bolso y agenda para el día siguiente antes de cenar.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_6",
            text = "Pon el móvil en silencio en otra habitación por 45 minutos continuos de trabajo enfocado.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_7",
            text = "Dedica 15 minutos cronometrados a resolver esa pequeña tarea pendiente que siempre pospones.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),
        Challenge(
            id = "prod_8",
            text = "Anota en un papel físico todas las ideas sueltas en tu cabeza para despejar espacio mental.",
            category = ChallengeCategory.PRODUCTIVIDAD
        ),

        // APRENDIZAJE (8 retos)
        Challenge(
            id = "apren_1",
            text = "Lee 5 páginas de un libro de desarrollo personal, filosofía o no ficción.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_2",
            text = "Aprende una palabra nueva en otro idioma y constrúyete una frase usándola hoy.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_3",
            text = "Lee un artículo informativo detallado o de divulgación sobre un tema totalmente nuevo para ti.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_4",
            text = "Busca e investiga un dato histórico interesante sobre la ciudad o el país en el que vives.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_5",
            text = "Escucha atentamente 10 minutos de un podcast educativo, científico o inspirador.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_6",
            text = "Investiga cómo funciona técnicamente un invento simple del día a día (ej. la cremallera o bombilla).",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_7",
            text = "Aprende un atajo de teclado nuevo del programa que más utilizas y repítelo 5 veces hoy.",
            category = ChallengeCategory.APRENDIZAJE
        ),
        Challenge(
            id = "apren_8",
            text = "Escribe un breve resumen explicativo sobre el aprendizaje más valioso que has tenido esta última semana.",
            category = ChallengeCategory.APRENDIZAJE
        ),

        // MENTALIDAD (8 retos)
        Challenge(
            id = "ment_1",
            text = "No uses redes sociales de ningún tipo por 30 minutos consecutivos hoy.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_2",
            text = "Anota en tu cuaderno 3 cosas específicas por las que estés profundamente agradecido hoy.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_3",
            text = "Envía un mensaje corto pero sincero de aprecio o saludo cariñoso a un amigo con el que no hables habitualmente.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_4",
            text = "Dedica 5 minutos enteros a estar sentado en absoluto silencio, contemplando calmadamente tu alrededor.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_5",
            text = "Evita emitir cualquier queja o crítica en voz alta durante todo el día de hoy.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_6",
            text = "Hazle un cumplido genuino y sincero a un compañero de trabajo, de clase o familiar.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_7",
            text = "Pasa 5 minutos escribiendo tus reflexiones u opiniones libres sobre una meta vital importante para ti.",
            category = ChallengeCategory.MENTALIDAD
        ),
        Challenge(
            id = "ment_8",
            text = "Identifica un pensamiento negativo de autocrítica hoy, y transfórmalo inmediatamente en uno de apoyo.",
            category = ChallengeCategory.MENTALIDAD
        )
    )

    fun getChallengeById(id: String): Challenge? {
        return challengesList.find { it.id == id }
    }

    /**
     * Devuelve un reto aleatorio asegurando que NO sea idéntico al de ayer (excludeId).
     */
    fun getRandomChallenge(excludeId: String?): Challenge {
        val pool = if (excludeId != null && challengesList.size > 1) {
            challengesList.filter { it.id != excludeId }
        } else {
            challengesList
        }
        return pool[Random.nextInt(pool.size)]
    }
}
