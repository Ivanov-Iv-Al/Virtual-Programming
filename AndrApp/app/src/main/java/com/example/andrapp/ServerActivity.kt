package com.example.andrapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.zeromq.ZMQ
import java.io.File
import android.os.Environment
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

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
    private val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private const val SERVER_ADDRESS = "tcp://10.23.14.10:12345"
        private const val CONNECTION_TIMEOUT = 3000
        private const val TAG = "ServerActivity"
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
        buttonConnect.setOnClickListener { connectToServer() }
        buttonDisconnect.setOnClickListener { disconnectFromServer() }
        buttonSend.setOnClickListener { sendMessage() }
        buttonSendLocation.setOnClickListener { sendLocationData() }
    }

    private fun updateUI(connected: Boolean) {
        isConnected = connected
        buttonConnect.isEnabled = !connected
        buttonDisconnect.isEnabled = connected
        buttonSend.isEnabled = connected
        buttonSendLocation.isEnabled = connected
        editTextMessage.isEnabled = connected
    }

    private fun connectToServer() {
        textViewResponse.text = "Подключение к серверу..."
        buttonConnect.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    context = ZMQ.context(1)
                    socket = context?.socket(ZMQ.REQ)
                    socket?.receiveTimeOut = CONNECTION_TIMEOUT
                    socket?.connect(SERVER_ADDRESS)

                    socket?.send("CONNECT_TEST".toByteArray(ZMQ.CHARSET), 0)
                    socket?.recvStr(0)
                }

                if (response != null) {
                    textViewResponse.text = "Подключено!\nОтвет: $response"
                    updateUI(true)
                } else {
                    throw Exception("Таймаут ответа")
                }
            } catch (e: Exception) {
                textViewResponse.text = "Ошибка: ${e.message}"
                disconnectFromServer()
            }
        }
    }

    private fun sendMessage() {
        val message = editTextMessage.text.toString().trim()
        if (message.isEmpty()) return

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    socket?.send(message.toByteArray(ZMQ.CHARSET), 0)
                    socket?.recvStr(0)
                }
                appendToResponse("Вы: $message\nСервер: $response")
                editTextMessage.text.clear()
            } catch (e: Exception) {
                appendToResponse("Ошибка отправки: ${e.message}")
            }
        }
    }

    private fun disconnectFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            socket?.close()
            context?.term()
            socket = null
            context = null
        }
        updateUI(false)
        textViewResponse.text = "Отключено"
    }

    private fun appendToResponse(text: String) {
        val currentText = textViewResponse.text.toString()
        textViewResponse.text = "$text\n---\n$currentText"
    }

    private fun sendLocationData() {
        lifecycleScope.launch {
            try {
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "location_history.json")
                if (file.exists()) {
                    val content = withContext(Dispatchers.IO) { file.readText() }
                    val response = withContext(Dispatchers.IO) {
                        socket?.send(content.toByteArray(ZMQ.CHARSET), 0)
                        socket?.recvStr(0)
                    }
                    appendToResponse("Локация отправлена. Сервер: $response")
                } else {
                    appendToResponse("Файл локации не найден")
                }
            } catch (e: Exception) {
                appendToResponse("Ошибка файла: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        disconnectFromServer()
        super.onDestroy()
    }
}
