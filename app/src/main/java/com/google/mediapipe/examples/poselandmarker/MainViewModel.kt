
package com.google.mediapipe.examples.poselandmarker

import androidx.lifecycle.ViewModel

/**
 * Этот ViewModel используется для хранения настроек помощника по обнаружению позы.
 */
class MainViewModel : ViewModel() {

    // Модель используемого алгоритма обнаружения позы
    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL

    // Делегат для выполнения операций по обнаружению позы
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU

    // Минимальное доверие обнаружения позы
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE

    // Минимальное доверие трекинга позы
    private var _minPoseTrackingConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE

    // Минимальное доверие наличия позы
    private var _minPosePresenceConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE

    // Получение текущего делегата
    val currentDelegate: Int get() = _delegate

    // Получение текущей модели
    val currentModel: Int get() = _model

    // Получение текущего минимального доверия обнаружения позы
    val currentMinPoseDetectionConfidence: Float
        get() = _minPoseDetectionConfidence

    // Получение текущего минимального доверия трекинга позы
    val currentMinPoseTrackingConfidence: Float
        get() = _minPoseTrackingConfidence

    // Получение текущего минимального доверия наличия позы
    val currentMinPosePresenceConfidence: Float
        get() = _minPosePresenceConfidence

    // Установка делегата
    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    // Установка минимального доверия обнаружения позы
    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    // Установка минимального доверия трекинга позы
    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    // Установка минимального доверия наличия позы
    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }

    // Установка модели
    fun setModel(model: Int) {
        _model = model
    }
}