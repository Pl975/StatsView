package ru.netology.statsview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.utils.AndroidUtils
import kotlin.math.min
import kotlin.random.Random


class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes,
) {

    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)

    private var lineWidth = AndroidUtils.dp(context, 5F).toFloat()
    private var fontSize = AndroidUtils.dp(context, 40F).toFloat()
    private var colors = emptyList<Int>()
    private var progress = 0F
    private val precision = 0.1F
    private val minimalArc = 0.0001F
    private var valueAnimator: ValueAnimator? = null

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)
            fontSize = getDimension(R.styleable.StatsView_fontSize, fontSize)
            val resId = getResourceId(R.styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()

        }
    }

    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth.toFloat()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND

    }

    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        textSize = fontSize
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER

    }

    var data: List<Float> = emptyList()
        set(value) {
            field = calcPartsOf(value)
            update()
        }

    private var unFilled: Float = 0F
    private var containsUnfilled = false
    private var dataSum = 0F

    private fun calcPartsOf(list: List<Float>): List<Float> {
        val listSum = list.sum()
        val sum = listSum + unFilled
        val result = list.toMutableList().map {
            it.div(sum)
        }
        dataSum = listSum.div(sum)
        return if (containsUnfilled) {
            result + unFilled.div(sum)
        } else {
            result
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth / 2
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius,

            )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }
        var startFrom = -90F
        data.forEachIndexed { index, datum ->
            val angle = 360F * datum

            if (index == data.size - 1 && containsUnfilled) {
                paint.color = ContextCompat.getColor(context, R.color.divider_color)
            } else {
                paint.color = colors.getOrNull(index) ?: randomColor()
            }

            canvas.drawArc(oval, startFrom + 360F * progress, angle * progress, false, paint)
            startFrom += angle
        }

        if (progress > (1F - precision)) {
            canvas.drawArc(
                oval,
                startFrom + 360F * progress,
                minimalArc,
                false,
                paint.apply { color = colors[0] })

        }

        canvas.drawText(
            "%.2f%%".format(dataSum * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint,
        )
    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = 5000
            interpolator = LinearInterpolator()

        }.also {
            it.start()

        }
    }

    private fun randomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

}