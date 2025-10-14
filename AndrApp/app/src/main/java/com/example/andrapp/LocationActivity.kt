package com.example.andrapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvTime: TextView

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val coordinateFormat = "%.6f"
    private val altitudeAccuracyFormat = "%.1f"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val LOCATION_UPDATE_INTERVAL = 5000L // Увеличим интервал для тестирования
        private const val JSON_FILE_NAME = "location_history.json"
        private const val TAG = "LocationActivity"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        initViews()
        setupLocationClient()
        setupLocationRequest()
        setupLocationCallback()

        if (checkLocationPermission()) {
            startLocationUpdates()
            Toast.makeText(this, "Поиск локации...", Toast.LENGTH_SHORT).show()
        } else {
            requestLocationPermission()
        }
    }

    private fun initViews() {
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        tvTime = findViewById(R.id.tvTime)

        tvLatitude.text = "Широта: ---"
        tvLongitude.text = "Долгота: ---"
        tvAltitude.text = "Высота: --- м"
        tvAccuracy.text = "Точность: --- м"
        tvTime.text = "Время: ---"
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_INTERVAL / 2
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1.0f // Минимальное перемещение в метрах для обновления
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Получена новая локация: ${location.latitude}, ${location.longitude}")
                    updateLocationUI(location)
                    saveLocationData(location)
                }
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            tvLatitude.text = "Широта: ${coordinateFormat.format(location.latitude)}°"
            tvLongitude.text = "Долгота: ${coordinateFormat.format(location.longitude)}°"
            tvAltitude.text = "Высота: ${altitudeAccuracyFormat.format(location.altitude)} м"
            tvAccuracy.text = "Точность: ${altitudeAccuracyFormat.format(location.accuracy)} м"
            tvTime.text = "Обновлено: ${dateFormat.format(Date(location.time))}"
        }
    }

    private fun saveLocationData(location: Location) {
        try {
            // Создаем данные с явным приведением типов
            val locationData = mapOf(
                "latitude" to coordinateFormat.format(location.latitude),
                "longitude" to coordinateFormat.format(location.longitude),
                "altitude" to altitudeAccuracyFormat.format(location.altitude),
                "accuracy" to altitudeAccuracyFormat.format(location.accuracy),
                "timestamp" to location.time,
                "time_formatted" to dateFormat.format(Date(location.time)),
                "provider" to location.provider
            )

            Log.d(TAG, "Сохранение локации: $locationData")

            val documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir == null) {
                Log.e(TAG, "Не удалось получить директорию Documents")
                return
            }

            val file = File(documentsDir, JSON_FILE_NAME)
            Log.d(TAG, "Путь к файлу: ${file.absolutePath}")

            // Создаем директорию если не существует
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // Решение 1: Используем безопасное приведение типов
            val allLocations = mutableListOf<Map<String, Any>>()

            // Читаем существующие данные если файл есть
            if (file.exists() && file.length() > 0) {
                try {
                    val fileContent = file.readText()
                    Log.d(TAG, "Содержимое файла: $fileContent")

                    // Безопасное чтение с обработкой типа
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val existingData: List<Map<String, Any>>? = gson.fromJson(fileContent, type)

                    if (existingData != null) {
                        allLocations.addAll(existingData)
                        Log.d(TAG, "Загружено ${existingData.size} существующих записей")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка чтения существующих данных, создаем новый файл", e)
                }
            }

            // Решение 2: Явное приведение типа с проверкой
            @Suppress("UNCHECKED_CAST")
            val safeLocationData = locationData as Map<String, Any>
            allLocations.add(safeLocationData)

            Log.d(TAG, "Всего записей: ${allLocations.size}")

            // Сохраняем все данные обратно
            FileWriter(file, false).use { writer ->
                val jsonString = gson.toJson(allLocations)
                writer.write(jsonString)
                Log.d(TAG, "Данные успешно сохранены")
            }

            runOnUiThread {
                Toast.makeText(this, "Локация сохранена!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения локации", e)
            runOnUiThread {
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска обновлений локации", e)
            Toast.makeText(this, "Ошибка получения локации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение на локацию отклонено", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            startLocationUpdates()
        }
    }
}