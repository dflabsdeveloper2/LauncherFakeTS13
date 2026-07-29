package com.orbys.launcherfakets13.data.repository

import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.domain.model.Shortcut

/**
 * Estructura para definir los accesos directos por defecto "a mano".
 */
object DefaultShortcuts {
    
    val defaults: Map<Environment, Map<String, List<Shortcut>>> = mapOf(
        Environment.GOOGLE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora", R.string.shortcut_time),
                Shortcut("com.google.android.calendar", "Calendario", R.string.shortcut_calendar),
                Shortcut("com.google.android.deskclock", "Tiempo", R.string.shortcut_weather),
            ),
            "Aula" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador", R.string.shortcut_timer),
                Shortcut("com.orbys.selector", "Selector de alumnos", R.string.shortcut_student_selector),
                Shortcut("com.orbys.noise", "Medidor de ruido", R.string.shortcut_noise_meter),
                Shortcut("com.orbys.translate", "Orbys Translate", R.string.shortcut_translate)
            ),
            "Trabajo" to listOf(
                Shortcut("com.google.android.gm", "Gmail", R.string.shortcut_gmail),
                Shortcut("com.google.android.apps.docs", "Google Drive", R.string.shortcut_drive),
                Shortcut("com.google.android.apps.meetings", "Meet", R.string.shortcut_meet),
                Shortcut("com.google.android.apps.docs.editors.docs", "Docs", R.string.shortcut_docs)
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI", R.string.shortcut_orbys_ai),
                Shortcut("com.orbys.aiselector", "Selector", R.string.shortcut_selector),
                Shortcut("com.orbys.eshare", "Eshare", R.string.shortcut_eshare)
            )
        ),
        Environment.OFFICE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora", R.string.shortcut_time),
                Shortcut("com.google.android.calendar", "Calendario", R.string.shortcut_calendar),
                Shortcut("com.google.android.deskclock", "Tiempo", R.string.shortcut_weather),
            ),
            "Aula" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador", R.string.shortcut_timer),
                Shortcut("com.orbys.aitranslate", "Orbys Translate", R.string.shortcut_translate)
            ),
            "Trabajo" to listOf(
                Shortcut("com.microsoft.office.outlook", "Outlook", R.string.shortcut_outlook),
                Shortcut("com.microsoft.skydrive", "OneDrive", R.string.shortcut_onedrive),
                Shortcut("com.microsoft.teams", "Teams", R.string.shortcut_teams),
                Shortcut("com.microsoft.office.word", "Word", R.string.shortcut_word)
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI", R.string.shortcut_orbys_ai),
                Shortcut("com.orbys.aiselector", "Selector", R.string.shortcut_selector),
                Shortcut("com.orbys.eshare", "Eshare", R.string.shortcut_eshare)
            )
        ),
        Environment.CORPORATE to mapOf(
            "General" to listOf(
                Shortcut("com.google.android.deskclock", "Hora", R.string.shortcut_time),
                Shortcut("com.google.android.calendar", "Calendario", R.string.shortcut_calendar),
                Shortcut("com.google.android.deskclock", "Tiempo", R.string.shortcut_weather),
            ),
            "Sala" to listOf(
                Shortcut("com.google.android.deskclock", "Temporizador", R.string.shortcut_timer),
                Shortcut("com.orbys.videocall", "Teams", R.string.shortcut_teams),
                Shortcut("com.orbys.eshare", "Eshare", R.string.shortcut_eshare),
            ),
            "Trabajo" to listOf(
                Shortcut("com.microsoft.office.outlook", "Correo", R.string.shortcut_mail),
                Shortcut("com.microsoft.skydrive", "Almacenamiento", R.string.shortcut_storage),
                Shortcut("com.microsoft.office.word", "Documentos", R.string.shortcut_documents),
            ),
            "Colaboración" to listOf(
                Shortcut("com.orbys.orbysai", "Orbys AI", R.string.shortcut_orbys_ai),
                Shortcut("com.orbys.aiselector", "Selector", R.string.shortcut_selector),
                Shortcut("com.skg.writer", "Pizarra", R.string.shortcut_whiteboard)
            )
        ),
        Environment.SHOWROOM to mapOf(
            "General" to listOf(
                Shortcut("com.example.sampleds", "Digital Signage", R.string.shortcut_showroom_ds)
            )
        )
    )
}
