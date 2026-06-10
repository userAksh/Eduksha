package com.dobi.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.dobi.app.data.local.LanguagePreference
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.razorpay.Checkout
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class DobiApplication : Application() {

    override fun attachBaseContext(base: Context) {
        val lang = LanguagePreference.get(base)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        Checkout.preload(this)
        if (BuildConfig.DEBUG) {
            FirebaseAuth.getInstance().firebaseAuthSettings
                .setAppVerificationDisabledForTesting(true)
        }
    }
}
