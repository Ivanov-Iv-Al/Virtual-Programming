package com.example.andrapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.telephony.*
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import androidx.annotation.RequiresApi

class LocationActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvTime: TextView

    private lateinit var tvNetworkType: TextView
    private lateinit var tvSignalLevel: TextView

    private lateinit var tvWifiSSID: TextView
    private lateinit var tvWifiBSSID: TextView
    private lateinit var tvWifiRSSI: TextView
    private lateinit var tvWifiFrequency: TextView

    private lateinit var tvLTE_MCC: TextView
    private lateinit var tvLTE_MNC: TextView
    private lateinit var tvLTE_TAC: TextView
    private lateinit var tvLTE_PCI: TextView
    private lateinit var tvLTE_EARFCN: TextView
    private lateinit var tvLTE_RSRP: TextView
    private lateinit var tvLTE_RSRQ: TextView
    private lateinit var tvLTE_RSSNR: TextView

    private lateinit var tvGSM_MCC: TextView
    private lateinit var tvGSM_MNC: TextView
    private lateinit var tvGSM_LAC: TextView
    private lateinit var tvGSM_CID: TextView
    private lateinit var tvGSM_BSIC: TextView
    private lateinit var tvGSM_ARFCN: TextView
    private lateinit var tvGSM_RSSI: TextView

    private lateinit var tvNR_MCC: TextView
    private lateinit var tvNR_MNC: TextView
    private lateinit var tvNR_TAC: TextView
    private lateinit var tvNR_NCI: TextView
    private lateinit var tvNR_PCI: TextView
    private lateinit var tvNR_SS_RSRP: TextView
    private lateinit var tvNR_SS_RSRQ: TextView
    private lateinit var tvNR_SS_SINR: TextView

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val coordinateFormat = "%.6f"
    private val altitudeAccuracyFormat = "%.1f"

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val LOCATION_UPDATE_INTERVAL = 5000L
        private const val JSON_FILE_NAME = "location_history.json"
        private const val CELL_INFO_FILE_NAME = "cell_info.json"
        private const val TAG = "LocationActivity"

        val signalLevelMap = mapOf(
            0 to "None",
            1 to "Poor",
            2 to "Moderate",
            3 to "Good",
            4 to "Great"
        )

        val networkTypeMap = mapOf(
            TelephonyManager.NETWORK_TYPE_UNKNOWN to "Unknown",
            TelephonyManager.NETWORK_TYPE_GPRS to "GPRS",
            TelephonyManager.NETWORK_TYPE_EDGE to "EDGE",
            TelephonyManager.NETWORK_TYPE_UMTS to "UMTS",
            TelephonyManager.NETWORK_TYPE_HSDPA to "HSDPA",
            TelephonyManager.NETWORK_TYPE_HSUPA to "HSUPA",
            TelephonyManager.NETWORK_TYPE_HSPA to "HSPA",
            TelephonyManager.NETWORK_TYPE_CDMA to "CDMA",
            TelephonyManager.NETWORK_TYPE_EVDO_0 to "EVDO_0",
            TelephonyManager.NETWORK_TYPE_EVDO_A to "EVDO_A",
            TelephonyManager.NETWORK_TYPE_1xRTT to "1xRTT",
            TelephonyManager.NETWORK_TYPE_LTE to "LTE",
            TelephonyManager.NETWORK_TYPE_NR to "5G NR"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        initViews()
        setupLocationClient()
        setupLocationRequest()
        setupLocationCallback()

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (checkAllPermissions()) {
            startLocationUpdates()
            Toast.makeText(this, "Поиск локации...", Toast.LENGTH_SHORT).show()

            updateNetworkInfo()
        } else {
            requestAllPermissions()
        }
    }

    private fun initViews() {
        // Location Views
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvTime = findViewById(R.id.tvTime)

        // Network Views
        tvNetworkType = findViewById(R.id.tvNetworkType)
        tvSignalLevel = findViewById(R.id.tvSignalLevel)

        // Wi-Fi Views
        tvWifiSSID = findViewById(R.id.tvWifiSSID)
        tvWifiBSSID = findViewById(R.id.tvWifiBSSID)
        tvWifiRSSI = findViewById(R.id.tvWifiRSSI)
        tvWifiFrequency = findViewById(R.id.tvWifiFrequency)

        // LTE Views
        tvLTE_MCC = findViewById(R.id.tvLTE_MCC)
        tvLTE_MNC = findViewById(R.id.tvLTE_MNC)
        tvLTE_TAC = findViewById(R.id.tvLTE_TAC)
        tvLTE_PCI = findViewById(R.id.tvLTE_PCI)
        tvLTE_EARFCN = findViewById(R.id.tvLTE_EARFCN)
        tvLTE_RSRP = findViewById(R.id.tvLTE_RSRP)
        tvLTE_RSRQ = findViewById(R.id.tvLTE_RSRQ)
        tvLTE_RSSNR = findViewById(R.id.tvLTE_RSSNR)

        // GSM Views
        tvGSM_MCC = findViewById(R.id.tvGSM_MCC)
        tvGSM_MNC = findViewById(R.id.tvGSM_MNC)
        tvGSM_LAC = findViewById(R.id.tvGSM_LAC)
        tvGSM_CID = findViewById(R.id.tvGSM_CID)
        tvGSM_BSIC = findViewById(R.id.tvGSM_BSIC)
        tvGSM_ARFCN = findViewById(R.id.tvGSM_ARFCN)
        tvGSM_RSSI = findViewById(R.id.tvGSM_RSSI)

        // 5G NR Views
        tvNR_MCC = findViewById(R.id.tvNR_MCC)
        tvNR_MNC = findViewById(R.id.tvNR_MNC)
        tvNR_TAC = findViewById(R.id.tvNR_TAC)
        tvNR_NCI = findViewById(R.id.tvNR_NCI)
        tvNR_PCI = findViewById(R.id.tvNR_PCI)
        tvNR_SS_RSRP = findViewById(R.id.tvNR_SS_RSRP)
        tvNR_SS_RSRQ = findViewById(R.id.tvNR_SS_RSRQ)
        tvNR_SS_SINR = findViewById(R.id.tvNR_SS_SINR)
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_INTERVAL / 2
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1.0f
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
                    updateNetworkInfo()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        processAllCellInfo(location)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            tvLatitude.text = "Широта: ${coordinateFormat.format(location.latitude)}°"
            tvLongitude.text = "Долгота: ${coordinateFormat.format(location.longitude)}°"
            tvAltitude.text = "Высота: ${altitudeAccuracyFormat.format(location.altitude)} м"
            tvAccuracy.text = "Точность: ${altitudeAccuracyFormat.format(location.accuracy)} м"
            tvSpeed.text = "Скорость: ${String.format("%.2f", location.speed)} м/с"
            tvTime.text = "Время: ${dateFormat.format(Date(location.time))}"
        }
    }

    private fun updateNetworkInfo() {
        runOnUiThread {
            try {
                // Проверяем разрешения перед получением информации о сети
                if (!checkAllPermissions()) {
                    tvNetworkType.text = "Тип сети: Разрешения не предоставлены"
                    tvSignalLevel.text = "Уровень сигнала: -"
                    return@runOnUiThread
                }

                // Определяем тип сети
                val networkType = getCurrentNetworkType()
                tvNetworkType.text = "Тип сети: $networkType"

                // Обновляем информацию в зависимости от типа сети
                when {
                    networkType.contains("Wi-Fi", ignoreCase = true) -> {
                        updateWifiInfo()
                        clearCellularInfo()
                    }
                    networkType.contains("LTE", ignoreCase = true) -> {
                        updateCellularInfo()
                        clearWifiInfo()
                    }
                    networkType.contains("5G", ignoreCase = true) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            update5GInfo()
                        } else {
                            updateCellularInfo() // Fallback для старых версий
                        }
                        clearWifiInfo()
                    }
                    networkType.contains("GSM", ignoreCase = true) ||
                            networkType.contains("GPRS", ignoreCase = true) ||
                            networkType.contains("EDGE", ignoreCase = true) ||
                            networkType.contains("UMTS", ignoreCase = true) ||
                            networkType.contains("HSPA", ignoreCase = true) ||
                            networkType.contains("3G", ignoreCase = true) -> {
                        updateGSMInfo()
                        clearWifiInfo()
                    }
                    else -> {
                        clearAllNetworkInfo()
                    }
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException при обновлении информации о сети", e)
                tvNetworkType.text = "Тип сети: Ошибка безопасности"
                tvSignalLevel.text = "Уровень сигнала: -"
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления информации о сети", e)
                tvNetworkType.text = "Тип сети: Ошибка"
                tvSignalLevel.text = "Уровень сигнала: -"
            }
        }
    }

    private fun getCurrentNetworkType(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                if (network == null) return "No Network"

                val capabilities = connectivityManager.getNetworkCapabilities(network)

                when {
                    capabilities == null -> "No Network"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        "Wi-Fi"
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        // Проверяем разрешение перед доступом к данным телефона
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val mobileNetworkType = telephonyManager.dataNetworkType
                            networkTypeMap[mobileNetworkType] ?: "Cellular ($mobileNetworkType)"
                        } else {
                            "Cellular (Permission Required)"
                        }
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                // Для старых версий Android
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                    ConnectivityManager.TYPE_MOBILE -> {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val mobileNetworkType = telephonyManager.dataNetworkType
                            networkTypeMap[mobileNetworkType] ?: "Cellular"
                        } else {
                            "Cellular"
                        }
                    }
                    else -> networkInfo?.typeName ?: "Unknown"
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при определении типа сети", e)
            "Permission Error"
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка определения типа сети", e)
            "Error"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWifiInfo() {
        try {
            // Проверяем разрешение для Wi-Fi
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_WIFI_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                tvWifiSSID.text = "Wi-Fi SSID: Permission Required"
                tvWifiBSSID.text = "Wi-Fi BSSID: Permission Required"
                tvWifiRSSI.text = "Wi-Fi RSSI: Permission Required"
                tvWifiFrequency.text = "Wi-Fi Frequency: Permission Required"
                tvSignalLevel.text = "Уровень сигнала: Permission Required"
                return
            }

            val wifiInfo = wifiManager.connectionInfo

            // Получаем SSID
            val ssid = if (wifiInfo.ssid.isNullOrEmpty() || wifiInfo.ssid == "<unknown ssid>") {
                "Hidden Network"
            } else {
                wifiInfo.ssid.removeSurrounding("\"")
            }

            // Получаем BSSID
            val bssid = wifiInfo.bssid ?: "Unknown"

            // Получаем RSSI
            val rssi = wifiInfo.rssi

            // Получаем частоту
            val frequency = wifiInfo.frequency

            // Обновляем UI
            tvWifiSSID.text = "Wi-Fi SSID: $ssid"
            tvWifiBSSID.text = "Wi-Fi BSSID: $bssid"
            tvWifiRSSI.text = "Wi-Fi RSSI: $rssi dBm"
            tvWifiFrequency.text = "Wi-Fi Frequency: $frequency MHz"

            // Определяем уровень сигнала Wi-Fi
            val level = WifiManager.calculateSignalLevel(rssi, 5)
            val wifiSignalLevel = when (level) {
                0 -> "None"
                1 -> "Poor"
                2 -> "Moderate"
                3 -> "Good"
                4 -> "Great"
                else -> "Unknown"
            }

            tvSignalLevel.text = "Уровень сигнала: $wifiSignalLevel"

            // Устанавливаем цвет
            val color = when (wifiSignalLevel) {
                "Great" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                "Good" -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                "Moderate" -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                "Poor" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(this, android.R.color.black)
            }
            tvSignalLevel.setTextColor(color)

            Log.d(TAG, "Wi-Fi Info: SSID=$ssid, BSSID=$bssid, RSSI=$rssi, Freq=$frequency")

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при получении информации о Wi-Fi", e)
            tvWifiSSID.text = "Wi-Fi SSID: Security Error"
            tvWifiBSSID.text = "Wi-Fi BSSID: Security Error"
            tvWifiRSSI.text = "Wi-Fi RSSI: Security Error"
            tvWifiFrequency.text = "Wi-Fi Frequency: Security Error"
            tvSignalLevel.text = "Уровень сигнала: Security Error"
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения информации о Wi-Fi", e)
            tvWifiSSID.text = "Wi-Fi SSID: Error"
            tvWifiBSSID.text = "Wi-Fi BSSID: Error"
            tvWifiRSSI.text = "Wi-Fi RSSI: Error"
            tvWifiFrequency.text = "Wi-Fi Frequency: Error"
            tvSignalLevel.text = "Уровень сигнала: Error"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCellularInfo() {
        try {
            // Проверяем разрешение для данных телефона
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                tvSignalLevel.text = "Уровень сигнала: Permission Required"
                clearLTEInfo()
                return
            }

            // Получаем информацию о сигнале
            val signalStrength = telephonyManager.signalStrength
            val level = signalStrength?.level ?: 0
            val signalLevelStr = signalLevelMap[level] ?: "Unknown ($level)"

            tvSignalLevel.text = "Уровень сигнала: $signalLevelStr"

            // Устанавливаем цвет
            val color = when (signalLevelStr) {
                "Great" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                "Good" -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                "Moderate" -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                "Poor" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(this, android.R.color.black)
            }
            tvSignalLevel.setTextColor(color)

            // Получаем информацию о ячейках LTE (требует API 17+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val allCells: List<CellInfo>? = telephonyManager.allCellInfo
                    val lteCell = allCells?.filterIsInstance<CellInfoLte>()?.firstOrNull()

                    if (lteCell != null) {
                        val lteIdentity = lteCell.cellIdentity
                        val lteSignal = lteCell.cellSignalStrength

                        // Используем рефлексию для совместимости с разными версиями API
                        try {
                            val mccField = lteIdentity.javaClass.getDeclaredField("mccString")
                            mccField.isAccessible = true
                            val mcc = mccField.get(lteIdentity) as? String
                            mcc?.let { tvLTE_MCC.text = "MCC: $it" } ?: run { tvLTE_MCC.text = "MCC: -" }
                        } catch (e: Exception) {
                            tvLTE_MCC.text = "MCC: -"
                        }

                        try {
                            val mncField = lteIdentity.javaClass.getDeclaredField("mncString")
                            mncField.isAccessible = true
                            val mnc = mncField.get(lteIdentity) as? String
                            mnc?.let { tvLTE_MNC.text = "MNC: $it" } ?: run { tvLTE_MNC.text = "MNC: -" }
                        } catch (e: Exception) {
                            tvLTE_MNC.text = "MNC: -"
                        }

                        // PCI, TAC, EARFCN
                        try {
                            val pciField = lteIdentity.javaClass.getDeclaredField("pci")
                            pciField.isAccessible = true
                            val pci = pciField.get(lteIdentity) as? Int
                            pci?.let { tvLTE_PCI.text = "PCI: $it" } ?: run { tvLTE_PCI.text = "PCI: -" }
                        } catch (e: Exception) {
                            tvLTE_PCI.text = "PCI: -"
                        }

                        try {
                            val tacField = lteIdentity.javaClass.getDeclaredField("tac")
                            tacField.isAccessible = true
                            val tac = tacField.get(lteIdentity) as? Int
                            tac?.let { tvLTE_TAC.text = "TAC: $it" } ?: run { tvLTE_TAC.text = "TAC: -" }
                        } catch (e: Exception) {
                            tvLTE_TAC.text = "TAC: -"
                        }

                        try {
                            val earfcnField = lteIdentity.javaClass.getDeclaredField("earfcn")
                            earfcnField.isAccessible = true
                            val earfcn = earfcnField.get(lteIdentity) as? Int
                            earfcn?.let { tvLTE_EARFCN.text = "EARFCN: $it" } ?: run { tvLTE_EARFCN.text = "EARFCN: -" }
                        } catch (e: Exception) {
                            tvLTE_EARFCN.text = "EARFCN: -"
                        }

                        // Сигнальные параметры
                        try {
                            val rsrpField = lteSignal.javaClass.getDeclaredField("rsrp")
                            rsrpField.isAccessible = true
                            val rsrp = rsrpField.get(lteSignal) as? Int
                            rsrp?.let { tvLTE_RSRP.text = "RSRP: $it dBm" } ?: run { tvLTE_RSRP.text = "RSRP: - dBm" }
                        } catch (e: Exception) {
                            tvLTE_RSRP.text = "RSRP: - dBm"
                        }

                        try {
                            val rsrqField = lteSignal.javaClass.getDeclaredField("rsrq")
                            rsrqField.isAccessible = true
                            val rsrq = rsrqField.get(lteSignal) as? Int
                            rsrq?.let { tvLTE_RSRQ.text = "RSRQ: $it dB" } ?: run { tvLTE_RSRQ.text = "RSRQ: - dB" }
                        } catch (e: Exception) {
                            tvLTE_RSRQ.text = "RSRQ: - dB"
                        }

                        try {
                            val rssnrField = lteSignal.javaClass.getDeclaredField("rssnr")
                            rssnrField.isAccessible = true
                            val rssnr = rssnrField.get(lteSignal) as? Int
                            rssnr?.let { tvLTE_RSSNR.text = "RSSNR: $it dB" } ?: run { tvLTE_RSSNR.text = "RSSNR: - dB" }
                        } catch (e: Exception) {
                            tvLTE_RSSNR.text = "RSSNR: - dB"
                        }

                        Log.d(TAG, "LTE Info получена")
                    } else {
                        clearLTEInfo()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка получения LTE информации", e)
                    clearLTEInfo()
                }
            } else {
                clearLTEInfo()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при получении информации о LTE", e)
            tvSignalLevel.text = "Уровень сигнала: Security Error"
            clearLTEInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения информации о LTE", e)
            tvSignalLevel.text = "Уровень сигнала: Error"
            clearLTEInfo()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateGSMInfo() {
        try {
            // Проверяем разрешение
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                tvSignalLevel.text = "Уровень сигнала: Permission Required"
                clearGSMInfo()
                return
            }

            // Получаем информацию о сигнале
            val signalStrength = telephonyManager.signalStrength
            val level = signalStrength?.level ?: 0
            val signalLevelStr = signalLevelMap[level] ?: "Unknown ($level)"

            tvSignalLevel.text = "Уровень сигнала: $signalLevelStr"

            // Устанавливаем цвет
            val color = when (signalLevelStr) {
                "Great" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                "Good" -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                "Moderate" -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                "Poor" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(this, android.R.color.black)
            }
            tvSignalLevel.setTextColor(color)

            // GSM информация (требует API 17+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val allCells: List<CellInfo>? = telephonyManager.allCellInfo
                    val gsmCell = allCells?.filterIsInstance<CellInfoGsm>()?.firstOrNull()

                    if (gsmCell != null) {
                        val gsmIdentity = gsmCell.cellIdentity
                        val gsmSignal = gsmCell.cellSignalStrength

                        // Используем рефлексию для получения полей
                        try {
                            val mccField = gsmIdentity.javaClass.getDeclaredField("mccString")
                            mccField.isAccessible = true
                            val mcc = mccField.get(gsmIdentity) as? String
                            mcc?.let { tvGSM_MCC.text = "MCC: $it" } ?: run { tvGSM_MCC.text = "MCC: -" }
                        } catch (e: Exception) {
                            tvGSM_MCC.text = "MCC: -"
                        }

                        try {
                            val mncField = gsmIdentity.javaClass.getDeclaredField("mncString")
                            mncField.isAccessible = true
                            val mnc = mncField.get(gsmIdentity) as? String
                            mnc?.let { tvGSM_MNC.text = "MNC: $it" } ?: run { tvGSM_MNC.text = "MNC: -" }
                        } catch (e: Exception) {
                            tvGSM_MNC.text = "MNC: -"
                        }

                        try {
                            val lacField = gsmIdentity.javaClass.getDeclaredField("lac")
                            lacField.isAccessible = true
                            val lac = lacField.get(gsmIdentity) as? Int
                            lac?.let { tvGSM_LAC.text = "LAC: $it" } ?: run { tvGSM_LAC.text = "LAC: -" }
                        } catch (e: Exception) {
                            tvGSM_LAC.text = "LAC: -"
                        }

                        try {
                            val cidField = gsmIdentity.javaClass.getDeclaredField("cid")
                            cidField.isAccessible = true
                            val cid = cidField.get(gsmIdentity) as? Int
                            cid?.let { tvGSM_CID.text = "CID: $it" } ?: run { tvGSM_CID.text = "CID: -" }
                        } catch (e: Exception) {
                            tvGSM_CID.text = "CID: -"
                        }

                        try {
                            val bsicField = gsmIdentity.javaClass.getDeclaredField("bsic")
                            bsicField.isAccessible = true
                            val bsic = bsicField.get(gsmIdentity) as? Int
                            bsic?.let { tvGSM_BSIC.text = "BSIC: $it" } ?: run { tvGSM_BSIC.text = "BSIC: -" }
                        } catch (e: Exception) {
                            tvGSM_BSIC.text = "BSIC: -"
                        }

                        try {
                            val arfcnField = gsmIdentity.javaClass.getDeclaredField("arfcn")
                            arfcnField.isAccessible = true
                            val arfcn = arfcnField.get(gsmIdentity) as? Int
                            arfcn?.let { tvGSM_ARFCN.text = "ARFCN: $it" } ?: run { tvGSM_ARFCN.text = "ARFCN: -" }
                        } catch (e: Exception) {
                            tvGSM_ARFCN.text = "ARFCN: -"
                        }

                        // RSSI из сигнала
                        try {
                            val dbmField = gsmSignal.javaClass.getDeclaredField("dbm")
                            dbmField.isAccessible = true
                            val dbm = dbmField.get(gsmSignal) as? Int
                            dbm?.let { tvGSM_RSSI.text = "RSSI: $it dBm" } ?: run { tvGSM_RSSI.text = "RSSI: - dBm" }
                        } catch (e: Exception) {
                            tvGSM_RSSI.text = "RSSI: - dBm"
                        }

                        Log.d(TAG, "GSM Info получена")
                    } else {
                        clearGSMInfo()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка получения GSM информации", e)
                    clearGSMInfo()
                }
            } else {
                clearGSMInfo()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при получении информации о GSM", e)
            tvSignalLevel.text = "Уровень сигнала: Security Error"
            clearGSMInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения информации о GSM", e)
            tvSignalLevel.text = "Уровень сигнала: Error"
            clearGSMInfo()
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun update5GInfo() {
        try {
            // Проверяем разрешение
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                tvSignalLevel.text = "Уровень сигнала: Permission Required"
                clear5GInfo()
                return
            }

            val signalStrength = telephonyManager.signalStrength
            val level = signalStrength?.level ?: 0
            val signalLevelStr = signalLevelMap[level] ?: "Unknown ($level)"

            tvSignalLevel.text = "Уровень сигнала: $signalLevelStr"

            val color = when (signalLevelStr) {
                "Great" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
                "Good" -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                "Moderate" -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                "Poor" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(this, android.R.color.black)
            }
            tvSignalLevel.setTextColor(color)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val allCells: List<CellInfo>? = telephonyManager.allCellInfo
                    val nrCell = allCells?.filterIsInstance<CellInfoNr>()?.firstOrNull()

                    if (nrCell != null) {
                        val nrIdentity = nrCell.cellIdentity
                        val nrSignal = nrCell.cellSignalStrength

                        try {
                            val mccField = nrIdentity.javaClass.getDeclaredField("mccString")
                            mccField.isAccessible = true
                            val mcc = mccField.get(nrIdentity) as? String
                            mcc?.let { tvNR_MCC.text = "MCC: $it" } ?: run { tvNR_MCC.text = "MCC: -" }
                        } catch (e: Exception) {
                            tvNR_MCC.text = "MCC: -"
                        }

                        try {
                            val mncField = nrIdentity.javaClass.getDeclaredField("mncString")
                            mncField.isAccessible = true
                            val mnc = mncField.get(nrIdentity) as? String
                            mnc?.let { tvNR_MNC.text = "MNC: $it" } ?: run { tvNR_MNC.text = "MNC: -" }
                        } catch (e: Exception) {
                            tvNR_MNC.text = "MNC: -"
                        }

                        try {
                            val tacField = nrIdentity.javaClass.getDeclaredField("tac")
                            tacField.isAccessible = true
                            val tac = tacField.get(nrIdentity) as? Int
                            tac?.let { tvNR_TAC.text = "TAC: $it" } ?: run { tvNR_TAC.text = "TAC: -" }
                        } catch (e: Exception) {
                            tvNR_TAC.text = "TAC: -"
                        }

                        try {
                            val nciField = nrIdentity.javaClass.getDeclaredField("nci")
                            nciField.isAccessible = true
                            val nci = nciField.get(nrIdentity) as? Long
                            nci?.let { tvNR_NCI.text = "NCI: $it" } ?: run { tvNR_NCI.text = "NCI: -" }
                        } catch (e: Exception) {
                            tvNR_NCI.text = "NCI: -"
                        }

                        try {
                            val pciField = nrIdentity.javaClass.getDeclaredField("pci")
                            pciField.isAccessible = true
                            val pci = pciField.get(nrIdentity) as? Int
                            pci?.let { tvNR_PCI.text = "PCI: $it" } ?: run { tvNR_PCI.text = "PCI: -" }
                        } catch (e: Exception) {
                            tvNR_PCI.text = "PCI: -"
                        }

                        try {
                            val ssRsrpField = nrSignal.javaClass.getDeclaredField("ssRsrp")
                            ssRsrpField.isAccessible = true
                            val ssRsrp = ssRsrpField.get(nrSignal) as? Int
                            ssRsrp?.let { tvNR_SS_RSRP.text = "SS-RSRP: $it dBm" } ?: run { tvNR_SS_RSRP.text = "SS-RSRP: - dBm" }
                        } catch (e: Exception) {
                            tvNR_SS_RSRP.text = "SS-RSRP: - dBm"
                        }

                        try {
                            val ssRsrqField = nrSignal.javaClass.getDeclaredField("ssRsrq")
                            ssRsrqField.isAccessible = true
                            val ssRsrq = ssRsrqField.get(nrSignal) as? Int
                            ssRsrq?.let { tvNR_SS_RSRQ.text = "SS-RSRQ: $it dB" } ?: run { tvNR_SS_RSRQ.text = "SS-RSRQ: - dB" }
                        } catch (e: Exception) {
                            tvNR_SS_RSRQ.text = "SS-RSRQ: - dB"
                        }

                        try {
                            val ssSinrField = nrSignal.javaClass.getDeclaredField("ssSinr")
                            ssSinrField.isAccessible = true
                            val ssSinr = ssSinrField.get(nrSignal) as? Int
                            ssSinr?.let { tvNR_SS_SINR.text = "SS-SINR: $it dB" } ?: run { tvNR_SS_SINR.text = "SS-SINR: - dB" }
                        } catch (e: Exception) {
                            tvNR_SS_SINR.text = "SS-SINR: - dB"
                        }

                        Log.d(TAG, "5G NR Info получена")
                    } else {
                        clear5GInfo()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка получения 5G информации", e)
                    clear5GInfo()
                }
            } else {
                clear5GInfo()
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при получении информации о 5G", e)
            tvSignalLevel.text = "Уровень сигнала: Security Error"
            clear5GInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения информации о 5G", e)
            tvSignalLevel.text = "Уровень сигнала: Error"
            clear5GInfo()
        }
    }

    private fun clearWifiInfo() {
        tvWifiSSID.text = "Wi-Fi SSID: -"
        tvWifiBSSID.text = "Wi-Fi BSSID: -"
        tvWifiRSSI.text = "Wi-Fi RSSI: - dBm"
        tvWifiFrequency.text = "Wi-Fi Frequency: - MHz"
    }

    private fun clearLTEInfo() {
        tvLTE_MCC.text = "MCC: -"
        tvLTE_MNC.text = "MNC: -"
        tvLTE_TAC.text = "TAC: -"
        tvLTE_PCI.text = "PCI: -"
        tvLTE_EARFCN.text = "EARFCN: -"
        tvLTE_RSRP.text = "RSRP: - dBm"
        tvLTE_RSRQ.text = "RSRQ: - dB"
        tvLTE_RSSNR.text = "RSSNR: - dB"
    }

    private fun clearGSMInfo() {
        tvGSM_MCC.text = "MCC: -"
        tvGSM_MNC.text = "MNC: -"
        tvGSM_LAC.text = "LAC: -"
        tvGSM_CID.text = "CID: -"
        tvGSM_BSIC.text = "BSIC: -"
        tvGSM_ARFCN.text = "ARFCN: -"
        tvGSM_RSSI.text = "RSSI: - dBm"
    }

    private fun clear5GInfo() {
        tvNR_MCC.text = "MCC: -"
        tvNR_MNC.text = "MNC: -"
        tvNR_TAC.text = "TAC: -"
        tvNR_NCI.text = "NCI: -"
        tvNR_PCI.text = "PCI: -"
        tvNR_SS_RSRP.text = "SS-RSRP: - dBm"
        tvNR_SS_RSRQ.text = "SS-RSRQ: - dB"
        tvNR_SS_SINR.text = "SS-SINR: - dB"
    }

    private fun clearCellularInfo() {
        clearLTEInfo()
        clearGSMInfo()
        clear5GInfo()
    }

    private fun clearAllNetworkInfo() {
        clearWifiInfo()
        clearCellularInfo()
        tvSignalLevel.text = "Уровень сигнала: -"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun processAllCellInfo(location: Location) {
    }

    private fun saveLocationData(location: Location) {
        try {
            val documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir == null) {
                Log.e(TAG, "Не удалось получить директорию Documents")
                return
            }

            val file = File(documentsDir, JSON_FILE_NAME)

            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val existingData = mutableListOf<Map<String, Any>>()
            if (file.exists() && file.length() > 0) {
                try {
                    val fileContent = file.readText()
                    if (fileContent.isNotEmpty()) {
                        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val parsedData: List<Map<String, Any>>? = Gson().fromJson(fileContent, type)
                        if (parsedData != null) {
                            existingData.addAll(parsedData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка чтения существующих данных", e)
                }
            }

            val newLocationData = mapOf(
                "id" to UUID.randomUUID().toString(),
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "altitude" to location.altitude,
                "accuracy" to location.accuracy,
                "speed" to location.speed,
                "time" to location.time,
                "timestamp" to System.currentTimeMillis(),
                "date" to dateFormat.format(Date()),
                "formatted_lat" to coordinateFormat.format(location.latitude),
                "formatted_lon" to coordinateFormat.format(location.longitude)
            )

            existingData.add(newLocationData)

            val limitedData = if (existingData.size > 1000) {
                existingData.takeLast(1000)
            } else {
                existingData
            }

            FileWriter(file, false).use { writer ->
                val finalJson = Gson().toJson(limitedData)
                writer.write(finalJson)
            }

            Log.d(TAG, "Локация сохранена. Всего записей: ${limitedData.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения локации", e)
        }
    }

    private fun saveCellInfoData(locationInfo: Map<String, Any>) {
        try {
            val documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir == null) {
                Log.e(TAG, "Не удалось получить директорию Documents")
                return
            }

            val file = File(documentsDir, CELL_INFO_FILE_NAME)

            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val allCellInfo = mutableListOf<Map<String, Any>>()
            if (file.exists() && file.length() > 0) {
                try {
                    val fileContent = file.readText()
                    if (fileContent.isNotEmpty()) {
                        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val existingData: List<Map<String, Any>>? = gson.fromJson(fileContent, type)
                        if (existingData != null) {
                            allCellInfo.addAll(existingData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка чтения существующих данных о ячейках", e)
                }
            }

            allCellInfo.add(locationInfo)

            val limitedData = if (allCellInfo.size > 1000) {
                allCellInfo.takeLast(1000)
            } else {
                allCellInfo
            }

            FileWriter(file, false).use { writer ->
                val finalJson = gson.toJson(limitedData)
                writer.write(finalJson)
            }

            Log.d(TAG, "Информация о ячейках сохранена. Всего записей: ${limitedData.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения информации о ячейках", e)
        }
    }

    private fun checkAllPermissions(): Boolean {
        val locationFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val locationCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val wifiState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return locationFine && locationCoarse && phoneState && wifiState
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Нет разрешения на доступ к локации")
                Toast.makeText(this, "Нет разрешения на доступ к локации", Toast.LENGTH_SHORT).show()
                return
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при запуске обновлений локации", e)
            Toast.makeText(this, "Ошибка безопасности при получении локации", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска обновлений локации", e)
            Toast.makeText(this, "Ошибка получения локации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка остановки обновлений локации", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                startLocationUpdates()
                Toast.makeText(this, "Все разрешения получены!", Toast.LENGTH_SHORT).show()
                updateNetworkInfo()
            } else {
                Toast.makeText(this, "Некоторые разрешения отклонены", Toast.LENGTH_LONG).show()
                val deniedPermissions = mutableListOf<String>()
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permission)
                    }
                }
                Log.w(TAG, "Отклоненные разрешения: $deniedPermissions")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (checkAllPermissions()) {
            startLocationUpdates()
            updateNetworkInfo()
        }
    }
}
