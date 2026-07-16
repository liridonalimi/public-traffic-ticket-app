package com.buspay.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage {
    SYSTEM,
    ALBANIAN,
    ENGLISH
}

fun resolvedLanguageTag(selection: AppLanguage, systemLanguageTag: String): String {
    return when (selection) {
        AppLanguage.ALBANIAN -> ALBANIAN_LANGUAGE_TAG
        AppLanguage.ENGLISH -> ENGLISH_LANGUAGE_TAG
        AppLanguage.SYSTEM -> if (systemLanguageTag.startsWith("sq", ignoreCase = true)) {
            ALBANIAN_LANGUAGE_TAG
        } else {
            ENGLISH_LANGUAGE_TAG
        }
    }
}

object AppLanguageManager {
    fun selectedLanguage(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, null)
        return runCatching { AppLanguage.valueOf(stored.orEmpty()) }
            .getOrDefault(AppLanguage.SYSTEM)
    }

    fun localizedContext(context: Context): Context {
        val languageTag = resolvedLanguageTag(
            selection = selectedLanguage(context),
            systemLanguageTag = systemLanguageTag()
        )
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    fun selectLanguage(application: Application, selection: AppLanguage) {
        application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, selection.name)
            .apply()

        val locale = Locale.forLanguageTag(
            resolvedLanguageTag(selection, systemLanguageTag())
        )
        Locale.setDefault(locale)
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(locale)
        }
        application.resources.updateConfiguration(
            configuration,
            application.resources.displayMetrics
        )
    }

    private fun systemLanguageTag(): String {
        return android.content.res.Resources.getSystem().configuration.locales[0].toLanguageTag()
    }

    private const val PREFERENCES_NAME = "buspay_app_preferences"
    private const val LANGUAGE_KEY = "app_language"
}

private const val ALBANIAN_LANGUAGE_TAG = "sq"
private const val ENGLISH_LANGUAGE_TAG = "en"
