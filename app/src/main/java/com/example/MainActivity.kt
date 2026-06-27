package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.model.MockMusicData
import com.example.ui.components.AppShell
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PlaybackViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val playbackViewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleMusicIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppShell()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMusicIntent(intent)
    }

    private fun handleMusicIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        Log.d("MainActivity", "Received intent action: $action")
        
        if (action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH || action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(android.app.SearchManager.QUERY) ?: ""
            Log.d("MainActivity", "Voice/Search query received: $query")
            if (query.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // 1. Try matching Playlist title first
                        val playlists = playbackViewModel.getPlaylistsList()
                        val matchedPlaylist = playlists.find { it.title.contains(query, ignoreCase = true) }
                        if (matchedPlaylist != null) {
                            val playlistSongs = playbackViewModel.getSongsInPlaylistSynchronously(matchedPlaylist.id)
                            if (playlistSongs.isNotEmpty()) {
                                Log.d("MainActivity", "Voice command: playing matched playlist: ${matchedPlaylist.title}")
                                playbackViewModel.playSong(playlistSongs.first(), playlistSongs)
                                return@launch
                            }
                        }

                        // 2. Try matching Genre name
                        val matchingSongsByGenre = MockMusicData.songs.filter {
                            it.genre.contains(query, ignoreCase = true)
                        }
                        if (matchingSongsByGenre.isNotEmpty()) {
                            Log.d("MainActivity", "Voice command: playing genre: $query")
                            playbackViewModel.playSong(matchingSongsByGenre.first(), matchingSongsByGenre)
                            return@launch
                        }

                        // 3. Fallback to Song/Artist/Album matching
                        val match = MockMusicData.songs.find {
                            it.title.contains(query, ignoreCase = true) || 
                            it.artist.contains(query, ignoreCase = true) ||
                            it.album.contains(query, ignoreCase = true)
                        }
                        if (match != null) {
                            Log.d("MainActivity", "Voice command: playing track: ${match.title}")
                            playbackViewModel.playSong(match, MockMusicData.songs)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing voice search intent", e)
                    }
                }
            }
        }
    }
}
