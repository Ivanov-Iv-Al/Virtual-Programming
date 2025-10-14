package com.example.andrapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.zeromq.ZMQ
import java.io.File
import java.io.FileWriter
import android.os.Environment
import android.util.Log

class ServerActivity : AppCompatActivity() {

    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewResponse: TextView
    private lateinit var buttonConnect: Button
    private lateinit var buttonDisconnect: Button
    private lateinit var buttonSendLocation: Button

    private var context: ZMQ.Context? = null
    private var socket: ZMQ.Socket? = null
    private var isConnected = false
    private val gson = Gson()

    companion object {
        private const val SERVER_ADDRESS = "tcp://192.168.56.1:12345"
        private const val CONNECTION_TIMEOUT = 3000
    }

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
        buttonSendLocation = findViewById(R.id.buttonSendLocation)

        updateUI(false)
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
                    context = ZMQ.context(1)

                    socket = context?.socket(ZMQ.REQ)

                    socket?.setReceiveTimeOut(CONNECTION_TIMEOUT)

                    socket?.connect(SERVER_ADDRESS)

                    socket?.send("TEST_CONNECTION".toByteArray(), 0)
                    val response = socket?.recvStr(0)

                    if (response != null) {
                        withContext(Dispatchers.Main) {
                            textViewResponse.text = "✅ Подключено к ZeroMQ серверу!"
                            updateUI(true)
                            isConnected = true
                        }
                    } else {
                        throw Exception("Нет ответа от сервера")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResponse.text = "Ошибка подключения: ${e.message}"
                    updateUI(false)
                    isConnected = false
                    disconnectFromServer()
                }
            }
        }
    }

    private fun sendMessage() {
        val message = editTextMessage.text.toString().trim()
        if (message.isEmpty()) {
            textViewResponse.append("\n⚠Введите сообщение")
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    socket?.send(message.toByteArray(), 0)
                    socket?.recvStr(0)
                }

                withContext(Dispatchers.Main) {
                    textViewResponse.append("\nВы: $message")
                    textViewResponse.append("\nСервер: $response")
                    editTextMessage.text.clear()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResponse.append("\nОшибка отправки: ${e.message}")
                    disconnectFromServer()
                }
            }
        }
    }

    private fun sendLocationData() {
        lifecycleScope.launch {
            try {
                val locationData = readLocationData()
                if (locationData.isNotEmpty()) {
                    val latestLocation = locationData.last()
                    val locationMessage = "LOCATION_DATA: ${gson.toJson(latestLocation)}"

                    val response = withContext(Dispatchers.IO) {
                        socket?.send(locationMessage.toByteArray(), 0)
                        socket?.recvStr(0)
                    }

                    withContext(Dispatchers.Main) {
                        textViewResponse.append("\nОтправлены данные локации")
                        textViewResponse.append("\nСервер: $response")

                        Log.d("ZMQ_LOCATION", "Отправлена локация: $latestLocation")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        textViewResponse.append("\nНет данных о локации")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResponse.append("\nОшибка отправки локации: ${e.message}")
                    Log.e("ZMQ_LOCATION", "Ошибка отправки локации", e)
                }
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
            Log.e("ServerActivity", "Ошибка чтения локации", e)
            emptyList()
        }
    }

    private fun disconnectFromServer() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    socket?.close()
                    context?.term()
                } catch (e: Exception) {
                    Log.e("ServerActivity", "Ошибка закрытия соединения", e)
                }
            }

            withContext(Dispatchers.Main) {
                isConnected = false
                socket = null
                context = null
                updateUI(false)
                textViewResponse.append("\n🔌 Отключено от сервера")
            }
        }
    }

    private fun updateUI(connected: Boolean) {
        buttonConnect.isEnabled = !connected
        buttonDisconnect.isEnabled = connected
        buttonSend.isEnabled = connected
        buttonSendLocation.isEnabled = connected
        editTextMessage.isEnabled = connected
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}