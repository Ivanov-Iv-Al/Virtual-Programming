package com.example.andrapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        val buttonServer = findViewById<Button>(R.id.button_server)
        buttonServer.setOnClickListener {
            val intent = Intent(this, ServerActivity::class.java)
            startActivity(intent)
        }
    }
}
