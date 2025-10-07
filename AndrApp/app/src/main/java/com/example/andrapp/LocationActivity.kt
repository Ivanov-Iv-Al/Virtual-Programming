package com.example.andrapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
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
import com.google.gson.JsonParser
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
    private val jsonParser = JsonParser()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val coordinateFormat = "%.6f"
    private val altitudeAccuracyFormat = "%.1f"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val LOCATION_UPDATE_INTERVAL = 1000L
        private const val JSON_FILE_NAME = "location_history.json"
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
            fastestInterval = LOCATION_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    updateLocationUI(location)
                    saveLocationData(location)
                }
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        tvLatitude.text = "Широта: ${coordinateFormat.format(location.latitude)}°"
        tvLongitude.text = "Долгота: ${coordinateFormat.format(location.longitude)}°"
        tvAltitude.text = "Высота: ${altitudeAccuracyFormat.format(location.altitude)} м"
        tvAccuracy.text = "Точность: ${altitudeAccuracyFormat.format(location.accuracy)} м"
        tvTime.text = "Обновлено: ${dateFormat.format(Date(location.time))}"
    }

    private fun saveLocationData(location: Location) {
        val locationData = mapOf(
            "latitude" to coordinateFormat.format(location.latitude),
            "longitude" to coordinateFormat.format(location.longitude),
            "altitude" to altitudeAccuracyFormat.format(location.altitude),
            "accuracy" to altitudeAccuracyFormat.format(location.accuracy),
            "timestamp" to location.time,
            "time_formatted" to dateFormat.format(Date(location.time)),
            "provider" to location.provider
        )

        try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), JSON_FILE_NAME)

            val existingData = if (file.exists()) {
                val fileContent = file.readText()
                if (fileContent.isNotEmpty()) {
                    gson.fromJson(fileContent, MutableList::class.java) as? MutableList<Map<String, *>> ?: mutableListOf()
                } else {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            existingData.add(locationData)

            FileWriter(file, false).use { writer ->
                writer.write(gson.toJson(existingData))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка сохранения локации", Toast.LENGTH_SHORT).show()
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
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
