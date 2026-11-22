package com.pla.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.pla.keyboard.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    private enum class SettingsPage {
        HOME, HEIGHT_LAYOUT, DOUBLE_TAP, SOUND_HAPTIC, ACCESSIBILITY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupNavigation()
        setupHeightAndLayoutControls()
        setupDoubleTapControls()
        setupSoundAndHapticControls()
        setupAccessibilityControls()

        showPage(SettingsPage.HOME)
    }

    private fun showPage(page: SettingsPage) {
        binding.pageHome.visibility = if (page == SettingsPage.HOME) View.VISIBLE else View.GONE
        binding.pageHeightLayout.visibility = if (page == SettingsPage.HEIGHT_LAYOUT) View.VISIBLE else View.GONE
        binding.pageDoubleTap.visibility = if (page == SettingsPage.DOUBLE_TAP) View.VISIBLE else View.GONE
        binding.pageSoundHaptic.visibility = if (page == SettingsPage.SOUND_HAPTIC) View.VISIBLE else View.GONE
        binding.pageAccessibility.visibility = if (page == SettingsPage.ACCESSIBILITY) View.VISIBLE else View.GONE

        // Tombol Kembali khusus di pojok kanan bawah muncul pada setiap halaman sub-menu
        binding.btnBackBottomRight.visibility =
            if (page == SettingsPage.HOME) View.GONE else View.VISIBLE
    }

    private fun setupNavigation() {
        binding.btnEnableIme.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        binding.btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        binding.btnMenuHeightLayout.setOnClickListener {
            syncHeightLayoutUI()
            showPage(SettingsPage.HEIGHT_LAYOUT)
        }

        binding.btnMenuDoubleTap.setOnClickListener {
            syncDoubleTapUI()
            showPage(SettingsPage.DOUBLE_TAP)
        }

        binding.btnMenuSoundHaptic.setOnClickListener {
            syncSoundHapticUI()
            showPage(SettingsPage.SOUND_HAPTIC)
        }

        binding.btnMenuAccessibility.setOnClickListener {
            syncAccessibilityUI()
            showPage(SettingsPage.ACCESSIBILITY)
        }

        binding.btnBackBottomRight.setOnClickListener {
            showPage(SettingsPage.HOME)
        }
    }

    // 1. Tinggi & Tata Letak Keyboard (Jari Panjang)
    private fun syncHeightLayoutUI() {
        val h = prefs.keyboardHeightDp
        when {
            h <= 225 -> binding.rbHeightShort.isChecked = true
            h <= 265 -> binding.rbHeightStandard.isChecked = true
            h <= 310 -> binding.rbHeightTall.isChecked = true
            else -> binding.rbHeightExtraTall.isChecked = true
        }
        binding.seekPreciseHeight.progress = (h - 200).coerceIn(0, 180)
        binding.tvPreciseHeightLabel.text = "Penggeser Presisi Tinggi: $h dp"

        binding.seekBottomOffset.progress = prefs.bottomOffsetDp
        binding.tvBottomOffsetLabel.text = "Posisi Tinggi-Rendah Layar (Bottom Offset): ${prefs.bottomOffsetDp} dp"

        binding.seekKeyGap.progress = prefs.keyGapDp
        binding.tvKeyGapLabel.text = "Jarak Antar Tombol: ${prefs.keyGapDp} dp"

        binding.switchNumberRow.isChecked = prefs.showNumberRow
    }

    private fun setupHeightAndLayoutControls() {
        binding.rgHeightPreset.setOnCheckedChangeListener { _, checkedId ->
            val targetDp = when (checkedId) {
                R.id.rbHeightShort -> 215
                R.id.rbHeightStandard -> 255
                R.id.rbHeightTall -> 295
                else -> 335
            }
            prefs.keyboardHeightDp = targetDp
            binding.seekPreciseHeight.progress = targetDp - 200
            binding.tvPreciseHeightLabel.text = "Penggeser Presisi Tinggi: $targetDp dp"
        }

        binding.seekPreciseHeight.setOnSeekBarChangeListener(simpleSeek { progress ->
            val dp = 200 + progress
            prefs.keyboardHeightDp = dp
            binding.tvPreciseHeightLabel.text = "Penggeser Presisi Tinggi: $dp dp"
        })

        binding.seekBottomOffset.setOnSeekBarChangeListener(simpleSeek { progress ->
            prefs.bottomOffsetDp = progress
            binding.tvBottomOffsetLabel.text = "Posisi Tinggi-Rendah Layar (Bottom Offset): $progress dp"
        })

        binding.seekKeyGap.setOnSeekBarChangeListener(simpleSeek { progress ->
            val gap = progress.coerceAtLeast(2)
            prefs.keyGapDp = gap
            binding.tvKeyGapLabel.text = "Jarak Antar Tombol: $gap dp"
        })

        binding.switchNumberRow.setOnCheckedChangeListener { _, checked ->
            prefs.showNumberRow = checked
        }
    }

    // 2. Ketukan Dua Kali & Gestur
    private fun syncDoubleTapUI() {
        binding.switchDoubleSpacePeriod.isChecked = prefs.doubleSpacePeriod
        binding.switchDoubleLetterCaps.isChecked = prefs.doubleLetterCaps
        binding.switchDoubleShiftLock.isChecked = prefs.doubleShiftCapsLock
        binding.switchGlideTyping.isChecked = prefs.glideTypingEnabled
        binding.seekDoubleTapWindow.progress = (prefs.doubleTapWindowMs - 100).coerceIn(0, 400)
        binding.tvDoubleTapWindowLabel.text = "Kecepatan Jendela Ketuk 2x: ${prefs.doubleTapWindowMs} ms"
    }

    private fun setupDoubleTapControls() {
        binding.switchDoubleSpacePeriod.setOnCheckedChangeListener { _, checked -> prefs.doubleSpacePeriod = checked }
        binding.switchDoubleLetterCaps.setOnCheckedChangeListener { _, checked -> prefs.doubleLetterCaps = checked }
        binding.switchDoubleShiftLock.setOnCheckedChangeListener { _, checked -> prefs.doubleShiftCapsLock = checked }
        binding.switchGlideTyping.setOnCheckedChangeListener { _, checked -> prefs.glideTypingEnabled = checked }

        binding.seekDoubleTapWindow.setOnSeekBarChangeListener(simpleSeek { progress ->
            val ms = 100 + progress
            prefs.doubleTapWindowMs = ms
            binding.tvDoubleTapWindowLabel.text = "Kecepatan Jendela Ketuk 2x: $ms ms"
        })
    }

    // 3. Kecepatan, Suara & Getaran
    private fun syncSoundHapticUI() {
        binding.switchKeySound.isChecked = prefs.keySoundEnabled
        binding.seekSoundVolume.progress = prefs.soundVolumePercent
        binding.tvSoundVolLabel.text = "Volume Suara Ketukan: ${prefs.soundVolumePercent}%"

        when (prefs.soundProfile) {
            1 -> binding.rbSoundSoft.isChecked = true
            2 -> binding.rbSoundMech.isChecked = true
            else -> binding.rbSoundCrisp.isChecked = true
        }

        binding.seekHapticStrength.progress = prefs.hapticStrengthPercent
        binding.tvHapticLabel.text = "Intensitas Getaran Mikro (Haptic): ${prefs.hapticStrengthPercent}%"
    }

    private fun setupSoundAndHapticControls() {
        binding.switchKeySound.setOnCheckedChangeListener { _, checked -> prefs.keySoundEnabled = checked }

        binding.seekSoundVolume.setOnSeekBarChangeListener(simpleSeek { progress ->
            prefs.soundVolumePercent = progress
            binding.tvSoundVolLabel.text = "Volume Suara Ketukan: $progress%"
        })

        binding.rgSoundProfile.setOnCheckedChangeListener { _, checkedId ->
            prefs.soundProfile = when (checkedId) {
                R.id.rbSoundSoft -> 1
                R.id.rbSoundMech -> 2
                else -> 0
            }
        }

        binding.seekHapticStrength.setOnSeekBarChangeListener(simpleSeek { progress ->
            prefs.hapticStrengthPercent = progress
            binding.tvHapticLabel.text = "Intensitas Getaran Mikro (Haptic): $progress%"
        })
    }

    // 4. Aksesibilitas & Tampilan Anti-Silau
    private fun syncAccessibilityUI() {
        binding.switchScreenReaderSpeak.isChecked = prefs.screenReaderSpeakKeys
        binding.switchTwoTouchConfirm.isChecked = prefs.twoTouchModeEnabled
        binding.switchSpatialAudio.isChecked = prefs.spatialAudioEnabled

        when (prefs.antiGlareTheme) {
            1 -> binding.rbThemeLight.isChecked = true
            2 -> binding.rbThemeHighContrast.isChecked = true
            else -> binding.rbThemeDark.isChecked = true
        }

        binding.seekKeyFontSize.progress = (prefs.keyFontSizeSp - 16).coerceIn(0, 14)
        binding.tvKeyFontSizeLabel.text = "Ukuran Huruf pada Tombol: ${prefs.keyFontSizeSp} sp"
    }

    private fun setupAccessibilityControls() {
        binding.switchScreenReaderSpeak.setOnCheckedChangeListener { _, checked -> prefs.screenReaderSpeakKeys = checked }
        binding.switchTwoTouchConfirm.setOnCheckedChangeListener { _, checked -> prefs.twoTouchModeEnabled = checked }
        binding.switchSpatialAudio.setOnCheckedChangeListener { _, checked -> prefs.spatialAudioEnabled = checked }

        binding.rgThemeAntiGlare.setOnCheckedChangeListener { _, checkedId ->
            prefs.antiGlareTheme = when (checkedId) {
                R.id.rbThemeLight -> 1
                R.id.rbThemeHighContrast -> 2
                else -> 0
            }
        }

        binding.seekKeyFontSize.setOnSeekBarChangeListener(simpleSeek { progress ->
            val sp = 16 + progress
            prefs.keyFontSizeSp = sp
            binding.tvKeyFontSizeLabel.text = "Ukuran Huruf pada Tombol: $sp sp"
        })
    }

    private fun simpleSeek(onUpdate: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onUpdate(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
    }
}