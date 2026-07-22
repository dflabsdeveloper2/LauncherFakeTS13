package com.orbys.launcherts13.domain.repository

import com.orbys.launcherts13.domain.model.AppInfo

/**
 * Interfaz para el listado de aplicaciones instaladas en el sistema.
 */
interface AppsRepository {
    fun getInstalledApps(): List<AppInfo>
}
