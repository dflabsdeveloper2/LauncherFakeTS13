package com.orbys.launcherfakets13.di

import android.content.Context
import android.app.WallpaperManager
import com.orbys.launcherfakets13.data.repository.AppShortcutsRepositoryImpl
import com.orbys.launcherfakets13.data.repository.AppsRepositoryImpl
import com.orbys.launcherfakets13.data.repository.DesktopRepositoryImpl
import com.orbys.launcherfakets13.data.repository.ShortcutsRepositoryImpl
import com.orbys.launcherfakets13.data.repository.WallpaperRepositoryImpl
import com.orbys.launcherfakets13.data.repository.WidgetRepositoryImpl
import com.orbys.launcherfakets13.domain.repository.AppShortcutsRepository
import com.orbys.launcherfakets13.domain.repository.AppsRepository
import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import com.orbys.launcherfakets13.domain.repository.ShortcutsRepository
import com.orbys.launcherfakets13.domain.repository.WallpaperRepository
import com.orbys.launcherfakets13.domain.repository.WidgetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideShortcutsRepository(@ApplicationContext context: Context): ShortcutsRepository {
        return ShortcutsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideWidgetRepository(@ApplicationContext context: Context): WidgetRepository {
        return WidgetRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperRepository(@ApplicationContext context: Context): WallpaperRepository {
        return WallpaperRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperManager(@ApplicationContext context: Context): WallpaperManager {
        return WallpaperManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAppsRepository(@ApplicationContext context: Context): AppsRepository {
        return AppsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAppShortcutsRepository(@ApplicationContext context: Context): AppShortcutsRepository {
        return AppShortcutsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideDesktopRepository(@ApplicationContext context: Context): DesktopRepository {
        return DesktopRepositoryImpl(context)
    }
}
