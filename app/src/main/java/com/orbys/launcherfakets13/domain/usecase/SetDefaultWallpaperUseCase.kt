package com.orbys.launcherfakets13.domain.usecase

import android.app.WallpaperManager
import com.orbys.launcherfakets13.domain.model.WallpaperType
import com.orbys.launcherfakets13.domain.repository.WallpaperRepository
import javax.inject.Inject

/**
 * UseCase para establecer el fondo de pantalla predeterminado del sistema.
 */
class SetDefaultWallpaperUseCase @Inject constructor(
    private val repository: WallpaperRepository,
    private val wallpaperManager: WallpaperManager
) {
    /**
     * Aplica un recurso como fondo de pantalla del sistema y actualiza el estado.
     *
     * @param resourceId El ID del recurso drawable a aplicar.
     */
    operator fun invoke(resourceId: Int) {
        runCatching {
            wallpaperManager.setResource(resourceId)
            repository.setWallpaperType(WallpaperType.DEFAULT)
            repository.setLastWallpaperId(wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM))
        }
    }
}
