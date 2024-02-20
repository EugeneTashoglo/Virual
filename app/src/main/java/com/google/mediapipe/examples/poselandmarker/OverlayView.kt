
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

/**
 * Класс OverlayView представляет пользовательский вид, который используется для отображения результатов
 * обнаружения landmarks на изображении.
 */
class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    // Результаты обнаружения landmarks.
    private var results: PoseLandmarkerResult? = null
    // Кисть для рисования точек.
    private var pointPaint = Paint()
    // Кисть для рисования линий.
    private var linePaint = Paint()

    // Масштабный коэффициент для корректного отображения результатов на экране.
    private var scaleFactor: Float = 1f
    // Ширина изображения.
    private var imageWidth: Int = 1
    // Высота изображения.
    private var imageHeight: Int = 1

    init {
        // Инициализация кистей для рисования.
        initPaints()
    }

    // Метод для очистки результатов и переинициализации кистей.
    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    // Инициализация кистей для рисования.
    private fun initPaints() {
        // Установка цвета и толщины для линий между точками.
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        // Установка цвета и толщины для точек.
        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    // Переопределение метода отрисовки.
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            // Отрисовка точек и линий между ними для всех найденных landmarks.
            for(landmark in poseLandmarkerResult.landmarks()) {
                for(normalizedLandmark in landmark) {
                    // Рисование точки на экране.
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Отрисовка линий между определенными landmarks.
                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                        linePaint)
                }
            }
        }
    }

    /**
     * Метод для установки результатов обнаружения landmarks.
     * @param poseLandmarkerResults Результаты обнаружения landmarks.
     * @param imageHeight Высота изображения.
     * @param imageWidth Ширина изображения.
     * @param runningMode Режим работы (по умолчанию - режим работы с изображением).
     */
    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        // Установка результатов обнаружения landmarks.
        results = poseLandmarkerResults

        // Установка высоты и ширины изображения.
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        // Вычисление масштабного коэффициента для корректного отображения результатов на экране.
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                // Масштабирование результатов, если режим работы с изображением или видео.
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // В случае живого видеопотока необходимо увеличить масштаб, чтобы соответствовать размеру изображения на экране.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }

        // Перерисовка представления для отображения обновленных результатов.
        invalidate()
    }

    companion object {
        // Толщина линий для отображения landmarks.
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}
