package com.orbys.launcherts13.domain.repository

import com.orbys.launcherts13.domain.model.WallpaperType

/**
 * Interfaz para gestionar la persistencia y el estado del fondo de pantalla.
 */
interface WallpaperRepository {
    /** Obtiene el tipo de fondo de pantalla actual (DEFAULT o CUSTOM). */
    fun getWallpaperType(): WallpaperType
    
    /** Establece el tipo de fondo de pantalla actual. */
    fun setWallpaperType(type: WallpaperType)
    
    /** Obtiene el último ID de fondo de pantalla del sistema registrado. */
    fun getLastWallpaperId(): Int
    
    /** Registra el ID de fondo de pantalla del sistema actual. */
    fun setLastWallpaperId(id: Int)
}
