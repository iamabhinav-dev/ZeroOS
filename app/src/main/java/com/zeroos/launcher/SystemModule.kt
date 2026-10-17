package com.zeroos.launcher

import android.content.Context
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.Logger
import com.zeroos.launcher.accessibility.AccessibilityBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Special permissions and the Accessibility service's actions (Steps 4.1, 4.2). */
@Module
@InstallIn(SingletonComponent::class)
object SystemModule {
    @Provides
    @Singleton
    fun specialPermissions(@ApplicationContext context: Context, logger: Logger): SpecialPermissions = SpecialPermissions(context, logger)

    @Provides
    fun systemActions(bridge: AccessibilityBridge): SystemActions = bridge
}
