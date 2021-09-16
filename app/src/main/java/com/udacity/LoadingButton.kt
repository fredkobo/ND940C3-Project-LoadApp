package com.udacity

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.withStyledAttributes
import kotlin.math.min
import kotlin.properties.Delegates.observable

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(
    context,
    attrs,
    defStyleAttr
) {

    private var lbStartBackgroundColor = 0
    private var lbBusyBackgroundColor = 0
    private var lbStartText: CharSequence = ""
    private var lbBusyText: CharSequence = ""
    private var lbTextColor = 0
    private var lbProgressCircleColor = 0

    private var widthValue = 0
    private var heightValue = 0

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var buttonText = ""
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55f
        typeface = Typeface.DEFAULT
    }

    private lateinit var buttonTextBounds: Rect
    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f

    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = 3000
        disableWhileAnimating(this@LoadingButton)
    }
    private var currentProgressCircleAnimationValue = 0f
    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
    }
    private var currentButtonBackgroundAnimationValue = 0f
    private lateinit var buttonBackgroundAnimator: ValueAnimator

    private var buttonState: ButtonState by observable<ButtonState>(ButtonState.Completed) { _, _, newState ->
        Log.d(TAG, "Button state changed: $newState")
        when (newState) {
            ButtonState.Loading -> {
                buttonText = lbBusyText.toString()
                if (!::buttonTextBounds.isInitialized) {
                    retrieveButtonTextBounds()
                    computeProgressCircleRect()
                }
                animatorSet.start()
            }
            else -> {
                buttonText = lbStartText.toString()
                newState.takeIf { it == ButtonState.Completed }?.run { animatorSet.cancel() }
            }
        }
    }

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            lbStartBackgroundColor = getColor(R.styleable.LoadingButton_lbStartBackgroundColor, 0)
            lbBusyBackgroundColor = getColor(R.styleable.LoadingButton_lbBusyBackgroundColor, 0)
            lbStartText = getText(R.styleable.LoadingButton_lbStartText)
            lbTextColor = getColor(R.styleable.LoadingButton_lbTextColor, 0)
            lbBusyText = getText(R.styleable.LoadingButton_lbBusyText)
            lbProgressCircleColor = getColor(R.styleable.LoadingButton_lbProgressCircleColor, 0)
            buttonText = lbStartText.toString()
        }
    }

    private fun retrieveButtonTextBounds() {
        buttonTextBounds = Rect()
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, buttonTextBounds)
    }

    private fun computeProgressCircleRect() {
        val horizontalCenter =
            (buttonTextBounds.right + buttonTextBounds.width() + 16f)
        val verticalCenter = (heightValue / 2f)

        progressCircleRect.set(
            horizontalCenter - progressCircleSize,
            verticalCenter - progressCircleSize,
            horizontalCenter + progressCircleSize,
            verticalCenter + progressCircleSize
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(
            minWidth,
            widthMeasureSpec,
            1
        )
        val h = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthValue = w
        heightValue = h
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressCircleSize = (min(w, h) / 2f) * 0.4f
        createButtonBackgroundAnimator()
    }

    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthValue.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
        }.also {
            buttonBackgroundAnimator = it
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }

    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, buttonBackgroundAnimator) }

    override fun performClick(): Boolean {
        super.performClick()
        if (buttonState == ButtonState.Completed) {
            buttonState = ButtonState.Clicked
            invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { buttonCanvas ->
            Log.d(TAG, "LoadingButton onDraw()")
            buttonCanvas.apply {
                drawBackgroundColor()
                drawButtonText()
                drawProgressCircleIfLoading()
            }
        }
    }

    private fun Canvas.drawButtonText() {
        buttonTextPaint.color = lbTextColor
        drawText(
            buttonText,
            (widthValue / 2f),
            (heightValue / 2f) + buttonTextPaint.computeTextOffset(),
            buttonTextPaint
        )
    }

    private fun TextPaint.computeTextOffset() = ((descent() - ascent()) / 2) - descent()

    private fun Canvas.drawBackgroundColor() {
        when (buttonState) {
            ButtonState.Loading -> {
                drawLoadingBackgroundColor()
                drawDefaultBackgroundColor()
            }
            else -> drawColor(lbStartBackgroundColor)
        }
    }

    private fun Canvas.drawLoadingBackgroundColor() = buttonPaint.apply {
        color = lbBusyBackgroundColor
    }.run {
        drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightValue.toFloat(),
            buttonPaint
        )
    }

    private fun Canvas.drawDefaultBackgroundColor() = buttonPaint.apply {
        color = lbStartBackgroundColor
    }.run {
        drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthValue.toFloat(),
            heightValue.toFloat(),
            buttonPaint
        )
    }

    private fun Canvas.drawProgressCircleIfLoading() =
        buttonState.takeIf { it == ButtonState.Loading }?.let { drawProgressCircle(this) }

    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonPaint.color = lbProgressCircleColor
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            buttonPaint
        )
    }

    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }

    fun AnimatorSet.disableWhileAnimating(view: View) = apply {
        doOnStart { view.isEnabled = false }
        doOnEnd { view.isEnabled = true }
    }

    companion object {
        private const val TAG = "LoadingButton"
    }
}