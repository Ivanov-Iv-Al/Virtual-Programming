package com.example.andrapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var result: TextView
    private lateinit var Vvod_1: EditText
    private lateinit var Vvod_2: EditText
    private lateinit var buttonAdd: Button
    private lateinit var buttonSubtract: Button
    private lateinit var buttonMultiply: Button
    private lateinit var buttonDivide: Button
    private lateinit var buttonGPS: Button
    private lateinit var gpsView: TextView
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                gpsView.text = "Широта: $latitude, Долгота: $longitude"

                // Записываем координаты в файл
                writeLocationToFile(latitude, longitude)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        result = findViewById(R.id.Text_result)
        Vvod_1 = findViewById(R.id.Vvod_1)
        Vvod_2 = findViewById(R.id.Vvod_2)
        buttonAdd = findViewById(R.id.button1)
        buttonSubtract = findViewById(R.id.button2)
        buttonMultiply = findViewById(R.id.button4)
        buttonDivide = findViewById(R.id.button3)
        buttonGPS = findViewById(R.id.GPS)
        gpsView = findViewById(R.id.GPS_View)

        buttonGPS.setOnClickListener { getLastLocation() }
    }

    override fun onResume() {
        super.onResume()
        buttonAdd.setOnClickListener { calculate(Operation.ADD) }
        buttonSubtract.setOnClickListener { calculate(Operation.SUBTRACT) }
        buttonMultiply.setOnClickListener { calculate(Operation.MULTIPLY) }
        buttonDivide.setOnClickListener { calculate(Operation.DIVIDE) }
    }

    private fun calculate(operation: Operation) {
        val num1 = Vvod_1.text.toString().toFloatOrNull() ?: 0f
        val num2 = Vvod_2.text.toString().toFloatOrNull() ?: 0f

        performOperation(num1, num2, operation, result)
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
            return
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper())

        val lastKnownLocation: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        lastKnownLocation?.let {
            gpsView.text = "Широта: ${it.latitude}, Долгота: ${it.longitude}"
        } ?: run {
            gpsView.text = "Нет доступного местоположения"
        }
    }

    private fun writeLocationToFile(latitude: Double, longitude: Double) {
        val file = File(getExternalFilesDir(null), "locations.txt")
        val outputStream: FileOutputStream
        try {
            outputStream = FileOutputStream(file, true)
            outputStream.write("Широта: $latitude, Долгота: $longitude\n".toByteArray())
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                gpsView.text = "Разрешение на местоположение отклонено"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }
}

