package com.orbys.launcherfakets13.di

import com.orbys.launcherfakets13.domain.repository.AppShortcutsRepository
import com.orbys.launcherfakets13.domain.repository.AppsRepository
import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import com.orbys.launcherfakets13.domain.repository.ShortcutsRepository
import com.orbys.launcherfakets13.domain.repository.WidgetRepository
import com.orbys.launcherfakets13.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(repository: ShortcutsRepository) = GetCategoriesUseCase(repository)

    @Provides
    @Singleton
    fun provideSetCategoriesUseCase(repository: ShortcutsRepository) = SetCategoriesUseCase(repository)

    @Provides
    @Singleton
    fun provideAddCategoryUseCase(repository: ShortcutsRepository) = AddCategoryUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveCategoryUseCase(repository: ShortcutsRepository) = RemoveCategoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetWidgetsUseCase(repository: WidgetRepository) = GetWidgetsUseCase(repository)

    @Provides
    @Singleton
    fun provideAddWidgetUseCase(repository: WidgetRepository) = AddWidgetUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateWidgetUseCase(repository: WidgetRepository) = UpdateWidgetUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveWidgetUseCase(repository: WidgetRepository) = RemoveWidgetUseCase(repository)

    @Provides
    @Singleton
    fun provideRemapWidgetIdUseCase(repository: WidgetRepository) = RemapWidgetIdUseCase(repository)

    @Provides
    @Singleton
    fun provideSetShortcutUseCase(repository: ShortcutsRepository) = SetShortcutUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveShortcutUseCase(repository: ShortcutsRepository) = RemoveShortcutUseCase(repository)

    @Provides
    @Singleton
    fun provideGetShortcutUseCase(repository: ShortcutsRepository) = GetShortcutUseCase(repository)

    @Provides
    @Singleton
    fun provideGetInstalledAppsUseCase(repository: AppsRepository) = GetInstalledAppsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAppShortcutsUseCase(repository: AppShortcutsRepository) = GetAppShortcutsUseCase(repository)

    @Provides
    @Singleton
    fun provideLaunchAppShortcutUseCase(repository: AppShortcutsRepository) = LaunchAppShortcutUseCase(repository)

    @Provides
    @Singleton
    fun provideGetDesktopItemsUseCase(repository: DesktopRepository) = GetDesktopItemsUseCase(repository)

    @Provides
    @Singleton
    fun provideAddDesktopAppUseCase(repository: DesktopRepository) = AddDesktopAppUseCase(repository)

    @Provides
    @Singleton
    fun provideAddDesktopWidgetUseCase(repository: DesktopRepository) = AddDesktopWidgetUseCase(repository)

    @Provides
    @Singleton
    fun provideMoveDesktopItemUseCase(repository: DesktopRepository) = MoveDesktopItemUseCase(repository)

    @Provides
    @Singleton
    fun provideResizeDesktopItemUseCase(repository: DesktopRepository) = ResizeDesktopItemUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveDesktopItemUseCase(repository: DesktopRepository) = RemoveDesktopItemUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveDesktopWidgetByIdUseCase(repository: DesktopRepository) = RemoveDesktopWidgetByIdUseCase(repository)
}
