package com.mrdartsidetm.wasm.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    private val DYNAMIC_VARIANT_KEY = stringPreferencesKey("dynamic_variant")
    private val CHAT_WALLPAPER_KEY = stringPreferencesKey("chat_wallpaper")
    private val CHAT_WALLPAPER_IMAGE_PATH_KEY = stringPreferencesKey("chat_wallpaper_image_path")
    private val FONT_SELECTION_KEY = stringPreferencesKey("font_selection")
    private val EMOJI_FONT_KEY = stringPreferencesKey("emoji_font")
    private val EMOJI_REVISION_KEY = intPreferencesKey("emoji_revision")
    private val ARCHIVE_HIDDEN_KEY = booleanPreferencesKey("archive_hidden")

    // Google Sans Flex variable font axes
    private val GSF_GRADE_KEY = floatPreferencesKey("gsf_grade")
    private val GSF_WEIGHT_KEY = floatPreferencesKey("gsf_weight")
    private val GSF_WIDTH_KEY = floatPreferencesKey("gsf_width")
    private val GSF_ROUNDNESS_KEY = floatPreferencesKey("gsf_roundness")
    private val GSF_OPTICAL_SIZE_KEY = floatPreferencesKey("gsf_optical_size")
    private val GSF_SLANT_KEY = floatPreferencesKey("gsf_slant")

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

    // Selected static color palette variant when Dynamic Color is OFF
    val colorVariant: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COLOR_VARIANT_KEY] ?: "default"
    }

    // Selected Material 3 DynamicSchemeVariant when Dynamic Color is ON
    val dynamicVariant: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_VARIANT_KEY] ?: "tonal_spot"
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

    // Selected emoji font pack: defaults to "whatsapp" (WhatsApp 2.26.8.72)
    val emojiFont: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[EMOJI_FONT_KEY] ?: "whatsapp"
    }

    // Monotonic revision bumped whenever a runtime emoji TTF update is installed or reset
    val emojiRevision: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EMOJI_REVISION_KEY] ?: 0
    }

    fun emojiOverrideLabelFlow(packId: String): Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey("emoji_override_label_$packId")]
    }

    // Whether the Archive card on Home is hidden by long-press
    val isArchiveHidden: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ARCHIVE_HIDDEN_KEY] ?: false
    }

    val gsfGrade: Flow<Float> = context.dataStore.data.map { it[GSF_GRADE_KEY] ?: 0f }
    val gsfWeight: Flow<Float> = context.dataStore.data.map { it[GSF_WEIGHT_KEY] ?: 400f }
    val gsfWidth: Flow<Float> = context.dataStore.data.map { it[GSF_WIDTH_KEY] ?: 100f }
    val gsfRoundness: Flow<Float> = context.dataStore.data.map { it[GSF_ROUNDNESS_KEY] ?: 100f }
    val gsfOpticalSize: Flow<Float> = context.dataStore.data.map { it[GSF_OPTICAL_SIZE_KEY] ?: 24f }
    val gsfSlant: Flow<Float> = context.dataStore.data.map { it[GSF_SLANT_KEY] ?: 0f }

    suspend fun saveAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs -> prefs[APP_LANGUAGE_KEY] = languageCode }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETED_KEY] = completed }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode }
    }

    suspend fun saveDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DYNAMIC_COLOR_ENABLED_KEY] = enabled }
    }

    suspend fun saveColorVariant(variant: String) {
        context.dataStore.edit { prefs -> prefs[COLOR_VARIANT_KEY] = variant }
    }

    suspend fun saveDynamicVariant(variant: String) {
        context.dataStore.edit { prefs -> prefs[DYNAMIC_VARIANT_KEY] = variant }
    }

    suspend fun saveChatWallpaper(wallpaper: String) {
        context.dataStore.edit { prefs -> prefs[CHAT_WALLPAPER_KEY] = wallpaper }
    }

    suspend fun saveChatWallpaperImagePath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(CHAT_WALLPAPER_IMAGE_PATH_KEY)
            else prefs[CHAT_WALLPAPER_IMAGE_PATH_KEY] = path
        }
    }

    suspend fun saveFontSelection(font: String) {
        context.dataStore.edit { prefs -> prefs[FONT_SELECTION_KEY] = font }
    }

    suspend fun saveEmojiFont(emojiPackId: String) {
        context.dataStore.edit { prefs -> prefs[EMOJI_FONT_KEY] = emojiPackId }
    }

    suspend fun recordEmojiOverride(packId: String, label: String?) {
        val labelKey = stringPreferencesKey("emoji_override_label_$packId")
        context.dataStore.edit { prefs ->
            if (label.isNullOrBlank()) prefs.remove(labelKey) else prefs[labelKey] = label
            prefs[EMOJI_REVISION_KEY] = (prefs[EMOJI_REVISION_KEY] ?: 0) + 1
            prefs[EMOJI_FONT_KEY] = packId
        }
    }

    suspend fun bumpEmojiRevisionOnly(packId: String) {
        val labelKey = stringPreferencesKey("emoji_override_label_$packId")
        context.dataStore.edit { prefs ->
            prefs.remove(labelKey)
            prefs[EMOJI_REVISION_KEY] = (prefs[EMOJI_REVISION_KEY] ?: 0) + 1
        }
    }

    suspend fun saveArchiveHidden(hidden: Boolean) {
        context.dataStore.edit { prefs -> prefs[ARCHIVE_HIDDEN_KEY] = hidden }
    }

    suspend fun saveGoogleSansFlexAxes(
        grade: Float? = null,
        weight: Float? = null,
        width: Float? = null,
        roundness: Float? = null,
        opticalSize: Float? = null,
        slant: Float? = null
    ) {
        context.dataStore.edit { prefs ->
            if (grade != null) prefs[GSF_GRADE_KEY] = grade
            if (weight != null) prefs[GSF_WEIGHT_KEY] = weight
            if (width != null) prefs[GSF_WIDTH_KEY] = width
            if (roundness != null) prefs[GSF_ROUNDNESS_KEY] = roundness
            if (opticalSize != null) prefs[GSF_OPTICAL_SIZE_KEY] = opticalSize
            if (slant != null) prefs[GSF_SLANT_KEY] = slant
        }
    }

    suspend fun resetGoogleSansFlexAxes() {
        context.dataStore.edit { prefs ->
            prefs[GSF_GRADE_KEY] = 0f
            prefs[GSF_WEIGHT_KEY] = 400f
            prefs[GSF_WIDTH_KEY] = 100f
            prefs[GSF_ROUNDNESS_KEY] = 100f
            prefs[GSF_OPTICAL_SIZE_KEY] = 24f
            prefs[GSF_SLANT_KEY] = 0f
        }
    }

    suspend fun saveUserIdentity(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
            if (prefs[PERSON_NAME_KEY].isNullOrEmpty()) {
                prefs[PERSON_NAME_KEY] = name
            }
        }
    }

    suspend fun savePersonName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PERSON_NAME_KEY] = name
            if (prefs[USER_NAME_KEY].isNullOrEmpty()) {
                prefs[USER_NAME_KEY] = name
            }
        }
    }

    suspend fun saveProfileImagePath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(PROFILE_IMAGE_PATH_KEY)
            else prefs[PROFILE_IMAGE_PATH_KEY] = path
        }
    }
}
