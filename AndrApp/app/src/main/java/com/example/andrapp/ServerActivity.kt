package com.example.andrapp

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.util.*
import com.google.gson.reflect.TypeToken

class ServerActivity : AppCompatActivity() {

    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewResponse: TextView
    private lateinit var buttonConnect: Button
    private lateinit var buttonDisconnect: Button
    private lateinit var buttonSendLocation: Button // Новая кнопка для отправки локации

    private var socket: Socket? = null
    private var outputStream: BufferedWriter? = null
    private var inputStream: BufferedReader? = null
    private var isConnected = false
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        textViewResponse = findViewById(R.id.textViewResponse)
        buttonConnect = findViewById(R.id.buttonConnect)
        buttonDisconnect = findViewById(R.id.buttonDisconnect)
        buttonSendLocation = findViewById(R.id.buttonSendLocation) // Добавьте эту кнопку в layout

        editTextMessage.isEnabled = true
        buttonSendLocation.isEnabled = false
    }

    private fun setupClickListeners() {
        buttonConnect.setOnClickListener {
            connectToServer()
        }

        buttonDisconnect.setOnClickListener {
            disconnectFromServer()
        }

        buttonSend.setOnClickListener {
            sendMessage()
        }

        buttonSendLocation.setOnClickListener {
            sendLocationData()
        }
    }

    private fun connectToServer() {
        textViewResponse.text = "Подключение к серверу"
        buttonConnect.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    socket = Socket("10.0.2.2", 12345)
                    outputStream = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                    inputStream = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                }

                textViewResponse.text = "Подключено к серверу!"
                updateUI(true)
                isConnected = true

                startListening()

            } catch (e: Exception) {
                textViewResponse.text = "Ошибка подключения: ${e.message}"
                updateUI(false)
                isConnected = false
            }
        }
    }

    private fun startListening() {
        lifecycleScope.launch {
            try {
                while (isConnected && !socket!!.isClosed) {
                    val message = withContext(Dispatchers.IO) {
                        inputStream?.readLine()
                    }
                    if (message != null) {
                        textViewResponse.append("\nСервер: $message")
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    textViewResponse.append("\nСоединение разорвано")
                    disconnectFromServer()
                }
            }
        }
    }

    private fun sendMessage() {
        val message = editTextMessage.text.toString().trim()
        if (message.isEmpty()) {
            textViewResponse.append("\nВведите сообщение")
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    outputStream?.write(message)
                    outputStream?.newLine()
                    outputStream?.flush()
                }

                textViewResponse.append("\nВы: $message")
                editTextMessage.text.clear()
            } catch (e: Exception) {
                textViewResponse.append("\nОшибка отправки: ${e.message}")
                disconnectFromServer()
            }
        }
    }

    private fun sendLocationData() {
        lifecycleScope.launch {
            try {
                val locationData = readLocationData()
                if (locationData.isNotEmpty()) {
                    val latestLocation = locationData.last() // Берем последнюю запись
                    val locationMessage = "LOCATION_DATA: ${gson.toJson(latestLocation)}"

                    withContext(Dispatchers.IO) {
                        outputStream?.write(locationMessage)
                        outputStream?.newLine()
                        outputStream?.flush()
                    }

                    textViewResponse.append("\nОтправлены данные локации")
                } else {
                    textViewResponse.append("\nНет данных о локации")
                }
            } catch (e: Exception) {
                textViewResponse.append("\nОшибка отправки локации: ${e.message}")
            }
        }
    }

    private fun readLocationData(): List<Map<String, Any>> {
        return try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "location_history.json")
            if (file.exists()) {
                val fileContent = file.readText()
                if (fileContent.isNotEmpty()) {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    gson.fromJson<List<Map<String, Any>>>(fileContent, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun disconnectFromServer() {
        isConnected = false

        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateUI(false)
        textViewResponse.append("\nОтключено от сервера")
    }

    private fun updateUI(connected: Boolean) {
        buttonConnect.isEnabled = !connected
        buttonDisconnect.isEnabled = connected
        buttonSend.isEnabled = connected
        buttonSendLocation.isEnabled = connected
        editTextMessage.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}
