package com.zeroos.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import com.zeroos.core.data.apps.AndroidLauncherAppsSource
import com.zeroos.core.data.apps.AppActions
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.settings.AppCustomizationRepository
import com.zeroos.core.data.settings.FavoritesRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.createSettingsDataStore
import com.zeroos.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Provides repositories from core/data (Step 1.2). */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context, logger: Logger, io: CoroutineDispatcher): SettingsRepository =
        SettingsRepository(createSettingsDataStore(context, logger, CoroutineScope(SupervisorJob() + io)), logger)

    @Provides
    @Singleton
    fun favoritesRepository(settings: SettingsRepository): FavoritesRepository = FavoritesRepository(settings)

    @Provides
    @Singleton
    fun appCustomizationRepository(settings: SettingsRepository): AppCustomizationRepository = AppCustomizationRepository(settings)

    @Provides
    fun appActions(
        settings: SettingsRepository,
        favorites: FavoritesRepository,
        customization: AppCustomizationRepository,
        apps: AppsRepository
    ): AppActions = AppActions(settings, favorites, customization, apps)

    /** Dispatcher for disk and system-service work. */
    @Provides
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun appsRepository(@ApplicationContext context: Context, logger: Logger, io: CoroutineDispatcher): AppsRepository = AppsRepository(
        source = AndroidLauncherAppsSource(
            context.getSystemService(LauncherApps::class.java),
            context.getSystemService(UserManager::class.java),
            logger
        ),
        logger = logger,
        ioDispatcher = io
    )
}
