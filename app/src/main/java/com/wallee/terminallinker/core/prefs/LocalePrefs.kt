package com.wallee.terminallinker.core.prefs

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Language choice in the settings (docs/03 §Einstellungen → Anzeige → Sprache). */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    GERMAN("de-CH"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromName(name: String?): AppLanguage = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * The app language lives in plain SharedPreferences (not DataStore) because it has to be read
 * synchronously in `attachBaseContext`, before any coroutine can run. Non-sensitive by nature.
 * On Android 13+ the choice is mirrored into the system's per-app language setting.
 */
class LocalePrefs(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(AppLanguage.fromName(prefs.getString(KEY_LANGUAGE, null)))
    val language: StateFlow<AppLanguage> = _language

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _language.value = language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = appContext.getSystemService(LocaleManager::class.java)
            manager?.applicationLocales =
                language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        }
    }

    /** Wraps [base] so its resources use the chosen language; returns [base] itself for SYSTEM. */
    fun wrap(base: Context): Context {
        val tag = _language.value.tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    fun clear() {
        setLanguage(AppLanguage.SYSTEM)
    }

    private companion object {
        const val PREFS_NAME = "tl.locale"
        const val KEY_LANGUAGE = "language"
    }
}
