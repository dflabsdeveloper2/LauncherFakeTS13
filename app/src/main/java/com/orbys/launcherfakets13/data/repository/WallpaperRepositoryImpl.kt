package com.orbys.launcherfakets13.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.orbys.launcherfakets13.domain.model.WallpaperType
import com.orbys.launcherfakets13.domain.repository.WallpaperRepository
import javax.inject.Inject

class WallpaperRepositoryImpl @Inject constructor(
    private val context: Context
) : WallpaperRepository {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "wallpaper_prefs", Context.MODE_PRIVATE
    )

    override fun getWallpaperType(): WallpaperType {
        val typeName = sharedPrefs.getString(KEY_WALLPAPER_TYPE, WallpaperType.DEFAULT.name)
        return try {
            WallpaperType.valueOf(typeName!!)
        } catch (e: Exception) {
            WallpaperType.DEFAULT
        }
    }

    override fun setWallpaperType(type: WallpaperType) {
        sharedPrefs.edit().putString(KEY_WALLPAPER_TYPE, type.name).apply()
    }

    override fun getLastWallpaperId(): Int {
        return sharedPrefs.getInt(KEY_LAST_WALLPAPER_ID, -1)
    }

    override fun setLastWallpaperId(id: Int) {
        sharedPrefs.edit().putInt(KEY_LAST_WALLPAPER_ID, id).apply()
    }

    companion object {
        private const val KEY_WALLPAPER_TYPE = "key_wallpaper_type"
        private const val KEY_LAST_WALLPAPER_ID = "key_last_wallpaper_id"
    }
}
