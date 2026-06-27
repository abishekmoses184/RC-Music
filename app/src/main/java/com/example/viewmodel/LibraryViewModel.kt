package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MusicDatabase
import com.example.data.model.Song
import com.example.data.model.MockMusicData
import com.example.data.repository.MusicRepository
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao())

    // --- State Flows ---
    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteSongs = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed = repository.recentlyPlayedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Caching / Downloads States ---
    val downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadedSongs = MutableStateFlow<Set<String>>(emptySet())

    // --- Recently Added (Library Store Imports) ---
    private val libraryPrefs = application.getSharedPreferences("library_prefs", android.content.Context.MODE_PRIVATE)
    private val _recentlyAdded = MutableStateFlow<List<Song>>(emptyList())
    val recentlyAdded: StateFlow<List<Song>> = _recentlyAdded.asStateFlow()

    // --- User Profile & Music Preferences States ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow("Guest User")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow("guest@musicapp.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredGenres: StateFlow<Set<String>> = _preferredGenres.asStateFlow()

    private val _audioQuality = MutableStateFlow("High Quality (256kbps)")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _dolbyAtmos = MutableStateFlow(true)
    val dolbyAtmos: StateFlow<Boolean> = _dolbyAtmos.asStateFlow()

    private val _spatialAudio = MutableStateFlow(true)
    val spatialAudio: StateFlow<Boolean> = _spatialAudio.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(true)
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    init {
        updateDownloadedSongs()
        loadRecentlyAdded()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _isLoggedIn.value = libraryPrefs.getBoolean("profile_logged_in", false)
        _username.value = libraryPrefs.getString("profile_username", "Guest User") ?: "Guest User"
        _userEmail.value = libraryPrefs.getString("profile_email", "guest@musicapp.com") ?: "guest@musicapp.com"
        
        val genresStr = libraryPrefs.getString("profile_genres", "") ?: ""
        _preferredGenres.value = if (genresStr.isEmpty()) {
            setOf("Pop", "Rock", "Lo-Fi")
        } else {
            genresStr.split(",").toSet()
        }

        _audioQuality.value = libraryPrefs.getString("profile_audio_quality", "High Quality (256kbps)") ?: "High Quality (256kbps)"
        _dolbyAtmos.value = libraryPrefs.getBoolean("profile_dolby_atmos", true)
        _spatialAudio.value = libraryPrefs.getBoolean("profile_spatial_audio", true)
        _hapticFeedback.value = libraryPrefs.getBoolean("profile_haptic_feedback", true)
    }

    fun login(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) return false
        // Simple authentication: save credentials or verify against registered user
        val registeredPass = libraryPrefs.getString("reg_pass_$user", null)
        if (registeredPass != null && registeredPass != pass) {
            return false
        }
        
        val email = libraryPrefs.getString("reg_email_$user", "$user@musicapp.com") ?: "$user@musicapp.com"
        
        libraryPrefs.edit().apply {
            putBoolean("profile_logged_in", true)
            putString("profile_username", user)
            putString("profile_email", email)
            // If they weren't registered yet, register them on the fly
            if (registeredPass == null) {
                putString("reg_pass_$user", pass)
                putString("reg_email_$user", email)
            }
        }.apply()

        _isLoggedIn.value = true
        _username.value = user
        _userEmail.value = email
        return true
    }

    fun registerAndLogin(user: String, pass: String, email: String): Boolean {
        if (user.isBlank() || pass.isBlank() || email.isBlank()) return false
        
        libraryPrefs.edit().apply {
            putBoolean("profile_logged_in", true)
            putString("profile_username", user)
            putString("profile_email", email)
            putString("reg_pass_$user", pass)
            putString("reg_email_$user", email)
        }.apply()

        _isLoggedIn.value = true
        _username.value = user
        _userEmail.value = email
        return true
    }

    fun logout() {
        libraryPrefs.edit().apply {
            putBoolean("profile_logged_in", false)
            putString("profile_username", "Guest User")
            putString("profile_email", "guest@musicapp.com")
        }.apply()

        _isLoggedIn.value = false
        _username.value = "Guest User"
        _userEmail.value = "guest@musicapp.com"
    }

    fun togglePreferredGenre(genre: String) {
        val current = _preferredGenres.value.toMutableSet()
        if (current.contains(genre)) {
            current.remove(genre)
        } else {
            current.add(genre)
        }
        _preferredGenres.value = current
        libraryPrefs.edit().putString("profile_genres", current.joinToString(",")).apply()
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        libraryPrefs.edit().putString("profile_audio_quality", quality).apply()
    }

    fun setDolbyAtmos(enabled: Boolean) {
        _dolbyAtmos.value = enabled
        libraryPrefs.edit().putBoolean("profile_dolby_atmos", enabled).apply()
    }

    fun setSpatialAudio(enabled: Boolean) {
        _spatialAudio.value = enabled
        libraryPrefs.edit().putBoolean("profile_spatial_audio", enabled).apply()
    }

    fun setHapticFeedback(enabled: Boolean) {
        _hapticFeedback.value = enabled
        libraryPrefs.edit().putBoolean("profile_haptic_feedback", enabled).apply()
    }

    fun loadRecentlyAdded() {
        val savedIdsStr = libraryPrefs.getString("recently_added_ids_v2", null)
        val savedList = if (savedIdsStr != null) {
            val ids = savedIdsStr.split(",").filter { it.isNotBlank() }
            ids.mapNotNull { id ->
                MockMusicData.songs.find { it.id == id } ?: MockMusicData.radioStations.find { it.id == id }
            }
        } else {
            emptyList()
        }

        // If the saved list is empty or represents the old 3-song template, populate with our entire 28 songs
        if (savedList.size < 10) {
            _recentlyAdded.value = MockMusicData.songs
            saveRecentlyAdded(MockMusicData.songs)
        } else {
            _recentlyAdded.value = savedList
        }
    }

    private fun saveRecentlyAdded(list: List<Song>) {
        val idsStr = list.joinToString(",") { it.id }
        libraryPrefs.edit().putString("recently_added_ids_v2", idsStr).apply()
    }

    fun addToRecentlyAdded(song: Song) {
        val current = _recentlyAdded.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        _recentlyAdded.value = current
        saveRecentlyAdded(current)
    }

    fun removeFromRecentlyAdded(songId: String) {
        val current = _recentlyAdded.value.filter { it.id != songId }
        _recentlyAdded.value = current
        saveRecentlyAdded(current)
    }

    fun updateDownloadedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getApplication<Application>().filesDir, "downloads")
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    val ids = files?.filter { it.isFile && it.name.endsWith(".mp3") }
                        ?.map { it.name.removeSuffix(".mp3") }
                        ?.toSet() ?: emptySet()
                    downloadedSongs.value = ids
                } else {
                    downloadedSongs.value = emptySet()
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error scanning downloaded songs", e)
            }
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                downloadProgress.update { it + (song.id to 0f) }
                
                val url = URL(song.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val fileLength = connection.contentLength
                    val input = connection.inputStream
                    
                    val dir = File(getApplication<Application>().filesDir, "downloads")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "${song.id}.mp3")
                    val output = FileOutputStream(file)
                    
                    val data = ByteArray(4096)
                    var total = 0L
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val progress = total.toFloat() / fileLength
                            downloadProgress.update { it + (song.id to progress) }
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                    
                    downloadProgress.update { it - song.id }
                    updateDownloadedSongs()
                } else {
                    Log.e("LibraryViewModel", "Server returned HTTP ${connection.responseCode} for ${song.title}")
                    downloadProgress.update { it - song.id }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error downloading song ${song.title}", e)
                downloadProgress.update { it - song.id }
            }
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getApplication<Application>().filesDir, "downloads")
                val file = File(dir, "$songId.mp3")
                if (file.exists()) {
                    file.delete()
                }
                updateDownloadedSongs()
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error deleting download $songId", e)
            }
        }
    }

    // A map of playlistId to list of songs in that playlist
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return repository.getSongsInPlaylist(playlistId)
    }

    fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            // Pick a random styled gradient background
            val gradients = listOf(
                "linear-gradient(135deg, #f857a6 0%, #ff5858 100%)",
                "linear-gradient(135deg, #1D976C 0%, #93F9B9 100%)",
                "linear-gradient(135deg, #00c6ff 0%, #0072ff 100%)",
                "linear-gradient(135deg, #F3904F 0%, #3B4371 100%)",
                "linear-gradient(135deg, #4A00E0 0%, #8E2DE2 100%)",
                "linear-gradient(135deg, #f12711 0%, #f5af19 100%)"
            )
            val randomGradient = gradients.random()
            repository.createPlaylist(title, description, randomGradient)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
        }
    }

    fun reorderPlaylistTracks(playlistId: Long, songIds: List<String>) {
        viewModelScope.launch {
            repository.reorderPlaylistTracks(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun importLocalMusicFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val songsList = mutableListOf<Song>()
                
                val uri: android.net.Uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.ALBUM,
                    android.provider.MediaStore.Audio.Media.DURATION,
                    android.provider.MediaStore.Audio.Media.DATA
                )
                
                val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
                
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    
                    val gradients = listOf(
                        "linear-gradient(135deg, #FF3B30 0%, #FF9500 100%)",
                        "linear-gradient(135deg, #5856D6 0%, #007AFF 100%)",
                        "linear-gradient(135deg, #AF52DE 0%, #FF2D55 100%)",
                        "linear-gradient(135deg, #34C759 0%, #007AFF 100%)",
                        "linear-gradient(135deg, #1D976C 0%, #93F9B9 100%)"
                    )
                    
                    var index = 0
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol).toString()
                        val title = cursor.getString(titleCol) ?: "Unknown Title"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val durationMs = cursor.getInt(durationCol)
                        val durationSec = if (durationMs > 0) durationMs / 1000 else 180
                        val dataPath = cursor.getString(dataCol) ?: ""
                        
                        val song = Song(
                            id = "local_$id",
                            url = if (dataPath.isNotEmpty()) "file://$dataPath" else "",
                            title = title,
                            artist = artist,
                            album = album,
                            artwork = gradients[index % gradients.size],
                            duration = durationSec,
                            genre = "Local Audio"
                        )
                        songsList.add(song)
                        index++
                    }
                }
                
                if (songsList.isNotEmpty()) {
                    Log.d("LibraryViewModel", "Successfully scanned and found ${songsList.size} local music files.")
                    val current = _recentlyAdded.value.toMutableList()
                    current.removeAll { item -> songsList.any { it.id == item.id } }
                    current.addAll(0, songsList)
                    _recentlyAdded.value = current
                    saveRecentlyAdded(current)
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Failed to query local MediaStore", e)
            }
        }
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
