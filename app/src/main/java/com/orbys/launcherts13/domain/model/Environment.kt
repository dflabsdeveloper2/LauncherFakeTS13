package com.orbys.launcherts13.domain.model

/**
 * Representa los diferentes entornos de trabajo soportados por el launcher.
 * Cada entorno define una configuración visual y funcional específica.
 *
 * Los recursos visuales asociados se resuelven en la capa de UI para mantener
 * este modelo como dominio puro.
 *
 * @property id Identificador único del entorno.
 * @property badgeLabel Etiqueta corta representativa (ej: "EDU", "PRO").
 * @property defaultCategories Lista de categorías de aplicaciones predefinidas para este entorno.
 */
enum class Environment(
    val id: String,
    val badgeLabel: String,
    val defaultCategories: List<String>
) {
    /** Entorno enfocado a educación usando herramientas de Microsoft Office. */
    OFFICE(
        id = "OFFICE",
        badgeLabel = "EDU",
        defaultCategories = listOf("General", "Aula", "Trabajo", "Colaboración")
    ),
    /** Entorno enfocado a educación usando herramientas de Google Workspace. */
    GOOGLE(
        id = "GOOGLE",
        badgeLabel = "EDU",
        defaultCategories = listOf("General", "Aula", "Trabajo", "Colaboración")
    ),
    /** Entorno corporativo para salas de reuniones y empresas. */
    CORPORATE(
        id = "CORPORATE",
        badgeLabel = "PRO",
        defaultCategories = listOf("General", "Sala", "Trabajo", "Colaboración")
    );

    companion object {
        /**
         * Busca un entorno por su identificador.
         */
        fun fromId(id: String?): Environment = entries.find { it.id == id } ?: GOOGLE
    }
}
