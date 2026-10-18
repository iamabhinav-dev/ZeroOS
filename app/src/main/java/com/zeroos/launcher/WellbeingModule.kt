package com.zeroos.launcher

import android.app.usage.UsageStatsManager
import android.content.Context
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.focus.createFocusStateDataStore
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wellbeing.AndroidUsageEventSource
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.data.wellbeing.OutsideWatcher
import com.zeroos.core.data.wellbeing.TodayUsage
import com.zeroos.core.data.wellbeing.UsageEventSource
import com.zeroos.core.data.wellbeing.WellbeingStateRepository
import com.zeroos.core.data.wellbeing.WellbeingStatus
import com.zeroos.core.data.wellbeing.createWellbeingStateDataStore
import com.zeroos.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Launch gate, today's usage and running state for wellbeing (Step 4.3, F8). */
@Module
@InstallIn(SingletonComponent::class)
object WellbeingModule {
    @Provides
    @Singleton
    fun launchGate(apps: AppsRepository, settings: SettingsRepository, focus: FocusRepository, logger: Logger): LaunchGate =
        LaunchGate(apps, settings, focus, CoroutineScope(SupervisorJob() + Dispatchers.Default), logger)

    @Provides
    @Singleton
    fun focusRepository(@ApplicationContext context: Context, clock: Clock, logger: Logger): FocusRepository =
        FocusRepository(createFocusStateDataStore(context, logger, CoroutineScope(SupervisorJob() + Dispatchers.IO)), clock, logger)

    @Provides
    @Singleton
    fun wellbeingState(
        @ApplicationContext context: Context,
        clock: Clock,
        logger: Logger,
        io: CoroutineDispatcher
    ): WellbeingStateRepository =
        WellbeingStateRepository(createWellbeingStateDataStore(context, logger, CoroutineScope(SupervisorJob() + io)), clock, logger)

    @Provides
    fun wellbeingStatus(
        @ApplicationContext context: Context,
        state: WellbeingStateRepository,
        clock: Clock,
        logger: Logger,
        io: CoroutineDispatcher
    ) =
        WellbeingStatus(
            TodayUsage(AndroidUsageEventSource(context.getSystemService(UsageStatsManager::class.java)), clock, io, logger),
            state
        )

    /** Android's usage events (needs Usage access; F8.3, F8.16). */
    @Provides
    fun usageEventSource(@ApplicationContext context: Context): UsageEventSource =
        AndroidUsageEventSource(context.getSystemService(UsageStatsManager::class.java))

    @Provides
    @Singleton
    fun outsideWatcher(
        settings: SettingsRepository,
        source: UsageEventSource,
        state: WellbeingStateRepository,
        clock: Clock,
        logger: Logger
    ) =
        OutsideWatcher(settings, source, state, clock, Dispatchers.IO, logger)
}
