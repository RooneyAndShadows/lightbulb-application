package com.github.rooneyandshadows.lightbulb.application.activity.slidermenu.drawable

import android.content.Context
import android.graphics.*
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable

abstract class BaseHomeDrawable(context: Context?) : DrawerArrowDrawable(context) {
    private val backgroundPaint: Paint
    private val textPaint: Paint
    private var text: String? = null
    private var enabled = true

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (!enabled) {
            return
        }
        val bounds = bounds
        val x = (1 - HALF_SIZE_FACTOR) * bounds.width()
        val y = HALF_SIZE_FACTOR * bounds.height()
        canvas.drawCircle(x, y, SIZE_FACTOR * bounds.width(), backgroundPaint)
        if (text == null || text!!.length == 0) {
            return
        }
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text!!.length, textBounds)
        canvas.drawText(text!!, x, y + textBounds.height() / 2, textPaint)
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            invalidateSelf()
        }
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setText(text: String?) {
        if (this.text != text) {
            this.text = text
            invalidateSelf()
        }
    }

    fun getText(): String? {
        return text
    }

    var backgroundColor: Int
        get() = backgroundPaint.color
        set(color) {
            if (backgroundPaint.color != color) {
                backgroundPaint.color = color
                invalidateSelf()
            }
        }
    var textColor: Int
        get() = textPaint.color
        set(color) {
            if (textPaint.color != color) {
                textPaint.color = color
                invalidateSelf()
            }
        }

    companion object {
        // Fraction of the drawable's intrinsic size we want the badge to be.
        private const val SIZE_FACTOR = .30f
        private const val HALF_SIZE_FACTOR = SIZE_FACTOR / 2
    }

    init {
        backgroundPaint = Paint()
        backgroundPaint.color = Color.RED
        backgroundPaint.isAntiAlias = true
        textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.isAntiAlias = true
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = SIZE_FACTOR * intrinsicHeight
    }
}