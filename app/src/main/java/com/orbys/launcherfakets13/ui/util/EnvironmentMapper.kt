package com.orbys.launcherfakets13.ui.util

import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.Environment

/**
 * Mapeador para resolver recursos de Android asociados a modelos de dominio [Environment].
 * Esto permite que la capa de dominio permanezca agnóstica al framework.
 */
object EnvironmentMapper {

    /** Obtiene el ID del recurso de cadena para el título del entorno. */
    fun getTitleRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.string.profile_office_title
        Environment.GOOGLE -> R.string.profile_google_title
        Environment.CORPORATE -> R.string.profile_corp_title
    }

    /** Obtiene el ID del recurso de cadena para el subtítulo del entorno. */
    fun getSubtitleRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.string.profile_office_subtitle
        Environment.GOOGLE -> R.string.profile_google_subtitle
        Environment.CORPORATE -> R.string.profile_corp_subtitle
    }

    /** Obtiene el ID del recurso de drawable para el icono del entorno. */
    fun getIconRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE, Environment.GOOGLE -> R.drawable.ic_graduation
        Environment.CORPORATE -> R.drawable.ic_corporate
    }

    /** Obtiene el ID del recurso de drawable para el fondo del entorno. */
    fun getBackgroundRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.drawable.bg_home_gradient_office
        Environment.GOOGLE -> R.drawable.bg_home_gradient_google
        Environment.CORPORATE -> R.drawable.bg_home_gradient_corporate
    }
}
