
package com.google.mediapipe.examples.poselandmarker.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.mediapipe.examples.poselandmarker.R

// Массив разрешений, необходимых для работы приложения
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PermissionsFragment : Fragment() {

    // Лаунчер для запроса разрешений
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Показываем сообщение, что разрешение получено
                Toast.makeText(
                    context,
                    "Permission request granted",
                    Toast.LENGTH_LONG
                ).show()
                navigateToCamera() // Переходим к камере, если разрешение получено
            } else {
                // Показываем сообщение, что разрешение не получено
                Toast.makeText(
                    context,
                    "Permission request denied",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Проверяем, есть ли разрешение на использование камеры
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) -> {
                navigateToCamera() // Если разрешение есть, переходим к камере
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                ) // Если разрешения нет, запрашиваем у пользователя
            }
        }
    }

    // Переход к фрагменту камеры
    private fun navigateToCamera() {
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(
                requireActivity(),
                R.id.fragment_container
            ).navigate(
                R.id.action_permissions_to_camera
            )
        }
    }

    companion object {

        /** Convenience method used to check if all permissions required by this app are granted */
        // Проверка, есть ли все необходимые разрешения
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}