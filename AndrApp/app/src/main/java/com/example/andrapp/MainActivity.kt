package com.example.andrapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private val locationPermissionCode = 100
    private var isTrackingLocation = false
    private lateinit var textLocation: TextView

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        textLocation = findViewById(R.id.text_location)


        val buttonMusicPlayer = findViewById<Button>(R.id.button_music_player)
        buttonMusicPlayer.setOnClickListener {
            val intent = Intent(this, Player::class.java)
            startActivity(intent)
        }

        val buttonLocation = findViewById<Button>(R.id.button_get_location)
        buttonLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        val buttonCalculator = findViewById<Button>(R.id.button_calculator)
        buttonCalculator.setOnClickListener {
            val intent = Intent(this, CalcActivity::class.java)
            startActivity(intent)
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy
                val coordinatesText = "Широта: ${"%.6f".format(latitude)}, Долгота: ${"%.6f".format(longitude)} (точность: ${"%.1f".format(accuracy)} м)"
                textLocation.text = coordinatesText
            }

            override fun onProviderEnabled(provider: String) {
                Toast.makeText(this@MainActivity, "GPS включен", Toast.LENGTH_SHORT).show()
            }

            override fun onProviderDisabled(provider: String) {
                Toast.makeText(this@MainActivity, "GPS выключен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            textLocation.text = "Определение местоположения..."

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                1f,
                locationListener
            )
            findViewById<Button>(R.id.button_get_location).text = "Остановить отслеживание"
            isTrackingLocation = true
            Toast.makeText(this, "Начато отслеживание координат", Toast.LENGTH_SHORT).show()


            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                locationListener.onLocationChanged(it)
            }
        } else {
            Toast.makeText(this, "GPS не доступен. Включите GPS.", Toast.LENGTH_LONG).show()
            textLocation.text = "GPS не доступен. Включите GPS."
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        Toast.makeText(this, "Отслеживание координат остановлено", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    this,
                    "Для получения координат необходимо разрешение",
                    Toast.LENGTH_SHORT
                ).show()
                textLocation.text = "Необходимо разрешение на доступ к местоположению"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTrackingLocation) {
            stopLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isTrackingLocation && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
}
