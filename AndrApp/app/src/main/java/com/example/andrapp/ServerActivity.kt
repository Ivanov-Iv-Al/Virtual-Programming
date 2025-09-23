package com.example.andrapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        textViewResponse = findViewById(R.id.textViewResponse)
        buttonConnect = findViewById(R.id.buttonConnect)
        buttonDisconnect = findViewById(R.id.buttonDisconnect)

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
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Подключаемся к серверу
                socket = Socket("10.0.2.2", 12345) // 10.0.2.2 - специальный адрес для эмулятора
                outputStream = socket?.getOutputStream()
                inputStream = socket?.getInputStream()

                withContext(Dispatchers.Main) {
                    textViewResponse.text = "Подключено к серверу!"
                    buttonConnect.isEnabled = false
                    buttonDisconnect.isEnabled = true
                    buttonSend.isEnabled = true
                }

                // Слушаем ответы от сервера
                listenToServer()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResponse.text = "Ошибка подключения: ${e.message}"
                }
            }
        }
    }

    private fun listenToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (true) {
                    val response = reader.readLine()
                    if (response != null) {
                        withContext(Dispatchers.Main) {
                            textViewResponse.append("\nСервер: $response")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResponse.append("\nОшибка чтения: ${e.message}")
                }
            }
        }
    }

    private fun sendMessage() {
        val message = editTextMessage.text.toString()
        if (message.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    outputStream?.write("$message\n".toByteArray())
                    outputStream?.flush()

                    withContext(Dispatchers.Main) {
                        textViewResponse.append("\nВы: $message")
                        editTextMessage.text.clear()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        textViewResponse.append("\nОшибка отправки: ${e.message}")
                    }
                }
            }
        }
    }

    private fun disconnectFromServer() {
        job?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textViewResponse.append("\nОтключено от сервера")
        buttonConnect.isEnabled = true
        buttonDisconnect.isEnabled = false
        buttonSend.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}