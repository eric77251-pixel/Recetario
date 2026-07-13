package com.example.recetario.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import kotlin.math.min

/**
 * Controla el zoom de una imagen sin usar librerías externas.
 *
 * Permite:
 * - doble toque para acercar/restaurar;
 * - gesto de pinza para acercar o alejar;
 * - arrastre cuando la imagen está ampliada;
 * - límites de escala y desplazamiento para evitar que la imagen se pierda fuera de pantalla.
 */
class ZoomListener(
    context: Context,
    private val escalaMaxima: Float = 4f
) : View.OnTouchListener {

    private val matrizBase = Matrix()
    private val matrizUsuario = Matrix()
    private val matrizFinal = Matrix()
    private val valores = FloatArray(9)

    private var inicializado = false
    private var ultimoX = 0f
    private var ultimoY = 0f
    private var arrastrando = false

    private val detectorEscala = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val escalaActual = obtenerEscalaActual()
                var factor = detector.scaleFactor

                val escalaNueva = escalaActual * factor
                if (escalaNueva < ESCALA_MINIMA) {
                    factor = ESCALA_MINIMA / escalaActual
                } else if (escalaNueva > escalaMaxima) {
                    factor = escalaMaxima / escalaActual
                }

                matrizUsuario.postScale(factor, factor, detector.focusX, detector.focusY)
                corregirLimites()
                aplicarMatriz()
                return true
            }
        }
    )

    private val detectorGestos = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(event: MotionEvent): Boolean {
                if (obtenerEscalaActual() > 1.05f) {
                    restaurarImagen()
                } else {
                    matrizUsuario.postScale(2f, 2f, event.x, event.y)
                    corregirLimites()
                    aplicarMatriz()
                }
                return true
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        val imageView = view as? ImageView ?: return false
        val motionEvent = event ?: return false

        if (!inicializado) {
            inicializarMatriz(imageView)
        }

        detectorGestos.onTouchEvent(motionEvent)
        detectorEscala.onTouchEvent(motionEvent)

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ultimoX = motionEvent.x
                ultimoY = motionEvent.y
                arrastrando = obtenerEscalaActual() > 1.05f
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (arrastrando && !detectorEscala.isInProgress && motionEvent.pointerCount == 1) {
                    val dx = motionEvent.x - ultimoX
                    val dy = motionEvent.y - ultimoY

                    matrizUsuario.postTranslate(dx, dy)
                    corregirLimites()
                    aplicarMatriz()

                    ultimoX = motionEvent.x
                    ultimoY = motionEvent.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                arrastrando = false
                view.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }

    private lateinit var imageView: ImageView

    private fun inicializarMatriz(imageView: ImageView) {
        this.imageView = imageView
        imageView.scaleType = ImageView.ScaleType.MATRIX

        val drawable = imageView.drawable ?: return
        val anchoVista = imageView.width.toFloat()
        val altoVista = imageView.height.toFloat()
        val anchoImagen = drawable.intrinsicWidth.toFloat()
        val altoImagen = drawable.intrinsicHeight.toFloat()

        if (anchoVista <= 0f || altoVista <= 0f || anchoImagen <= 0f || altoImagen <= 0f) return

        matrizBase.reset()
        matrizUsuario.reset()

        val escala = min(anchoVista / anchoImagen, altoVista / altoImagen)
        val dx = (anchoVista - anchoImagen * escala) / 2f
        val dy = (altoVista - altoImagen * escala) / 2f

        matrizBase.postScale(escala, escala)
        matrizBase.postTranslate(dx, dy)

        inicializado = true
        aplicarMatriz()
    }

    private fun aplicarMatriz() {
        if (!::imageView.isInitialized) return
        matrizFinal.set(matrizBase)
        matrizFinal.postConcat(matrizUsuario)
        imageView.imageMatrix = matrizFinal
    }

    private fun restaurarImagen() {
        matrizUsuario.reset()
        aplicarMatriz()
    }

    private fun obtenerEscalaActual(): Float {
        matrizUsuario.getValues(valores)
        val escala = valores[Matrix.MSCALE_X]
        return if (escala == 0f) 1f else escala
    }

    private fun obtenerRectanguloImagen(): RectF? {
        if (!::imageView.isInitialized) return null
        val drawable = imageView.drawable ?: return null
        val rect = RectF(
            0f,
            0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )

        matrizFinal.set(matrizBase)
        matrizFinal.postConcat(matrizUsuario)
        matrizFinal.mapRect(rect)
        return rect
    }

    private fun corregirLimites() {
        if (!::imageView.isInitialized) return
        val rect = obtenerRectanguloImagen() ?: return
        val anchoVista = imageView.width.toFloat()
        val altoVista = imageView.height.toFloat()

        var dx = 0f
        var dy = 0f

        dx = if (rect.width() <= anchoVista) {
            anchoVista / 2f - rect.centerX()
        } else {
            when {
                rect.left > 0f -> -rect.left
                rect.right < anchoVista -> anchoVista - rect.right
                else -> 0f
            }
        }

        dy = if (rect.height() <= altoVista) {
            altoVista / 2f - rect.centerY()
        } else {
            when {
                rect.top > 0f -> -rect.top
                rect.bottom < altoVista -> altoVista - rect.bottom
                else -> 0f
            }
        }

        matrizUsuario.postTranslate(dx, dy)
    }

    private companion object {
        const val ESCALA_MINIMA = 1f
    }
}