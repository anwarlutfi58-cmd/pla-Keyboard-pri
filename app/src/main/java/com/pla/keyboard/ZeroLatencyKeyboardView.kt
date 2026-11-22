package com.pla.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class ZeroLatencyKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface KeyboardActionListener {
        fun onCharacterInput(charText: String, isDoubleTapCaps: Boolean)
        fun onDoubleTapSpacePeriod()
        fun onBackspace()
        fun onEnter()
        fun onGlideWordCompleted(swipedLetters: String)
    }

    data class KeyItem(
        val code: String,
        val displayLabel: String,
        val rect: RectF,
        val isFunctional: Boolean = false
    )

    var actionListener: KeyboardActionListener? = null
    private lateinit var prefs: PrefsManager
    private lateinit var feedbackEngine: InstantSoundHapticEngine

    private var isSymbolsMode = false
    private var shiftState = 1 // 0 = lowercase, 1 = shift once, 2 = caps lock

    private val renderedKeys = mutableListOf<KeyItem>()
    private var pressedKey: KeyItem? = null

    // Pelacakan Ketukan Dua Kali (Double-Tap)
    private var lastTappedCode: String = ""
    private var lastTapTimestamp: Long = 0L

    // Pelacakan Mode Dua Sentuhan Aksesibilitas
    private var hoveredAccessibilityCode: String = ""

    // Pelacakan Jalur Usapan Kata (Glide / Swipe Typing)
    private val glidePath = Path()
    private val glideLetterTrail = StringBuilder()
    private var isGliding = false
    private var downX = 0f
    private var downY = 0f

    // Kuas Rendering Zero-Latency
    private val paintKeyBg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val paintPopup = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintGlideTrail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 9f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun initEngines(prefsManager: PrefsManager, engine: InstantSoundHapticEngine) {
        this.prefs = prefsManager
        this.feedbackEngine = engine
        rebuildKeyGeometry(width, height)
        invalidate()
    }

    fun refreshLayout() {
        rebuildKeyGeometry(width, height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildKeyGeometry(w, h)
    }

    private fun rebuildKeyGeometry(viewW: Int, viewH: Int) {
        renderedKeys.clear()
        if (viewW <= 0 || viewH <= 0) return

        val density = resources.displayMetrics.density
        val gapPx = (if (::prefs.isInitialized) prefs.keyGapDp else 5) * density
        val includeNumberRow = if (::prefs.isInitialized) prefs.showNumberRow else true

        val rows = mutableListOf<List<Pair<String, Float>>>()

        if (includeNumberRow) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { it to 1f })
        }

        if (!isSymbolsMode) {
            rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { it to 1f })
            rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { it to 1f })
            rows.add(
                listOf("SHIFT" to 1.45f) +
                    listOf("z", "x", "c", "v", "b", "n", "m").map { it to 1f } +
                    listOf("DEL" to 1.45f)
            )
            rows.add(
                listOf(
                    "?123" to 1.4f,
                    "," to 1.0f,
                    " " to 4.2f,
                    "." to 1.0f,
                    "ENTER" to 1.6f
                )
            )
        } else {
            rows.add(listOf("@", "#", "Rp", "_", "&", "-", "+", "(", ")", "/").map { it to 1f })
            rows.add(listOf("*", "\"", "'", ":", ";", "!", "?", "~", "=").map { it to 1f })
            rows.add(
                listOf("ABC" to 1.45f) +
                    listOf("%", "[", "]", "{", "}", "\\", "|").map { it to 1f } +
                    listOf("DEL" to 1.45f)
            )
            rows.add(
                listOf(
                    "ABC" to 1.4f,
                    "," to 1.0f,
                    " " to 4.2f,
                    "." to 1.0f,
                    "ENTER" to 1.6f
                )
            )
        }

        val rowCount = rows.size
        val rowHeight = (viewH - gapPx * (rowCount + 1)) / rowCount

        for (rIdx in rows.indices) {
            val rowItems = rows[rIdx]
            val totalWeight = rowItems.sumOf { it.second.toDouble() }.toFloat()
            val usableWidth = viewW - gapPx * (rowItems.size + 1)

            // Pemusatan baris ke-2 (ASDF...) agar proporsional
            val totalRowActualW = usableWidth + gapPx * (rowItems.size - 1)
            var currentX = (viewW - totalRowActualW) / 2f
            val topY = gapPx + rIdx * (rowHeight + gapPx)

            for ((code, weight) in rowItems) {
                val keyW = (usableWidth * (weight / totalWeight))
                val isFunc = code in setOf("SHIFT", "DEL", "?123", "ABC", "ENTER")
                val label = formatDisplayLabel(code)
                renderedKeys.add(
                    KeyItem(
                        code = code,
                        displayLabel = label,
                        rect = RectF(currentX, topY, currentX + keyW, topY + rowHeight),
                        isFunctional = isFunc
                    )
                )
                currentX += keyW + gapPx
            }
        }
    }

    private fun formatDisplayLabel(code: String): String {
        return when (code) {
            "SHIFT" -> if (shiftState == 2) "⇪" else "⇧"
            "DEL" -> "⌫"
            "ENTER" -> "↵"
            " " -> "Pla Keyboard Pro"
            else -> if (shiftState > 0 && code.length == 1) code.uppercase() else code
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val theme = if (::prefs.isInitialized) prefs.antiGlareTheme else 0
        val fontSizeSp = if (::prefs.isInitialized) prefs.keyFontSizeSp.toFloat() else 22f
        val density = resources.displayMetrics.density

        val (bgColor, keyColor, funcColor, textColor) = when (theme) {
            1 -> QuadColor("#DCE1E7", "#F6F8FA", "#C7CFD9", "#171C24") // Terang Teduh
            2 -> QuadColor("#0A0C0F", "#1A2029", "#242C38", "#FFE66D") // Kontras Tinggi
            else -> QuadColor("#161A20", "#252B35", "#1D222B", "#F4F2EC") // Gelap Anti-Silau
        }

        canvas.drawColor(bgColor)
        paintKeyText.textSize = fontSizeSp * density

        for (key in renderedKeys) {
            val isPressed = (pressedKey?.code == key.code)
            paintKeyBg.color = when {
                isPressed -> Color.parseColor("#38BDF8")
                key.code == "ENTER" -> Color.parseColor("#34D399")
                key.isFunctional -> funcColor
                else -> keyColor
            }

            canvas.drawRoundRect(key.rect, 16f, 16f, paintKeyBg)

            paintKeyText.color = when {
                isPressed || key.code == "ENTER" -> Color.parseColor("#0B131A")
                key.code == " " -> Color.parseColor("#94A3B8")
                else -> textColor
            }

            if (key.code == " ") {
                paintKeyText.textSize = 13f * density
            } else {
                paintKeyText.textSize = fontSizeSp * density
            }

            val textY = key.rect.centerY() - (paintKeyText.descent() + paintKeyText.ascent()) / 2f
            canvas.drawText(key.displayLabel, key.rect.centerX(), textY, paintKeyText)
        }

        // Gambar jejak usapan Glide Typing
        if (isGliding) {
            canvas.drawPath(glidePath, paintGlideTrail)
        }

        // Pre-Rendered Key Preview Popup Instan saat huruf ditekan
        pressedKey?.let { pk ->
            if (!pk.isFunctional && pk.code != " ") {
                paintPopup.color = Color.parseColor("#38BDF8")
                val popupRect = RectF(
                    pk.rect.left - 8f,
                    (pk.rect.top - pk.rect.height() * 0.95f).coerceAtLeast(4f),
                    pk.rect.right + 8f,
                    pk.rect.top - 4f
                )
                canvas.drawRoundRect(popupRect, 14f, 14f, paintPopup)
                paintKeyText.color = Color.parseColor("#0B131A")
                paintKeyText.textSize = (fontSizeSp + 4f) * density
                val py = popupRect.centerY() - (paintKeyText.descent() + paintKeyText.ascent()) / 2f
                canvas.drawText(pk.displayLabel, popupRect.centerX(), py, paintKeyText)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                isGliding = false
                glidePath.reset()
                glidePath.moveTo(x, y)
                glideLetterTrail.clear()

                val hit = findKeyAt(x, y)
                pressedKey = hit
                if (hit != null) {
                    if (hit.code.length == 1 && hit.code[0].isLetter()) {
                        glideLetterTrail.append(hit.code)
                    }
                    val normX = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    if (::feedbackEngine.isInitialized) {
                        feedbackEngine.triggerInstantKeyFeedback(normX, hit.isFunctional)
                        if (prefs.screenReaderSpeakKeys) {
                            feedbackEngine.speakKeyForAccessibility(hit.displayLabel)
                        }
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val hit = findKeyAt(x, y)
                if (::prefs.isInitialized && prefs.glideTypingEnabled && !isSymbolsMode) {
                    if (abs(x - downX) > 36f || abs(y - downY) > 36f) {
                        isGliding = true
                        glidePath.lineTo(x, y)
                        if (hit != null && hit.code.length == 1 && hit.code[0].isLetter()) {
                            if (glideLetterTrail.isEmpty() || glideLetterTrail.last().toString() != hit.code) {
                                glideLetterTrail.append(hit.code)
                            }
                        }
                    }
                }
                if (!isGliding && hit != null && hit.code != pressedKey?.code) {
                    pressedKey = hit
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val releaseKey = pressedKey ?: findKeyAt(x, y)
                pressedKey = null

                if (isGliding && glideLetterTrail.length >= 2) {
                    isGliding = false
                    glidePath.reset()
                    actionListener?.onGlideWordCompleted(glideLetterTrail.toString())
                    invalidate()
                    return true
                }

                isGliding = false
                glidePath.reset()

                if (releaseKey != null) {
                    handleKeyCommit(releaseKey)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedKey = null
                isGliding = false
                glidePath.reset()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleKeyCommit(key: KeyItem) {
        val now = System.currentTimeMillis()
        val windowMs = if (::prefs.isInitialized) prefs.doubleTapWindowMs.toLong() else 280L
        val isDoubleTap = (key.code == lastTappedCode) && ((now - lastTapTimestamp) <= windowMs)

        // Mode Dua Sentuhan Aksesibilitas: Sentuhan 1 hanya membacakan tombol, Sentuhan 2 mengetik
        if (::prefs.isInitialized && prefs.twoTouchModeEnabled) {
            if (hoveredAccessibilityCode != key.code) {
                hoveredAccessibilityCode = key.code
                feedbackEngine.speakKeyForAccessibility(key.displayLabel)
                return
            }
            hoveredAccessibilityCode = ""
        }

        lastTappedCode = key.code
        lastTapTimestamp = now

        when (key.code) {
            "SHIFT" -> {
                shiftState = if (isDoubleTap && prefs.doubleShiftCapsLock) {
                    2 // Caps Lock aktif
                } else {
                    if (shiftState == 0) 1 else 0
                }
                rebuildKeyGeometry(width, height)
            }

            "?123", "ABC" -> {
                isSymbolsMode = !isSymbolsMode
                rebuildKeyGeometry(width, height)
            }

            "DEL" -> {
                actionListener?.onBackspace()
            }

            "ENTER" -> {
                actionListener?.onEnter()
            }

            " " -> {
                if (isDoubleTap && ::prefs.isInitialized && prefs.doubleSpacePeriod) {
                    actionListener?.onDoubleTapSpacePeriod()
                    shiftState = 1
                    rebuildKeyGeometry(width, height)
                } else {
                    actionListener?.onCharacterInput(" ", isDoubleTapCaps = false)
                }
            }

            else -> {
                val forceDoubleCaps = isDoubleTap && ::prefs.isInitialized && prefs.doubleLetterCaps && key.code.length == 1
                val outputChar = if (shiftState > 0 || forceDoubleCaps) key.code.uppercase() else key.code
                actionListener?.onCharacterInput(outputChar, isDoubleTapCaps = forceDoubleCaps)

                if (shiftState == 1) {
                    shiftState = 0
                    rebuildKeyGeometry(width, height)
                }
            }
        }
    }

    private fun findKeyAt(x: Float, y: Float): KeyItem? {
        for (key in renderedKeys) {
            if (key.rect.contains(x, y)) return key
        }
        return null
    }

    private data class QuadColor(val bg: Int, val key: Int, val func: Int, val text: Int) {
        constructor(bgHex: String, keyHex: String, funcHex: String, textHex: String) : this(
            Color.parseColor(bgHex),
            Color.parseColor(keyHex),
            Color.parseColor(funcHex),
            Color.parseColor(textHex)
        )
    }
}