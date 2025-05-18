package com.example.andrapp

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Player : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var playButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var trackName: TextView
    private lateinit var selectMusicButton: Button
    private lateinit var showSongsButton: Button
    private lateinit var createPlaylistButton: Button
    private lateinit var showPlaylistsButton: Button
    private lateinit var albumArt: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentTrackIndex = 0
    private var tracks = mutableListOf<Track>()
    private var playlists = mutableListOf<Playlist>()
    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        private const val TAG = "MusicPlayer"
        private const val REQUEST_PERMISSION = 1
        private const val REQUEST_PICK_MUSIC = 2
        private const val PREFS_NAME = "MusicPlayerPrefs"
    }

    data class Track(
        val uri: Uri,
        val name: String,
        val albumId: Long,
        val duration: Long
    )

    data class Playlist(
        val name: String,
        val tracks: MutableList<Track> = mutableListOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player2)

        initViews()
        setupMediaPlayer()
        setupButtons()
        loadPlaylists()
        checkPermissions()
    }

    private fun initViews() {
        seekBar = findViewById(R.id.seekBar)
        playButton = findViewById(R.id.playButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        trackName = findViewById(R.id.trackName)
        selectMusicButton = findViewById(R.id.selectMusicButton)
        showSongsButton = findViewById(R.id.showSongsButton)
        createPlaylistButton = findViewById(R.id.createPlaylistButton)
        showPlaylistsButton = findViewById(R.id.showPlaylistsButton)
        albumArt = findViewById(R.id.albumArt)
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        albumArt.setImageResource(R.drawable.ic_music_note)
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { nextTrack() }
            setOnPreparedListener {
                start()
                this@Player.isPlaying = true
                playButton.text = "Pause"
                startSeekbarUpdate()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Error what=$what extra=$extra")
                showToast("Ошибка воспроизведения")
                false
            }
        }
    }

    private fun setupButtons() {
        selectMusicButton.setOnClickListener { checkPermissionAndBrowseMusic() }
        showSongsButton.setOnClickListener { showAllSongsDialog() }
        createPlaylistButton.setOnClickListener { showCreatePlaylistDialog() }
        showPlaylistsButton.setOnClickListener { showPlaylistsDialog() }

        playButton.setOnClickListener {
            if (tracks.isEmpty()) {
                showToast("Сначала выберите музыку")
                return@setOnClickListener
            }
            if (isPlaying) pauseMusic() else playMusic()
        }

        nextButton.setOnClickListener {
            if (tracks.isEmpty()) {
                showToast("Сначала выберите музыку")
                return@setOnClickListener
            }
            nextTrack()
        }

        prevButton.setOnClickListener {
            if (tracks.isEmpty()) {
                showToast("Сначала выберите музыку")
                return@setOnClickListener
            }
            previousTrack()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissions() {
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
        }
    }

    private fun startSeekbarUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying) {
                    seekBar.progress = mediaPlayer.currentPosition
                    seekBar.max = mediaPlayer.duration
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun playTrack(index: Int) {
        if (index < 0 || index >= tracks.size) return

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(applicationContext, tracks[index].uri)
            mediaPlayer.prepareAsync()
            currentTrackIndex = index
            trackName.text = tracks[index].name
            loadAlbumArt(tracks[index].albumId)
        } catch (e: Exception) {
            Log.e(TAG, "Play track error", e)
            showToast("Ошибка: ${e.message}")
        }
    }

    private fun loadAlbumArt(albumId: Long) {
        try {
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
            albumArt.setImageURI(null)
            albumArt.setImageURI(albumArtUri)
            if (albumArt.drawable == null) {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load album art error", e)
            albumArt.setImageResource(R.drawable.ic_music_note)
        }
    }

    private fun playMusic() {
        if (!mediaPlayer.isPlaying) {
            if (mediaPlayer.currentPosition > 0) {
                mediaPlayer.start()
            } else {
                playTrack(currentTrackIndex)
            }
            isPlaying = true
            playButton.text = "Pause"
            startSeekbarUpdate()
        }
    }

    private fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            playButton.text = "Play"
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun nextTrack() {
        if (tracks.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex < tracks.size - 1) currentTrackIndex + 1 else 0
        playTrack(currentTrackIndex)
    }

    private fun previousTrack() {
        if (tracks.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else tracks.size - 1
        playTrack(currentTrackIndex)
    }

    private fun showAllSongsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_song_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.songsRecyclerView)
        val adapter = SongAdapter(getAllSongsFromStorage()) { track ->
            tracks.clear()
            tracks.add(track)
            currentTrackIndex = 0
            playTrack(currentTrackIndex)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Все песни")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showCreatePlaylistDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist, null)
        val playlistNameEditText = dialogView.findViewById<EditText>(R.id.playlistNameEditText)
        val songsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.songsRecyclerView)

        val allSongs = getAllSongsFromStorage()
        val adapter = SongAdapter(allSongs) { }
        adapter.setMultiSelectMode(true)

        songsRecyclerView.layoutManager = LinearLayoutManager(this)
        songsRecyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Создать плейлист")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val playlistName = playlistNameEditText.text.toString()
                if (playlistName.isNotEmpty() && adapter.getSelectedSongs().isNotEmpty()) {
                    val newPlaylist = Playlist(playlistName)
                    newPlaylist.tracks.addAll(adapter.getSelectedSongs())
                    playlists.add(newPlaylist)
                    savePlaylists()
                    showToast("Плейлист создан")
                } else {
                    showToast("Введите название и выберите песни")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPlaylistsDialog() {
        if (playlists.isEmpty()) {
            showToast("У вас пока нет плейлистов")
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.playlistsRecyclerView)

        val adapter = object : RecyclerView.Adapter<PlaylistViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist, parent, false)
                return PlaylistViewHolder(view, this)
            }

            override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
                val playlist = playlists[position]
                holder.bind(playlist) {
                    tracks.clear()
                    tracks.addAll(playlist.tracks)
                    currentTrackIndex = 0
                    playTrack(currentTrackIndex)
                }
            }

            override fun getItemCount(): Int = playlists.size
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Мои плейлисты")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    inner class PlaylistViewHolder(
        itemView: View,
        private val adapter: RecyclerView.Adapter<*>
    ) : RecyclerView.ViewHolder(itemView) {
        private val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        private val trackCount: TextView = itemView.findViewById(R.id.trackCount)
        private val playlistArt: ImageView = itemView.findViewById(R.id.playlistArt)

        fun bind(playlist: Playlist, onClick: () -> Unit) {
            playlistName.text = playlist.name
            trackCount.text = "${playlist.tracks.size} треков"

            if (playlist.tracks.isNotEmpty()) {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    playlist.tracks[0].albumId
                )
                playlistArt.setImageURI(albumArtUri)
                if (playlistArt.drawable == null) {
                    playlistArt.setImageResource(R.drawable.ic_music_note)
                }
            } else {
                playlistArt.setImageResource(R.drawable.ic_music_note)
            }

            itemView.setOnClickListener { onClick() }

            itemView.setOnLongClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Удалить плейлист?")
                    .setMessage("Вы уверены, что хотите удалить '${playlist.name}'?")
                    .setPositiveButton("Удалить") { _, _ ->
                        playlists.removeAt(adapterPosition)
                        savePlaylists()
                        adapter.notifyItemRemoved(adapterPosition)
                        showToast("Плейлист удален")
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                true
            }
        }
    }

    private fun getAllSongsFromStorage(): List<Track> {
        val songs = mutableListOf<Track>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return songs
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                songs.add(Track(uri, name, albumId, duration))
            }
        }
        return songs
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Выберите музыку"), REQUEST_PICK_MUSIC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_MUSIC && resultCode == RESULT_OK) {
            tracks.clear()


            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uri ->
                        addTrackFromUri(uri)
                    }
                }
            } ?: run {

                data?.data?.let { uri ->
                    addTrackFromUri(uri)
                }
            }

            if (tracks.isNotEmpty()) {
                currentTrackIndex = 0
                playTrack(currentTrackIndex)
            }
        }
    }

    private fun addTrackFromUri(uri: Uri) {
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    tracks.add(Track(uri, name, albumId, duration))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding track from URI", e)
        }
    }

    private fun savePlaylists() {
        val json = Gson().toJson(playlists)
        sharedPrefs.edit().putString("playlists", json).apply()
    }

    private fun loadPlaylists() {
        val json = sharedPrefs.getString("playlists", null)
        json?.let {
            val type = object : TypeToken<List<Playlist>>() {}.type
            playlists = Gson().fromJson(it, type) ?: mutableListOf()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadTracks()
                } else {
                    showToast("Необходимы разрешения для доступа к музыке")
                }
            }
        }
    }

    private fun loadTracks() {
        tracks.addAll(getAllSongsFromStorage())
        if (tracks.isNotEmpty()) {
            playTrack(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer.release()
    }
}

class SongAdapter(
    private val songs: List<Player.Track>,
    private val onItemClick: (Player.Track) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val selectedSongs = mutableSetOf<Player.Track>()
    private var isMultiSelectMode = false

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        notifyDataSetChanged()
    }

    fun getSelectedSongs(): List<Player.Track> {
        return selectedSongs.toList()
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songName: TextView = itemView.findViewById(R.id.songName)
        private val songDuration: TextView = itemView.findViewById(R.id.songDuration)
        private val albumArt: ImageView = itemView.findViewById(R.id.albumArt)

        fun bind(track: Player.Track) {
            songName.text = track.name
            songDuration.text = formatDuration(track.duration)

            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                track.albumId
            )
            albumArt.setImageURI(albumArtUri)
            if (albumArt.drawable == null) {
                albumArt.setImageResource(R.drawable.ic_music_note)
            }

            itemView.isSelected = selectedSongs.contains(track)
            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(track)
                } else {
                    onItemClick(track)
                }
            }
        }

        private fun toggleSelection(track: Player.Track) {
            if (selectedSongs.contains(track)) {
                selectedSongs.remove(track)
            } else {
                selectedSongs.add(track)
            }
            notifyItemChanged(adapterPosition)
        }

        private fun formatDuration(duration: Long): String {
            val seconds = (duration / 1000) % 60
            val minutes = (duration / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size
}