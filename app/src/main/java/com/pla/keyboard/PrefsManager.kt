package com.pla.keyboard

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pla_keyboard_pro_prefs", Context.MODE_PRIVATE)

    // 1. Tinggi & Tata Letak Ergonomis (Default 295dp agar nyaman untuk jari panjang)
    var keyboardHeightDp: Int
        get() = prefs.getInt("kb_height_dp", 295)
        set(value) = prefs.edit().putInt("kb_height_dp", value.coerceIn(200, 380)).apply()

    var bottomOffsetDp: Int
        get() = prefs.getInt("bottom_offset_dp", 14)
        set(value) = prefs.edit().putInt("bottom_offset_dp", value.coerceIn(0, 64)).apply()

    var keyGapDp: Int
        get() = prefs.getInt("key_gap_dp", 5)
        set(value) = prefs.edit().putInt("key_gap_dp", value.coerceIn(2, 12)).apply()

    var showNumberRow: Boolean
        get() = prefs.getBoolean("show_number_row", true)
        set(value) = prefs.edit().putBoolean("show_number_row", value).apply()

    // 2. Ketukan Dua Kali (Double-Tap) & Gestur
    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean("double_space_period", true)
        set(value) = prefs.edit().putBoolean("double_space_period", value).apply()

    var doubleLetterCaps: Boolean
        get() = prefs.getBoolean("double_letter_caps", false)
        set(value) = prefs.edit().putBoolean("double_letter_caps", value).apply()

    var doubleShiftCapsLock: Boolean
        get() = prefs.getBoolean("double_shift_lock", true)
        set(value) = prefs.edit().putBoolean("double_shift_lock", value).apply()

    var glideTypingEnabled: Boolean
        get() = prefs.getBoolean("glide_typing", true)
        set(value) = prefs.edit().putBoolean("glide_typing", value).apply()

    var doubleTapWindowMs: Int
        get() = prefs.getInt("double_tap_window_ms", 280)
        set(value) = prefs.edit().putInt("double_tap_window_ms", value.coerceIn(120, 500)).apply()

    // 3. Kecepatan, Suara & Getaran
    var keySoundEnabled: Boolean
        get() = prefs.getBoolean("key_sound_enabled", true)
        set(value) = prefs.edit().putBoolean("key_sound_enabled", value).apply()

    var soundVolumePercent: Int
        get() = prefs.getInt("sound_volume_pct", 80)
        set(value) = prefs.edit().putInt("sound_volume_pct", value.coerceIn(0, 100)).apply()

    var soundProfile: Int // 0 = Crisp, 1 = Soft, 2 = Mechanical
        get() = prefs.getInt("sound_profile", 0)
        set(value) = prefs.edit().putInt("sound_profile", value).apply()

    var hapticStrengthPercent: Int
        get() = prefs.getInt("haptic_strength_pct", 70)
        set(value) = prefs.edit().putInt("haptic_strength_pct", value.coerceIn(0, 100)).apply()

    // 4. Aksesibilitas & Tampilan Anti-Silau
    var screenReaderSpeakKeys: Boolean
        get() = prefs.getBoolean("sr_speak_keys", false)
        set(value) = prefs.edit().putBoolean("sr_speak_keys", value).apply()

    var twoTouchModeEnabled: Boolean
        get() = prefs.getBoolean("two_touch_mode", false)
        set(value) = prefs.edit().putBoolean("two_touch_mode", value).apply()

    var spatialAudioEnabled: Boolean
        get() = prefs.getBoolean("spatial_audio_keys", false)
        set(value) = prefs.edit().putBoolean("spatial_audio_keys", value).apply()

    var antiGlareTheme: Int // 0 = Dark Soft, 1 = Light Matte, 2 = High Contrast
        get() = prefs.getInt("anti_glare_theme", 0)
        set(value) = prefs.edit().putInt("anti_glare_theme", value).apply()

    var keyFontSizeSp: Int
        get() = prefs.getInt("key_font_size_sp", 22)
        set(value) = prefs.edit().putInt("key_font_size_sp", value.coerceIn(16, 30)).apply()
}