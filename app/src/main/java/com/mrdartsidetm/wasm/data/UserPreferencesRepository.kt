package com.mrdartsidetm.wasm.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val USER_NAME_KEY = stringPreferencesKey("user_identity")
    private val PERSON_NAME_KEY = stringPreferencesKey("person_name")
    private val PROFILE_IMAGE_PATH_KEY = stringPreferencesKey("profile_image_path")
    private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_ENABLED_KEY = booleanPreferencesKey("dynamic_color_enabled")
    private val COLOR_VARIANT_KEY = stringPreferencesKey("color_variant")
    private val CHAT_WALLPAPER_KEY = stringPreferencesKey("chat_wallpaper")
    private val CHAT_WALLPAPER_IMAGE_PATH_KEY = stringPreferencesKey("chat_wallpaper_image_path")
    private val FONT_SELECTION_KEY = stringPreferencesKey("font_selection")

    // Flow that emits the current saved name (or empty string if not set)
    val userIdentity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY] ?: ""
    }

    // Flow for personalized person name displayed on Home and Personalize screens
    val personName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PERSON_NAME_KEY] ?: (prefs[USER_NAME_KEY] ?: "")
    }

    // Flow for saved profile image local file path
    val profileImagePath: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_IMAGE_PATH_KEY]
    }

    // Flow for selected app language code (defaults to "en")
    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_LANGUAGE_KEY] ?: "en"
    }

    // Flow tracking whether initial onboarding has been completed
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED_KEY] ?: false
    }

    // Theme mode: "system", "light", or "dark"
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "system"
    }

    // Whether Material You / Monet dynamic color is enabled
    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_ENABLED_KEY] ?: true
    }

    // Selected color palette variant: "default", "ocean", "emerald", "violet", "coral", "rose"
    val colorVariant: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COLOR_VARIANT_KEY] ?: "default"
    }

    // Selected chat wallpaper preset or "custom"
    val chatWallpaper: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CHAT_WALLPAPER_KEY] ?: "default"
    }

    // Saved custom chat wallpaper image local path
    val chatWallpaperImagePath: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CHAT_WALLPAPER_IMAGE_PATH_KEY]
    }

    // Selected typography family: "google_sans_flex", "sf_pro", "system", "monospace"
    val fontSelection: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FONT_SELECTION_KEY] ?: "google_sans_flex"
    }

    // Function to save selected language code
    suspend fun saveAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE_KEY] = languageCode
        }
    }

    // Function to persist onboarding completion status
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }

    suspend fun saveDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveColorVariant(variant: String) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_VARIANT_KEY] = variant
        }
    }

    suspend fun saveChatWallpaper(wallpaper: String) {
        context.dataStore.edit { prefs ->
            prefs[CHAT_WALLPAPER_KEY] = wallpaper
        }
    }

    suspend fun saveChatWallpaperImagePath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(CHAT_WALLPAPER_IMAGE_PATH_KEY)
            } else {
                prefs[CHAT_WALLPAPER_IMAGE_PATH_KEY] = path
            }
        }
    }

    suspend fun saveFontSelection(font: String) {
        context.dataStore.edit { prefs ->
            prefs[FONT_SELECTION_KEY] = font
        }
    }

    // Function to save the selected "Who are you?" name
    suspend fun saveUserIdentity(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
            if (prefs[PERSON_NAME_KEY].isNullOrEmpty()) {
                prefs[PERSON_NAME_KEY] = name
            }
        }
    }

    // Function to save personalized person name
    suspend fun savePersonName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PERSON_NAME_KEY] = name
            if (prefs[USER_NAME_KEY].isNullOrEmpty()) {
                prefs[USER_NAME_KEY] = name
            }
        }
    }

    // Function to save or clear profile photo local file path
    suspend fun saveProfileImagePath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(PROFILE_IMAGE_PATH_KEY)
            } else {
                prefs[PROFILE_IMAGE_PATH_KEY] = path
            }
        }
    }
}

