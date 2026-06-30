package com.example.recetario.Funciones

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import kotlin.math.sqrt

class ZoomListener(context: Context) : View.OnTouchListener {

    private val matrix = Matrix()
    private val matrixGuardada = Matrix()

    // Estados del gesto táctil
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var modo = NONE

    private val puntoInicio = PointF()
    private val puntoMedio = PointF()
    private var distanciaInicialDedo = 1f
    private var escalaActual = 1f
    private val escalaMax = 4f
    private val escalaMin = 1f

    // 1. Detector del Gesto de Pinza (Pinch Zoom)
    private val detectorEscala = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factorEscala = detector.scaleFactor
            val nuevaEscala = escalaActual * factorEscala

            if (nuevaEscala in escalaMin..escalaMax) {
                escalaActual = nuevaEscala
                matrix.postScale(factorEscala, factorEscala, detector.focusX, detector.focusY)
            }
            return true
        }
    })

    // 2. Detector del Doble Click (Double Tap)
    private val detectorGestosEspeciales = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (escalaActual > 1f) {
                matrix.reset()
                escalaActual = 1f
                modo = NONE
            } else {
                matrix.postScale(2f, 2f, e.x, e.y)
                escalaActual = 2f
                modo = DRAG
            }
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val imageView = v as? ImageView ?: return false
        imageView.scaleType = ImageView.ScaleType.MATRIX

        event?.let {
            detectorGestosEspeciales.onTouchEvent(it)
            detectorEscala.onTouchEvent(it)

            val puntoActual = PointF(it.x, it.y)

            when (it.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    matrixGuardada.set(matrix)
                    puntoInicio.set(it.x, it.y)
                    if (escalaActual > 1f) modo = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    distanciaInicialDedo = calcularDistanciaEntreDedos(it)
                    if (distanciaInicialDedo > 10f) {
                        matrixGuardada.set(matrix)
                        calcularPuntoMedio(puntoMedio, it)
                        modo = ZOOM
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    modo = NONE
                }
                MotionEvent.ACTION_MOVE -> {
                    if (modo == DRAG && escalaActual > 1f) {
                        matrix.set(matrixGuardada)
                        matrix.postTranslate(puntoActual.x - puntoInicio.x, puntoActual.y - puntoInicio.y)
                    }
                }
            }
            imageView.imageMatrix = matrix
        }
        return true
    }

    private fun calcularDistanciaEntreDedos(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun calcularPuntoMedio(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
}