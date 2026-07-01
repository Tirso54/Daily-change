package com.example.model

enum class ChallengeCategory(val displayName: String, val promptName: String) {
    ANY("Cualquiera ✨", "Cualquier categoría que te parezca interesante"),
    FITNESS("Fitness 🏃", "Ejercicios físicos, movimiento, estiramientos o deporte"),
    PRODUCTIVITY("Productividad ⚡", "Organización, trabajo enfocado, orden o gestión del tiempo"),
    CREATIVITY("Creatividad 🎨", "Arte, escritura, dibujo, música o pensamiento fuera de la caja"),
    MINDFULNESS("Mindfulness 🧘", "Meditación, gratitud, relajación o respiración consciente")
}
