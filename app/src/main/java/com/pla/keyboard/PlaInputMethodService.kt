package com.pla.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import com.pla.keyboard.databinding.KeyboardContainerBinding

class PlaInputMethodService : InputMethodService(), ZeroLatencyKeyboardView.KeyboardActionListener {

    private lateinit var binding: KeyboardContainerBinding
    private lateinit var prefs: PrefsManager
    private lateinit var feedbackEngine: InstantSoundHapticEngine
    private val dictionary = FastTrieDictionary()
    private val currentWordBuffer = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        feedbackEngine = InstantSoundHapticEngine(this, prefs)
    }

    override fun onCreateInputView(): View {
        binding = KeyboardContainerBinding.inflate(layoutInflater)
        
        // Inisialisasi keyboard view
        binding.zeroLatencyKeyboard.initEngines(prefs, feedbackEngine)
        binding.zeroLatencyKeyboard.actionListener = this
        
        // PERBAIKAN: Pastikan view bisa menerima fokus dan sentuhan
        binding.zeroLatencyKeyboard.apply {
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }

        applyErgonomicDimensions()
        setupTopBarControls()
        updateSuggestionBar()
        
        return binding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::binding.isInitialized) {
            // Refresh dimensi setiap kali keyboard muncul untuk memastikan sinkronisasi
            applyErgonomicDimensions()
            binding.zeroLatencyKeyboard.refreshLayout()
            binding.zeroLatencyKeyboard.requestFocus()
        }
    }

    private fun applyErgonomicDimensions() {
        val density = resources.displayMetrics.density
        
        // PERBAIKAN: Gunakan minHeight dari XML sebagai baseline, 
        // lalu sesuaikan hanya jika preferensi memiliki nilai valid
        val targetHeightDp = prefs.keyboardHeightDp
        if (targetHeightDp > 0) {
            val kbParams = binding.zeroLatencyKeyboard.layoutParams
            kbParams.height = (targetHeightDp * density).toInt()
            binding.zeroLatencyKeyboard.layoutParams = kbParams
        }

        val offsetParams = binding.viewBottomOffset.layoutParams
        offsetParams.height = (prefs.bottomOffsetDp * density).toInt()
        binding.viewBottomOffset.layoutParams = offsetParams
    }

    private fun setupTopBarControls() {
        val suggViews = listOf(binding.tvSugg1, binding.tvSugg2, binding.tvSugg3)
        suggViews.forEach { tv ->
            tv.setOnClickListener {
                val chosenWord = tv.text.toString()
                if (chosenWord.isNotBlank()) {
                    replaceCurrentWordWith("$chosenWord ")
                }
            }
        }

        binding.btnKbSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        binding.btnKbClipboard.setOnClickListener {
            toggleClipboardPanel()
        }

        binding.btnCloseClipboard.setOnClickListener {
            binding.panelClipboard.visibility = View.GONE
            binding.zeroLatencyKeyboard.visibility = View.VISIBLE
        }
    }

    private fun toggleClipboardPanel() {
        if (binding.panelClipboard.visibility == View.VISIBLE) {
            binding.panelClipboard.visibility = View.GONE
            binding.zeroLatencyKeyboard.visibility = View.VISIBLE
            return
        }

        binding.containerClipItems.removeAllViews()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val primaryClip = cm.primaryClip

        if (primaryClip != null && primaryClip.itemCount > 0) {
            for (i in 0 until primaryClip.itemCount) {
                val text = primaryClip.getItemAt(i).coerceToText(this)?.toString() ?: continue
                val btnItem = Button(this).apply {
                    this.text = text.take(65)
                    isAllCaps = false
                    setOnClickListener {
                        currentInputConnection?.commitText(text, 1)
                        binding.panelClipboard.visibility = View.GONE
                        binding.zeroLatencyKeyboard.visibility = View.VISIBLE
                    }
                }
                binding.containerClipItems.addView(btnItem)
            }
        } else {
            val emptyBtn = Button(this).apply {
                text = "Papan klip masih kosong"
                isEnabled = false
            }
            binding.containerClipItems.addView(emptyBtn)
        }

        binding.zeroLatencyKeyboard.visibility = View.GONE
        binding.panelClipboard.visibility = View.VISIBLE
    }

    override fun onCharacterInput(charText: String, isDoubleTapCaps: Boolean) {
        val ic = currentInputConnection ?: return

        if (isDoubleTapCaps && currentWordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(1, 0)
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }

        ic.commitText(charText, 1)

        if (charText.length == 1 && charText[0].isLetter()) {
            currentWordBuffer.append(charText)
        } else {
            if (currentWordBuffer.length >= 2) {
                dictionary.insert(currentWordBuffer.toString(), 60)
            }
            currentWordBuffer.clear()
        }
        updateSuggestionBar()
    }

    override fun onDoubleTapSpacePeriod() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
        ic.commitText(". ", 1)
        currentWordBuffer.clear()
        updateSuggestionBar()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }
        updateSuggestionBar()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        currentWordBuffer.clear()
        updateSuggestionBar()
    }

    override fun onGlideWordCompleted(swipedLetters: String) {
        val matched = dictionary.matchSwipeTrail(swipedLetters) ?: swipedLetters
        currentInputConnection?.commitText("$matched ", 1)
        currentWordBuffer.clear()
        updateSuggestionBar()
    }

    private fun replaceCurrentWordWith(fullWordWithSpace: String) {
        val ic = currentInputConnection ?: return
        if (currentWordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(currentWordBuffer.length, 0)
            currentWordBuffer.clear()
        }
        ic.commitText(fullWordWithSpace, 1)
        updateSuggestionBar()
    }

    private fun updateSuggestionBar() {
        val suggestions = dictionary.getTopSuggestions(currentWordBuffer.toString(), 3)
        binding.tvSugg1.text = suggestions.getOrElse(0) { "" }
        binding.tvSugg2.text = suggestions.getOrElse(1) { "" }
        binding.tvSugg3.text = suggestions.getOrElse(2) { "" }
    }

    override fun onDestroy() {
        feedbackEngine.shutdown()
        super.onDestroy()
    }
}