package com.zeroos.feature.home

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    @Provides
    fun timeTicks(@ApplicationContext context: Context, clock: Clock): TimeTicks = TimeTicks(context, clock)
}
