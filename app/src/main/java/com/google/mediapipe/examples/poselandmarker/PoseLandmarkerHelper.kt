
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(
    var minPoseDetectionConfidence: Float = DEFAULT_POSE_DETECTION_CONFIDENCE,
    var minPoseTrackingConfidence: Float = DEFAULT_POSE_TRACKING_CONFIDENCE,
    var minPosePresenceConfidence: Float = DEFAULT_POSE_PRESENCE_CONFIDENCE,
    var currentModel: Int = MODEL_POSE_LANDMARKER_FULL,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    // Этот слушатель используется только при выполнении в RunningMode.LIVE_STREAM
    val poseLandmarkerHelperListener: LandmarkerListener? = null
) {
    // Для этого примера это должно быть var, чтобы его можно было сбросить при изменениях.
    // Если Pose Landmarker не будет изменяться, лучше использовать ленивое val.
    private var poseLandmarker: PoseLandmarker? = null
    init {
        setupPoseLandmarker()
    }
    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
    // Возвращает состояние выполнения PoseLandmarkerHelper
    fun isClose(): Boolean {
        return poseLandmarker == null
    }
    // Инициализирует Pose Landmarker с использованием текущих настроек на
    // потоке, который его использует. CPU может использоваться с Landmarker,
    // которые созданы в основном потоке и используются в фоновом потоке, но
    // делегат GPU должен использоваться на потоке, который инициализировал
    // Landmarker
    fun setupPoseLandmarker() {
        // Установка общих параметров pose landmarker
        val baseOptionBuilder = BaseOptions.builder()

        // Использование указанного оборудования для запуска модели. По умолчанию на CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }
        val modelName =
            when (currentModel) {
                MODEL_POSE_LANDMARKER_FULL -> "pose_landmarker_full.task"
                MODEL_POSE_LANDMARKER_LITE -> "pose_landmarker_lite.task"
                MODEL_POSE_LANDMARKER_HEAVY -> "pose_landmarker_heavy.task"
                else -> "pose_landmarker_full.task"
            }
        baseOptionBuilder.setModelAssetPath(modelName)
        // Проверка согласованности runningMode с poseLandmarkerHelperListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (poseLandmarkerHelperListener == null) {
                    throw IllegalStateException(
                        "Для runningMode LIVE_STREAM необходимо установить poseLandmarkerHelperListener."
                    )
                }
            }
            else -> {
                // Нет операции
            }
        }
        try {
            val baseOptions = baseOptionBuilder.build()
            // Создание построителя параметров с базовыми параметрами и специфическими
            // параметрами, используемыми только для Pose Landmarker.
            val optionsBuilder =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                    .setMinTrackingConfidence(minPoseTrackingConfidence)
                    .setMinPosePresenceConfidence(minPosePresenceConfidence)
                    .setRunningMode(runningMode)
            // Слушатель результатов и ошибок используется только для режима LIVE_STREAM.
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }
            val options = optionsBuilder.build()
            poseLandmarker =
                PoseLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Не удалось инициализировать Pose Landmarker. См. журналы ошибок для подробностей."
            )
            Log.e(
                TAG, "MediaPipe не удалось загрузить задачу с ошибкой: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // Это происходит, если используемая модель не поддерживает GPU
            poseLandmarkerHelperListener?.onError(
                "Не удалось инициализировать Pose Landmarker. См. журналы ошибок для подробностей", GPU_ERROR
            )
            Log.e(
                TAG,
                "Ошибка загрузки модели классификатора изображений: " + e.message
            )
        }
    }
    // Преобразует ImageProxy в MP Image и передает его в PoselandmakerHelper.
    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Попытка вызвать detectLiveStream" +
                        " при использовании не RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        // Копирование RGB-битов из кадра в буфер битов
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Поворот кадра, полученного с камеры, чтобы он был в том же направлении, что и будет показан
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // перевернуть изображение, если пользователь использует переднюю камеру
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
        // Преобразование входного объекта Bitmap в объект MPImage для выполнения вывода
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        detectAsync(mpImage, frameTime)
    }
    // Выполнить определение позы с использованием API Pose Landmarker MediaPipe
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
        // Поскольку мы используем режим LIVE_STREAM, результаты маркировки
        // будут возвращены в функцию returnLivestreamResult
    }
    // Принимает URI для видеофайла, загруженного из галереи пользователя, и пытается выполнить
    // вывод маркировки позы на видео. В этом процессе будут оценены все
    // кадры в видео, и результаты будут прикреплены к пакету, который будет
    // возвращен.
    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): ResultBundle? {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Попытка вызвать detectVideoFile" +
                        " при использовании не RunningMode.VIDEO"
            )
        }
        // Время вывода - разница между системным временем в начале и конце
        // процесса
        val startTime = SystemClock.uptimeMillis()
        var didErrorOccurred = false
        // Загрузка кадров из видео и выполнение маркировки позы.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()
        // Примечание: необходимо читать ширину/высоту из кадра, а не получать ширину/высоту
        // видео напрямую, потому что MediaRetriever возвращает кадры, которые меньше
        // фактического размера файла видео.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height
        // Если видео недопустимо, возвращается пустой результат обнаружения
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null
        // Затем мы будем получать один кадр каждые inferenceIntervalMs мс,
        // а затем выполнять детекцию на этих кадрах.
        val resultList = mutableListOf<PoseLandmarkerResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)
        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // мс
            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // преобразование из мс в микрос
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Преобразование видеокадра в ARGB_8888, который требуется MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Преобразование входного объекта Bitmap в объект MPImage для выполнения вывода
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Выполнение маркировки позы с использованием API Pose Landmarker MediaPipe
                    poseLandmarker?.detectForVideo(mpImage, timestampMs)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        } ?: {
                        didErrorOccurred = true
                        poseLandmarkerHelperListener?.onError(
                            "ResultBundle не может быть возвращен" +
                                    " в detectVideoFile"
                        )
                    }
                }
                ?: run {
                    didErrorOccurred = true
                    poseLandmarkerHelperListener?.onError(
                        "Кадр в указанное время не мог быть" +
                                " получен при обнаружении в видео."
                    )
                }
        }
        retriever.release()
        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)
        return if (didErrorOccurred) {
            null
        } else {
            ResultBundle(resultList, inferenceTimePerFrameMs, height, width)
        }
    }
    // Принимает Bitmap и выполняет вывод маркировки позы на нем, чтобы вернуть
    // результаты вызывающей стороне
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Попытка вызвать detectImage" +
                        " при использовании не RunningMode.IMAGE"
            )
        }
        // Время вывода - разница между системным временем в
        // начале и конце процесса
        val startTime = SystemClock.uptimeMillis()
        // Преобразование входного объекта Bitmap в объект MPImage для выполнения вывода
        val mpImage = BitmapImageBuilder(image).build()
        // Выполнение маркировки позы с использованием API Pose Landmarker MediaPipe
        poseLandmarker?.detect(mpImage)?.also { landmarkResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(landmarkResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }
        // Если poseLandmarker?.detect() возвращает null, это вероятно ошибка. Возврат null
        // для указания этого.
        poseLandmarkerHelperListener?.onError(
            "Не удалось выполнить обнаружение Pose Landmarker."
        )
        return null
    }
    // Возвращает результаты маркировки вызывающей стороне этого PoseLandmarkerHelper
    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()
        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }
    // Возвращает ошибки, возникшие во время обнаружения, вызывающей стороне этого PoseLandmarkerHelper
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(
            error.message ?: "Произошла неизвестная ошибка"
        )
    }
    companion object {
        const val TAG = "PoseLandmarkerHelper"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_POSES = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        const val MODEL_POSE_LANDMARKER_FULL = 0
        const val MODEL_POSE_LANDMARKER_LITE = 1
        const val MODEL_POSE_LANDMARKER_HEAVY = 2
    }
    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
    
}