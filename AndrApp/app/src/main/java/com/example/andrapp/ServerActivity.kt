package com.example.andrapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket

class ServerActivity : AppCompatActivity() {

    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewResponse: TextView
    private lateinit var buttonConnect: Button
    private lateinit var buttonDisconnect: Button

    private var socket: Socket? = null
    private var outputStream: BufferedWriter? = null
    private var inputStream: BufferedReader? = null
    private var isConnected = false

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

        editTextMessage.isEnabled = true
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
    }

    private fun connectToServer() {
        textViewResponse.text = "Подключение к серверу"
        buttonConnect.isEnabled = false

        lifecycleScope.launch {
            try {
                socket = Socket("10.0.2.2", 12345)

                outputStream = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                inputStream = BufferedReader(InputStreamReader(socket!!.getInputStream()))

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
                    val message = inputStream?.readLine()
                    if (message != null) {
                        textViewResponse.append("\nСервер: $message")
                    } else {
                        // Сервер отключился
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
                // Отправляем сообщение
                outputStream?.write(message)
                outputStream?.newLine()
                outputStream?.flush()

                textViewResponse.append("\nВы: $message")
                editTextMessage.text.clear()
            } catch (e: Exception) {
                textViewResponse.append("\nОшибка отправки: ${e.message}")
                disconnectFromServer()
            }
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
        editTextMessage.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}