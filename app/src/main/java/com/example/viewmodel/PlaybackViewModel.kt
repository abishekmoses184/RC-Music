package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MusicDatabase
import com.example.data.model.LyricLine
import com.example.data.model.MockMusicData
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao())
    val playerManager = AudioPlayerManager(application)

    // --- State Flows ---
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0)
    val playbackProgress: StateFlow<Int> = _playbackProgress.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _selectedArtistName = MutableStateFlow<String?>(null)
    val selectedArtistName: StateFlow<String?> = _selectedArtistName.asStateFlow()

    fun selectArtist(artistName: String?) {
        _selectedArtistName.value = artistName
    }

    // --- Crossfade States ---
    private val playbackPrefs = application.getSharedPreferences("playback_prefs", android.content.Context.MODE_PRIVATE)
    private val _isCrossfadeEnabled = MutableStateFlow(
        playbackPrefs.getBoolean("is_crossfade_enabled", false)
    )
    val isCrossfadeEnabled: StateFlow<Boolean> = _isCrossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(
        playbackPrefs.getInt("crossfade_duration_seconds", 3)
    )
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private var isCrossfadeTriggered = false

    // --- Audio Format States ---
    private val _preferredAudioFormat = MutableStateFlow(
        playbackPrefs.getString("preferred_audio_format", "Lossless (ALAC)") ?: "Lossless (ALAC)"
    )
    val preferredAudioFormat: StateFlow<String> = _preferredAudioFormat.asStateFlow()

    fun setPreferredAudioFormat(format: String) {
        _preferredAudioFormat.value = format
        playbackPrefs.edit().putString("preferred_audio_format", format).apply()
    }

    // --- Offline Mode States ---
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // --- Equalizer States (exposing AudioPlayerManager functions) ---
    val isEqEnabled: Boolean get() = playerManager.isEqEnabled
    fun toggleEqualizer(enabled: Boolean) = playerManager.toggleEqualizer(enabled)
    fun getBandLevels(): Map<Short, Short> = playerManager.getBandLevels()
    fun setBandLevel(band: Short, levelMillibels: Short) = playerManager.setBandLevel(band, levelMillibels)
    fun getEqualizerPresets(): List<String> = playerManager.getEqualizerPresets()
    fun usePreset(presetIndex: Short) = playerManager.usePreset(presetIndex)
    fun getBandFrequencies(): List<Int> = playerManager.getBandFrequencies()
    fun getBandLevelRange(): Pair<Short, Short> = playerManager.getBandLevelRange()

    fun toggleCrossfade(enabled: Boolean) {
        _isCrossfadeEnabled.value = enabled
        playbackPrefs.edit().putBoolean("is_crossfade_enabled", enabled).apply()
    }

    fun setCrossfadeDuration(seconds: Int) {
        val coerced = seconds.coerceIn(1, 10)
        _crossfadeDurationSeconds.value = coerced
        playbackPrefs.edit().putInt("crossfade_duration_seconds", coerced).apply()
    }

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun getPlayableUrl(song: Song): String {
        val localFile = File(getApplication<Application>().filesDir, "downloads/${song.id}.mp3")
        return if (localFile.exists()) {
            localFile.absolutePath
        } else {
            song.url
        }
    }

    suspend fun getPlaylistsList(): List<com.example.data.database.PlaylistEntity> {
        return repository.allPlaylists.first()
    }

    suspend fun getSongsInPlaylistSynchronously(playlistId: Long): List<Song> {
        return repository.getSongsInPlaylist(playlistId).first()
    }

    // --- Dynamic Lyrics Support ---
    val lyricLines: StateFlow<List<LyricLine>> = _currentSong
        .map { it?.parseLyrics() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeLyricIndex: StateFlow<Int> = combine(_playbackProgress, lyricLines) { progressMs, lines ->
        if (lines.isEmpty()) return@combine -1
        // Find the line that corresponds to the current timestamp
        var index = -1
        for (i in lines.indices) {
            val line = lines[i]
            if (line.timeMs != -1L && progressMs >= line.timeMs) {
                index = i
            }
        }
        index
    }.stateIn(viewModelScope, SharingStarted.Lazily, -1)

    // Favorites monitoring for current song
    val isCurrentSongFavorite: StateFlow<Boolean> = _currentSong
        .flatMapLatest { song ->
            if (song == null) flowOf(false)
            else repository.isSongFavorite(song.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // User listening history and favorites cached flows for smart shuffle
    private val recentlyPlayedSongs = repository.recentlyPlayedSongs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val favoriteSongs = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var progressJob: Job? = null

    init {
        playerManager.setListeners(
            onPrepared = {
                _isLoading.value = false
                _isPlaying.value = true
                _duration.value = playerManager.getDuration()
                startProgressTicker()
                // Save to history
                _currentSong.value?.let { song ->
                    viewModelScope.launch {
                        repository.addSongToRecentlyPlayed(song)
                    }
                }
            },
            onComplete = {
                nextTrack(autoPlay = true)
            },
            onError = { _ ->
                _isLoading.value = false
                _isPlaying.value = false
                stopProgressTicker()
            }
        )
    }

    fun playSong(song: Song, customQueue: List<Song> = listOf(song)) {
        viewModelScope.launch {
            _currentSong.value = song
            _queue.value = customQueue
            _currentIndex.value = customQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            _isLoading.value = true
            _isPlaying.value = false
            _playbackProgress.value = 0
            _duration.value = if (song.isLiveRadio) 0 else song.duration * 1000
            stopProgressTicker()
            isCrossfadeTriggered = false
            playerManager.play(getPlayableUrl(song))
        }
    }

    fun togglePlayPause() {
        if (_currentSong.value == null) return
        viewModelScope.launch {
            if (playerManager.isPlaying()) {
                playerManager.pause()
                _isPlaying.value = false
                stopProgressTicker()
            } else {
                if (playerManager.isPrepared) {
                    playerManager.start()
                    _isPlaying.value = true
                    startProgressTicker()
                } else {
                    // Re-play from scratch
                    _currentSong.value?.let { playSong(it, _queue.value) }
                }
            }
        }
    }

    fun nextTrack(autoPlay: Boolean = true) {
        val q = _queue.value
        if (q.isEmpty()) return
        
        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            nextIdx = if (_isRepeat.value) 0 else -1
        }
        
        if (_isShuffle.value && q.size > 1) {
            // Smart weighted shuffle algorithm prioritizing listening history & favorites
            val recents = recentlyPlayedSongs.value.map { it.id }.toSet()
            val favorites = favoriteSongs.value.map { it.id }.toSet()
            val currentId = _currentSong.value?.id

            // Filter out current song to avoid immediate replay
            val candidates = q.filter { it.id != currentId }
            val sourceList = if (candidates.isNotEmpty()) candidates else q

            // Calculate weights for each song in the sourceList
            val weights = sourceList.map { song ->
                var weight = 10.0
                if (favorites.contains(song.id)) {
                    weight += 20.0
                }
                if (recents.contains(song.id)) {
                    weight += 15.0
                }
                // Also prioritize similar artist if current song is set
                _currentSong.value?.let { current ->
                    if (song.artist == current.artist) {
                        weight += 10.0
                    }
                }
                song to weight
            }

            // Weighted random selection
            val totalWeight = weights.sumOf { it.second }
            var randomValue = java.util.Random().nextDouble() * totalWeight
            var selectedSong: Song = sourceList.first()
            for (item in weights) {
                randomValue -= item.second
                if (randomValue <= 0) {
                    selectedSong = item.first
                    break
                }
            }
            nextIdx = q.indexOfFirst { it.id == selectedSong.id }.coerceAtLeast(0)
        }

        if (nextIdx != -1) {
            if (autoPlay) {
                playSong(q[nextIdx], q)
            } else {
                _currentSong.value = q[nextIdx]
                _currentIndex.value = nextIdx
                _playbackProgress.value = 0
                _duration.value = if (q[nextIdx].isLiveRadio) 0 else q[nextIdx].duration * 1000
                _isPlaying.value = false
                stopProgressTicker()
                playerManager.reset()
            }
        } else {
            // Queue finished
            _isPlaying.value = false
            _playbackProgress.value = 0
            stopProgressTicker()
            playerManager.reset()
        }
    }

    fun prevTrack() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If progress is > 3 seconds, restart the song
        if (_playbackProgress.value > 3000) {
            seekTo(0)
            return
        }

        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_isRepeat.value) q.size - 1 else 0
        }

        playSong(q[prevIdx], q)
    }

    fun seekTo(positionMs: Int) {
        if (_currentSong.value?.isLiveRadio == true) return // Cannot seek live streams
        viewModelScope.launch {
            playerManager.seekTo(positionMs)
            _playbackProgress.value = positionMs
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun toggleFavoriteCurrentSong() {
        val song = _currentSong.value ?: return
        val isFav = isCurrentSongFavorite.value
        viewModelScope.launch {
            repository.toggleFavorite(song, isFav)
        }
    }

    // --- Sleep Timer ---
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        
        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val remaining = _sleepTimerRemainingSeconds.value ?: break
                if (remaining <= 1) {
                    _sleepTimerRemainingSeconds.value = null
                    if (playerManager.isPlaying()) {
                        playerManager.pause()
                        _isPlaying.value = false
                        stopProgressTicker()
                    }
                    break
                } else {
                    _sleepTimerRemainingSeconds.value = remaining - 1
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val song = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, song)
            _queue.value = currentQueue
            
            // Adjust the current index if the currently playing song's index shifted
            val currentPlaying = _currentSong.value
            if (currentPlaying != null) {
                _currentIndex.value = currentQueue.indexOfFirst { it.id == currentPlaying.id }.coerceAtLeast(0)
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        if (_currentSong.value?.isLiveRadio == true) return // Live streams don't need ticker progress
        progressJob = viewModelScope.launch {
            while (true) {
                delay(250) // high precision ticker
                val currentPos = playerManager.getCurrentPosition()
                _playbackProgress.value = currentPos
                
                // Crossfade detection!
                if (_isCrossfadeEnabled.value) {
                    val duration = _duration.value
                    val limit = _crossfadeDurationSeconds.value * 1000
                    if (duration > 0 && currentPos > 0 && (duration - currentPos) <= limit) {
                        triggerCrossfadeNext()
                    }
                }
            }
        }
    }

    private fun triggerCrossfadeNext() {
        if (isCrossfadeTriggered) return
        isCrossfadeTriggered = true
        
        val q = _queue.value
        if (q.isEmpty()) return
        
        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            nextIdx = if (_isRepeat.value) 0 else -1
        }
        
        if (_isShuffle.value && q.size > 1) {
            val recents = recentlyPlayedSongs.value.map { it.id }.toSet()
            val favorites = favoriteSongs.value.map { it.id }.toSet()
            val currentId = _currentSong.value?.id
            val pool = q.filter { it.id != currentId }
            if (pool.isNotEmpty()) {
                val weights = pool.map { s ->
                    var w = 1.0
                    if (favorites.contains(s.id)) w += 1.5
                    if (recents.contains(s.id)) w -= 0.5
                    w.coerceAtLeast(0.1)
                }
                val totalWeight = weights.sum()
                var r = Math.random() * totalWeight
                var selectedIdx = 0
                for (i in weights.indices) {
                    r -= weights[i]
                    if (r <= 0) {
                        selectedIdx = i
                        break
                    }
                }
                nextIdx = q.indexOfFirst { it.id == pool[selectedIdx].id }
            }
        }
        
        if (nextIdx != -1) {
            val nextSong = q[nextIdx]
            _currentIndex.value = nextIdx
            
            viewModelScope.launch {
                _currentSong.value = nextSong
                _isLoading.value = true
                _isPlaying.value = false
                _playbackProgress.value = 0
                _duration.value = if (nextSong.isLiveRadio) 0 else nextSong.duration * 1000
                isCrossfadeTriggered = false // Reset trigger for next song
                
                val urlToPlay = getPlayableUrl(nextSong)
                playerManager.crossfadePlay(urlToPlay, _crossfadeDurationSeconds.value * 1000L) {
                    _isLoading.value = false
                    _isPlaying.value = true
                    _duration.value = playerManager.getDuration()
                    
                    // Save to history
                    viewModelScope.launch {
                        repository.addSongToRecentlyPlayed(nextSong)
                    }
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTicker()
        playerManager.release()
    }
}
