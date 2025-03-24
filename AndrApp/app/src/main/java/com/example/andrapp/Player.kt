package com.example.andrapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Player : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var playButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var trackName: TextView

    private val handler = Handler()
    private var isPlaying = false
    private var currentTrackIndex = 0


    private val tracks = listOf(
        R.raw.track1,
        R.raw.track2,
        R.raw.track3
    )


    private val trackNames = listOf(
        "Track 1",
        "Track 2",
        "Track 3"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player2)


        seekBar = findViewById(R.id.seekBar)
        playButton = findViewById(R.id.playButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        trackName = findViewById(R.id.trackName)


        playTrack(currentTrackIndex)


        playButton.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic()
            }
        }


        nextButton.setOnClickListener {
            nextTrack()
        }


        prevButton.setOnClickListener {
            previousTrack()
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    private fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer.create(this, tracks[index])
        mediaPlayer.start()
        isPlaying = true
        playButton.text = "Pause"
        trackName.text = trackNames[index]
        seekBar.max = mediaPlayer.duration
        updateSeekBar()


        mediaPlayer.setOnCompletionListener {
            nextTrack()
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
        if (currentTrackIndex < tracks.size - 1) {
            currentTrackIndex++
        } else {
            currentTrackIndex = 0
        }
        playTrack(currentTrackIndex)
    }


    private fun previousTrack() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
        } else {
            currentTrackIndex = tracks.size - 1
        }
        playTrack(currentTrackIndex)
    }


    private fun updateSeekBar() {
        handler.postDelayed({
            seekBar.progress = mediaPlayer.currentPosition
            updateSeekBar()
        }, 1000)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}