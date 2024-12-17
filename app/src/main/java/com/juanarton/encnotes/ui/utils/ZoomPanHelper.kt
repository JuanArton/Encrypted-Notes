package com.juanarton.encnotes.ui.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

@SuppressLint("ClickableViewAccessibility")
class ZoomPanHelper(private val imageView: ImageView) {
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private var mode = NONE

    private var scale = 1f
    private val minScale = 1f
    private val maxScale = 4f
    private var isZoomedIn = false

    private val scaleGestureDetector = ScaleGestureDetector(imageView.context, ScaleListener())
    private val gestureDetector = GestureDetector(imageView.context, GestureListener())

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.viewTreeObserver.addOnGlobalLayoutListener {
            initializeImage()
        }

        imageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    startPoint.set(event.x, event.y)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    savedMatrix.set(matrix)
                    mode = ZOOM
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        if (isZoomedIn) {
                            val dx = event.x - startPoint.x
                            val dy = event.y - startPoint.y
                            matrix.set(savedMatrix)
                            matrix.postTranslate(dx, dy)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                }
            }

            imageView.imageMatrix = matrix
            true
        }
    }

    private fun initializeImage() {
        val drawable = imageView.drawable ?: return
        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        scale = minOf(scaleX, scaleY)

        val dx = (viewWidth - imageWidth * scale) / 2
        val dy = (viewHeight - imageHeight * scale) / 2

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        imageView.imageMatrix = matrix
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var originalScale = 0F

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!isZoomedIn) originalScale = detector.scaleFactor

            var scaleFactor = detector.scaleFactor
            val prevScale = scale
            scale *= scaleFactor

            scale = scale.coerceIn(originalScale, maxScale)
            scaleFactor = scale / prevScale

            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            imageView.imageMatrix = matrix

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            if (scale in originalScale .. originalScale + 0.5F && isZoomedIn) {
                initializeImage()
                isZoomedIn = false
            } else {
                isZoomedIn = true
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private var originalScale = 0F

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (scale < maxScale) 2F else minScale
            val startScale = scale
            val scaleFactor = targetScale / scale

            isZoomedIn = if (scaleFactor > 1) {
                originalScale = startScale
                ValueAnimator.ofFloat(startScale, targetScale).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        val animatedValue = animator.animatedValue as Float
                        val animatedScaleFactor = animatedValue / scale
                        matrix.postScale(animatedScaleFactor, animatedScaleFactor, e.x, e.y)
                        scale = animatedValue
                        imageView.imageMatrix = matrix
                    }
                    start()
                }
                true
            } else {
                ValueAnimator.ofFloat(targetScale, originalScale).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        val animatedValue = animator.animatedValue as Float
                        val animatedScaleFactor = animatedValue / scale
                        matrix.postScale(animatedScaleFactor, animatedScaleFactor, e.x, e.y)
                        scale = animatedValue
                        imageView.imageMatrix = matrix
                    }
                    start()
                    initializeImage()
                }
                false
            }

            return true
        }
    }


    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}

