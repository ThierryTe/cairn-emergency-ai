package com.cairn.emergency.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.cairn.emergency.R
import java.util.Locale

internal enum class CairnLanguage(val tag: String, @StringRes val nativeName: Int, val promptName: String) {
    English("en", R.string.language_english, "English"),
    French("fr", R.string.language_french, "French"),
    Spanish("es", R.string.language_spanish, "Spanish");

    companion object {
        fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag?.substringBefore('-') } ?: English
    }
}

internal object CairnLocales {
    private const val Prefs = "cairn_language"
    private const val Key = "language"

    fun current(context: Context): CairnLanguage {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales != null && !locales.isEmpty) return CairnLanguage.fromTag(locales[0].language)
        }
        return CairnLanguage.fromTag(context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, null))
    }

    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(current(context).tag))
        return context.createConfigurationContext(configuration)
    }

    fun migrateLegacySelection(context: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        val preferences = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        val legacy = preferences.getString(Key, null) ?: return
        if (manager.applicationLocales.isEmpty) {
            manager.applicationLocales = LocaleList.forLanguageTags(CairnLanguage.fromTag(legacy).tag)
        }
        preferences.edit().remove(Key).apply()
    }

    fun select(activity: Activity, language: CairnLanguage) {
        if (current(activity) == language) return
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = activity.getSystemService(LocaleManager::class.java)
            if (manager != null) {
                manager.applicationLocales = LocaleList.forLanguageTags(language.tag)
                return
            }
        }
        activity.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, language.tag).apply()
        activity.recreate()
    }
}
