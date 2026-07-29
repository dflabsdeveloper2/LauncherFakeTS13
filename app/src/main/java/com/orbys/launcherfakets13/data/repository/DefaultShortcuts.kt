package com.orbys.launcherfakets13.data.repository

import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.domain.model.Shortcut

/**
 * Estructura para definir los accesos directos por defecto "a mano".
 */
object DefaultShortcuts {
    
    val defaults: Map<Environment, Map<String, List<Shortcut>>> = mapOf(
        Environment.GOOGLE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora"),
                Shortcut("com.google.android.calendar", "Calendario"),
                Shortcut("com.google.android.deskclock", "Tiempo"),
            ),
            "Aula" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador"),
                Shortcut("com.orbys.selector", "Selector de alumnos"),
                Shortcut("com.orbys.noise", "Medidor de ruido"),
                Shortcut("com.orbys.translate", "Orbys Translate")
            ),
            "Trabajo" to listOf(
                Shortcut("com.google.android.gm", "Gmail"),
                Shortcut("com.google.android.apps.docs", "Google Drive"),
                Shortcut("com.google.android.apps.meetings", "Meet"),
                Shortcut("com.google.android.apps.docs.editors.docs", "Docs")
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI"),
                Shortcut("com.orbys.aiselector", "Selector"),
                Shortcut("com.orbys.eshare", "Eshare")
            )
        ),
        Environment.OFFICE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora"),
                Shortcut("com.google.android.calendar", "Calendario"),
                Shortcut("com.google.android.deskclock", "Tiempo"),
            ),
            "Aula" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador"),
                Shortcut("com.orbys.aitranslate", "Orbys Translate")
            ),
            "Trabajo" to listOf(
                Shortcut("com.microsoft.office.outlook", "Outlook"),
                Shortcut("com.microsoft.skydrive", "OneDrive"),
                Shortcut("com.microsoft.teams", "Teams"),
                Shortcut("com.microsoft.office.word", "Word")
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI"),
                Shortcut("com.orbys.aiselector", "Selector"),
                Shortcut("com.orbys.eshare", "Eshare")
            )
        ),
        Environment.CORPORATE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora"),
                Shortcut("com.google.android.calendar", "Calendario"),
                Shortcut("com.google.android.deskclock", "Tiempo"),
            ),
            "Sala" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador"),
                Shortcut("com.orbys.videocall", "Teams"),
                Shortcut("com.orbys.eshare", "Eshare"),
            ),
            "Trabajo" to listOf(
                Shortcut("com.microsoft.office.outlook", "Correo"),
                Shortcut("com.microsoft.skydrive", "Almacenamiento"),
                Shortcut("com.microsoft.office.word", "Documentos"),
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI"),
                Shortcut("com.orbys.aiselector", "Selector"),
                Shortcut("com.skg.writer", "Pizarra")
            )
        ),
        Environment.SHOWROOM to mapOf(
            "General" to listOf(
                Shortcut("com.example.sampleds", "Digital Signage")
            )
        )
    )
}
