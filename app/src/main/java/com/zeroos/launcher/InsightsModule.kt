package com.zeroos.launcher

import android.content.Context
import android.os.Build
import com.zeroos.core.data.insights.InsightsRepository
import com.zeroos.core.data.wellbeing.UsageEventSource
import com.zeroos.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlinx.coroutines.Dispatchers

/** Screen-time insights (Step 4.6, F10). */
@Module
@InstallIn(SingletonComponent::class)
object InsightsModule {
    /** Unlocks need Android 9's KEYGUARD_HIDDEN events (F10.12). */
    @Provides
    fun insightsRepository(
        @ApplicationContext context: Context,
        source: UsageEventSource,
        clock: Clock,
        logger: Logger
    ): InsightsRepository =
        InsightsRepository(source, clock, Dispatchers.IO, logger, context.packageName, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
}
