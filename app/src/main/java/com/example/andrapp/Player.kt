package com.example.andrapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Player : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var playButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var trackName: TextView
    private lateinit var selectMusicButton: Button

    private val handler = Handler()
    private var isPlaying = false
    private var currentTrackIndex = 0
    private var tracks = mutableListOf<Uri>()
    private var trackNames = mutableListOf<String>()

    companion object {
        private const val REQUEST_PERMISSION = 1
        private const val REQUEST_PICK_MUSIC = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player2)

        seekBar = findViewById(R.id.seekBar)
        playButton = findViewById(R.id.playButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        trackName = findViewById(R.id.trackName)
        selectMusicButton = findViewById(R.id.selectMusicButton)


        mediaPlayer = MediaPlayer()

        selectMusicButton.setOnClickListener {
            checkPermissionAndBrowseMusic()
        }

        playButton.setOnClickListener {
            if (tracks.isEmpty()) {
                Toast.makeText(this, "Сначала выберите музыку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic()
            }
        }

        nextButton.setOnClickListener {
            if (tracks.isEmpty()) {
                Toast.makeText(this, "Сначала выберите музыку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            nextTrack()
        }

        prevButton.setOnClickListener {
            if (tracks.isEmpty()) {
                Toast.makeText(this, "Сначала выберите музыку", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            previousTrack()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && ::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissionAndBrowseMusic() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        } else {
            browseMusic()
        }
    }

    private fun browseMusic() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Выберите музыку"), REQUEST_PICK_MUSIC)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                browseMusic()
            } else {
                Toast.makeText(
                    this,
                    "Разрешение необходимо для выбора музыки",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_MUSIC && resultCode == RESULT_OK) {
            tracks.clear()
            trackNames.clear()

            if (data?.clipData != null) {
                // Multiple files selected
                val clipData = data.clipData
                for (i in 0 until (clipData?.itemCount ?: 0)) {
                    clipData?.getItemAt(i)?.uri?.let { uri ->
                        tracks.add(uri)
                        trackNames.add(getFileNameFromUri(uri))
                    }
                }
            } else if (data?.data != null) {
                // Single file selected
                data.data?.let { uri ->
                    tracks.add(uri)
                    trackNames.add(getFileNameFromUri(uri))
                }
            }

            if (tracks.isNotEmpty()) {
                currentTrackIndex = 0
                playTrack(currentTrackIndex)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "Неизвестный трек"
        val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                fileName = it.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, tracks[index])
            mediaPlayer.prepare()
            mediaPlayer.start()
            isPlaying = true
            playButton.text = "Pause"
            trackName.text = trackNames[index]
            seekBar.max = mediaPlayer.duration
            updateSeekBar()

            mediaPlayer.setOnCompletionListener {
                nextTrack()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка воспроизведения трека", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic() {
        mediaPlayer.start()
        isPlaying = true
        playButton.text = "Pause"
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        isPlaying = false
        playButton.text = "Play"
    }

    private fun nextTrack() {
        if (tracks.isEmpty()) return

        if (currentTrackIndex < tracks.size - 1) {
            currentTrackIndex++
        } else {
            currentTrackIndex = 0
        }
        playTrack(currentTrackIndex)
    }

    private fun previousTrack() {
        if (tracks.isEmpty()) return

        if (currentTrackIndex > 0) {
            currentTrackIndex--
        } else {
            currentTrackIndex = tracks.size - 1
        }
        playTrack(currentTrackIndex)
    }

    private fun updateSeekBar() {
        handler.postDelayed({
            if (::mediaPlayer.isInitialized && isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                updateSeekBar()
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacksAndMessages(null)
    }
}