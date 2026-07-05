package com.example.recetario.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Vista sencilla para recortar una imagen en formato cuadrado 1:1.
 * Permite mover la foto con un dedo y hacer zoom con gesto de pinza.
 */
class SquareCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val cropRect = RectF()
    private val bitmapRect = RectF()

    private var scale = 1f
    private var minScale = 1f
    private var maxScale = 5f
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var needsReset = true

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val previousScale = scale
                scale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
                val factor = scale / previousScale

                offsetX = detector.focusX - (detector.focusX - offsetX) * factor
                offsetY = detector.focusY - (detector.focusY - offsetY) * factor

                limitarMovimiento()
                invalidate()
                return true
            }
        }
    )

    fun setImageBitmapForCrop(newBitmap: Bitmap) {
        bitmap = newBitmap
        needsReset = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calcularCropRect()
        needsReset = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentBitmap = bitmap ?: return
        if (needsReset) {
            reiniciarImagen(currentBitmap)
            needsReset = false
        }

        bitmapRect.set(
            offsetX,
            offsetY,
            offsetX + currentBitmap.width * scale,
            offsetY + currentBitmap.height * scale
        )

        canvas.drawBitmap(currentBitmap, null, bitmapRect, imagePaint)
        dibujarMascara(canvas)
        dibujarGuia(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                lastX = event.x
                lastY = event.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    offsetX += dx
                    offsetY += dy
                    limitarMovimiento()
                    invalidate()
                }
                lastX = event.x
                lastY = event.y
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }

        return true
    }

    fun obtenerBitmapRecortado(outputSize: Int = 900): Bitmap? {
        val currentBitmap = bitmap ?: return null
        if (width == 0 || height == 0 || cropRect.isEmpty) return null

        val left = ((cropRect.left - offsetX) / scale).coerceIn(0f, currentBitmap.width.toFloat())
        val top = ((cropRect.top - offsetY) / scale).coerceIn(0f, currentBitmap.height.toFloat())
        val right = ((cropRect.right - offsetX) / scale).coerceIn(0f, currentBitmap.width.toFloat())
        val bottom = ((cropRect.bottom - offsetY) / scale).coerceIn(0f, currentBitmap.height.toFloat())

        val availableWidth = (right - left).toInt().coerceAtLeast(1)
        val availableHeight = (bottom - top).toInt().coerceAtLeast(1)
        val squareSize = min(availableWidth, availableHeight).coerceAtLeast(1)

        val x = left.toInt().coerceIn(0, (currentBitmap.width - squareSize).coerceAtLeast(0))
        val y = top.toInt().coerceIn(0, (currentBitmap.height - squareSize).coerceAtLeast(0))

        val cropped = Bitmap.createBitmap(currentBitmap, x, y, squareSize, squareSize)
        return Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)
    }

    private fun calcularCropRect() {
        if (width == 0 || height == 0) return

        val horizontalMargin = width * 0.08f
        val verticalMargin = height * 0.08f
        val size = min(width - horizontalMargin * 2f, height - verticalMargin * 2f)
            .coerceAtLeast(1f)

        val left = (width - size) / 2f
        val top = (height - size) / 2f
        cropRect.set(left, top, left + size, top + size)
    }

    private fun reiniciarImagen(currentBitmap: Bitmap) {
        calcularCropRect()
        minScale = max(
            cropRect.width() / currentBitmap.width.toFloat(),
            cropRect.height() / currentBitmap.height.toFloat()
        )
        maxScale = minScale * 5f
        scale = minScale
        offsetX = cropRect.centerX() - currentBitmap.width * scale / 2f
        offsetY = cropRect.centerY() - currentBitmap.height * scale / 2f
        limitarMovimiento()
    }

    private fun limitarMovimiento() {
        val currentBitmap = bitmap ?: return
        val scaledWidth = currentBitmap.width * scale
        val scaledHeight = currentBitmap.height * scale

        offsetX = if (scaledWidth <= cropRect.width()) {
            cropRect.centerX() - scaledWidth / 2f
        } else {
            offsetX.coerceIn(cropRect.right - scaledWidth, cropRect.left)
        }

        offsetY = if (scaledHeight <= cropRect.height()) {
            cropRect.centerY() - scaledHeight / 2f
        } else {
            offsetY.coerceIn(cropRect.bottom - scaledHeight, cropRect.top)
        }
    }

    private fun dibujarMascara(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)
    }

    private fun dibujarGuia(canvas: Canvas) {
        canvas.drawRect(cropRect, borderPaint)

        val thirdWidth = cropRect.width() / 3f
        val thirdHeight = cropRect.height() / 3f

        canvas.drawLine(cropRect.left + thirdWidth, cropRect.top, cropRect.left + thirdWidth, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + thirdWidth * 2f, cropRect.top, cropRect.left + thirdWidth * 2f, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight, cropRect.right, cropRect.top + thirdHeight, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight * 2f, cropRect.right, cropRect.top + thirdHeight * 2f, gridPaint)
    }
}
