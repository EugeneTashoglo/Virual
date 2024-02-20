

package com.google.mediapipe.examples.poselandmarker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.mediapipe.examples.poselandmarker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация привязки к макету активности
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // Находим фрагмент-хост навигации
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment

        // Получаем контроллер навигации для фрагмента-хоста
        val navController = navHostFragment.navController

        // Настройка нижней навигации с использованием контроллера навигации
        activityMainBinding.navigation.setupWithNavController(navController)

        // Установка слушателя для игнорирования повторных выборов элементов навигации
        activityMainBinding.navigation.setOnNavigationItemReselectedListener {
            // игнорируем повторное нажатие
        }
    }

    override fun onBackPressed() {
        // Завершаем активность при нажатии кнопки назад
        finish()
    }
}