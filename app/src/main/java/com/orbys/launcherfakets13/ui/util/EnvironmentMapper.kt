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
        Environment.SHOWROOM -> R.string.profile_showroom_title
    }

    /** Obtiene el ID del recurso de cadena para la categoría del entorno. */
    fun getCategoryRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.string.profile_office_category
        Environment.GOOGLE -> R.string.profile_google_category
        Environment.CORPORATE -> R.string.profile_corp_category
        Environment.SHOWROOM -> R.string.profile_showroom_category
    }

    /** Obtiene el ID del recurso de cadena para el pie de la tarjeta del entorno. */
    fun getFooterRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.string.profile_office_footer
        Environment.GOOGLE -> R.string.profile_google_footer
        Environment.CORPORATE -> R.string.profile_corp_footer
        Environment.SHOWROOM -> R.string.profile_showroom_footer
    }

    /** Obtiene el ID del recurso de drawable para el icono del entorno. */
    fun getIconRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.drawable.ic_microsoft_365
        Environment.GOOGLE -> R.drawable.google_color_svgrepo_com
        Environment.CORPORATE -> R.drawable.corporativo_ic_enviroment
        Environment.SHOWROOM -> R.drawable.showroom_ic_enviroment
    }

    /** Obtiene el ID del recurso de drawable para el fondo del entorno. */
    fun getBackgroundRes(environment: Environment): Int = when (environment) {
        Environment.OFFICE -> R.drawable.bg_home_gradient_office
        Environment.GOOGLE -> R.drawable.bg_home_gradient_google
        Environment.CORPORATE -> R.drawable.bg_home_gradient_corporate
        Environment.SHOWROOM -> R.drawable.bg_home_gradient_showroom
    }
}
