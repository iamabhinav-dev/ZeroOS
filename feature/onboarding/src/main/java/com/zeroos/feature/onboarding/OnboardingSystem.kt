package com.zeroos.feature.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.MediaStore
import androidx.core.net.toUri
import com.zeroos.core.data.system.isDefaultHome
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** What onboarding asks the phone (F14.2, F14.3, F14.6). An interface so the flow is tested without Android. */
interface OnboardingSystem {
    fun isDefaultHome(): Boolean

    /** True while Android may stop ZeroOS in the background to save battery (F14.6). */
    fun isBatteryOptimized(): Boolean

    /** Packages of the default Phone, Messages, Camera and Maps apps, in that order; missing ones are left out (F14.3). */
    fun suggestedPackages(): List<String>
}

class AndroidOnboardingSystem @Inject constructor(@ApplicationContext private val context: Context) : OnboardingSystem {
    private val packageManager get() = context.packageManager

    override fun isDefaultHome() = isDefaultHome(packageManager, context.packageName)

    override fun isBatteryOptimized() =
        !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    override fun suggestedPackages(): List<String> = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_SENDTO, "smsto:".toUri()),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent(Intent.ACTION_VIEW, "geo:0,0".toUri())
    ).mapNotNull(::defaultPackageFor).distinct()

    /** The app that opens [intent] without asking; null when no default is chosen (Android's chooser answers). */
    private fun defaultPackageFor(intent: Intent): String? {
        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
        return info?.packageName?.takeUnless { it == ANDROID_PACKAGE }
    }

    private companion object {
        /** Android's own chooser ("Open with…") lives in this package. */
        const val ANDROID_PACKAGE = "android"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface OnboardingModule {
    @Binds
    fun onboardingSystem(system: AndroidOnboardingSystem): OnboardingSystem
}
