package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    private var onPreparedListener: (() -> Unit)? = null

    var isPrepared = false
        private set

    private var crossfadeJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // --- Equalizer Native Support ---
    var isEqEnabled = false
        private set
    private val savedBandLevels = mutableMapOf<Short, Short>() // Band -> Level in millibels (mB)
    private var equalizer: Equalizer? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener {
                    isPrepared = true
                    start()
                    onPreparedListener?.invoke()
                }
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                    Log.e("AudioPlayerManager", errorMsg)
                    onErrorListener?.invoke(errorMsg)
                    isPrepared = false
                    // Reset on error
                    reset()
                    true
                }
            }
            mediaPlayer?.let { initEqualizerForPlayer(it) }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize MediaPlayer", e)
        }
    }

    fun setListeners(
        onPrepared: () -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        onPreparedListener = onPrepared
        onCompletionListener = onComplete
        onErrorListener = onError
    }

    fun play(url: String) {
        try {
            isPrepared = false
            mediaPlayer?.apply {
                stop()
                reset()
                // Re-apply attributes after reset
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (url.startsWith("/")) {
                    setDataSource(url)
                } else {
                    setDataSource(context, Uri.parse(url))
                }
                prepareAsync() // Network audio must be prepared asynchronously!
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing URL: $url", e)
            onErrorListener?.invoke("Error playing URL: ${e.localizedMessage}")
        }
    }

    fun start() {
        if (isPrepared) {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error starting playback", e)
            }
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing playback", e)
        }
    }

    fun stop() {
        try {
            crossfadeJob?.cancel()
            mediaPlayer?.stop()
            isPrepared = false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping playback", e)
        }
    }

    fun seekTo(positionMs: Int) {
        if (isPrepared) {
            try {
                mediaPlayer?.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error seeking to $positionMs", e)
            }
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            if (isPrepared) mediaPlayer?.currentPosition ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDuration(): Int {
        return try {
            if (isPrepared) mediaPlayer?.duration ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun reset() {
        try {
            crossfadeJob?.cancel()
            mediaPlayer?.reset()
            isPrepared = false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error resetting MediaPlayer", e)
        }
    }

    fun release() {
        try {
            crossfadeJob?.cancel()
            mediaPlayer?.release()
            mediaPlayer = null
            isPrepared = false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing MediaPlayer", e)
        }
    }

    fun crossfadePlay(url: String, crossfadeDurationMs: Long, onPreparedCallback: () -> Unit) {
        try {
            crossfadeJob?.cancel()
            
            // 1. Create the new MediaPlayer (nextPlayer)
            val nextPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (url.startsWith("/")) {
                    setDataSource(url)
                } else {
                    setDataSource(context, Uri.parse(url))
                }
                setVolume(0f, 0f) // Start completely silent
            }
            
            nextPlayer.setOnPreparedListener {
                isPrepared = true
                initEqualizerForPlayer(nextPlayer)
                nextPlayer.start()
                onPreparedCallback()
                
                // 2. Perform smooth crossfade
                crossfadeJob = mainScope.launch {
                    val steps = 20
                    val interval = crossfadeDurationMs / steps
                    val oldPlayer = mediaPlayer
                    
                    for (i in 1..steps) {
                        val progress = i.toFloat() / steps // from 0.0f to 1.0f
                        
                        try {
                            nextPlayer.setVolume(progress, progress)
                        } catch (e: Exception) {
                            Log.e("AudioPlayerManager", "Error setting crossfade volume", e)
                        }
                        
                        if (oldPlayer != null) {
                            try {
                                if (oldPlayer.isPlaying) {
                                    val oldVolume = 1.0f - progress
                                    oldPlayer.setVolume(oldVolume, oldVolume)
                                }
                            } catch (e: Exception) {
                                Log.e("AudioPlayerManager", "Error setting old player volume during crossfade", e)
                            }
                        }
                        
                        delay(interval)
                    }
                    
                    // 3. Clean up the old player
                    if (oldPlayer != null) {
                        try {
                            if (oldPlayer.isPlaying) {
                                oldPlayer.stop()
                            }
                            oldPlayer.release()
                        } catch (e: Exception) {
                            Log.e("AudioPlayerManager", "Error cleaning up old player", e)
                        }
                    }
                    
                    // Set nextPlayer as the active mediaPlayer
                    mediaPlayer = nextPlayer
                    // Re-register completion listener on active player
                    mediaPlayer?.setOnCompletionListener {
                        onCompletionListener?.invoke()
                    }
                    mediaPlayer?.setOnErrorListener { _, what, extra ->
                        val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                        Log.e("AudioPlayerManager", errorMsg)
                        onErrorListener?.invoke(errorMsg)
                        isPrepared = false
                        reset()
                        true
                    }
                }
            }

            nextPlayer.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayerManager", "nextPlayer Error preparing: what=$what, extra=$extra")
                false
            }
            
            nextPlayer.prepareAsync()
            
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error in crossfadePlay", e)
            onErrorListener?.invoke("Error playing crossfade URL: ${e.localizedMessage}")
        }
    }

    private fun initEqualizerForPlayer(player: MediaPlayer) {
        try {
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                equalizer?.release()
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = isEqEnabled
                    // Apply currently saved band levels
                    savedBandLevels.forEach { (band, level) ->
                        try {
                            setBandLevel(band, level)
                        } catch (e: Exception) {
                            Log.e("AudioPlayerManager", "Error setting band level", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to init Equalizer for session", e)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        isEqEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error toggling equalizer", e)
        }
    }

    fun getBandLevels(): Map<Short, Short> {
        val map = mutableMapOf<Short, Short>()
        val eq = equalizer
        if (eq != null) {
            for (band in 0 until eq.numberOfBands) {
                map[band.toShort()] = try { eq.getBandLevel(band.toShort()) } catch (e: Exception) { 0.toShort() }
            }
        } else {
            savedBandLevels.forEach { (band, level) ->
                map[band] = level
            }
        }
        return map
    }

    fun setBandLevel(band: Short, levelMillibels: Short) {
        savedBandLevels[band] = levelMillibels
        try {
            equalizer?.setBandLevel(band, levelMillibels)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error setting band level on active equalizer", e)
        }
    }

    fun getEqualizerPresets(): List<String> {
        val list = mutableListOf<String>()
        val eq = equalizer ?: try {
            Equalizer(0, 1) // Temporary just to get presets
        } catch (e: Exception) {
            null
        }
        if (eq != null) {
            try {
                for (p in 0 until eq.numberOfPresets) {
                    list.add(eq.getPresetName(p.toShort()))
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error getting presets", e)
            } finally {
                if (eq != equalizer) {
                    eq.release()
                }
            }
        }
        return list
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            // Save the resulting band levels
            val eq = equalizer
            if (eq != null) {
                for (band in 0 until eq.numberOfBands) {
                    savedBandLevels[band.toShort()] = eq.getBandLevel(band.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error setting preset $presetIndex", e)
        }
    }

    fun getBandFrequencies(): List<Int> {
        val list = mutableListOf<Int>()
        val eq = equalizer ?: try { Equalizer(0, 1) } catch (e: Exception) { null }
        if (eq != null) {
            try {
                for (band in 0 until eq.numberOfBands) {
                    list.add(eq.getCenterFreq(band.toShort()) / 1000) // Convert to Hz
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error getting band freqs", e)
            } finally {
                if (eq != equalizer) {
                    eq.release()
                }
            }
        }
        if (list.isEmpty()) {
            // Default 5-band frequencies in case Equalizer couldn't be initialized
            return listOf(60, 230, 910, 4000, 14000)
        }
        return list
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        return try {
            val range = equalizer?.bandLevelRange
            if (range != null && range.size >= 2) {
                Pair(range[0], range[1])
            } else {
                Pair((-1500).toShort(), 1500.toShort())
            }
        } catch (e: Exception) {
            Pair((-1500).toShort(), 1500.toShort())
        }
    }
}
