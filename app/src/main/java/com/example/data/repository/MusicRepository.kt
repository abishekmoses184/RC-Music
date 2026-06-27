package com.example.data.repository

import com.example.data.database.MusicDao
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistTrackEntity
import com.example.data.database.FavoriteSongEntity
import com.example.data.database.RecentlyPlayedEntity
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(private val musicDao: MusicDao) {

    // --- Playlists ---
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()

    suspend fun createPlaylist(title: String, description: String, artwork: String): Long {
        return musicDao.insertPlaylist(
            PlaylistEntity(title = title, description = description, artwork = artwork)
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        musicDao.deleteTracksForPlaylist(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity? {
        return musicDao.getPlaylistById(playlistId)
    }

    // --- Playlist Tracks ---
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return musicDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val currentTracks = musicDao.getTracksForPlaylistSync(playlistId)
        val nextIndex = (currentTracks.maxOfOrNull { it.orderIndex } ?: -1) + 1
        musicDao.insertPlaylistTrack(PlaylistTrackEntity.fromSong(playlistId, song, nextIndex))
    }

    suspend fun reorderPlaylistTracks(playlistId: Long, songIds: List<String>) {
        songIds.forEachIndexed { index, songId ->
            musicDao.updateTrackOrderIndex(playlistId, songId, index)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        musicDao.deletePlaylistTrackBySong(playlistId, songId)
    }

    // --- Favorites ---
    val favoriteSongs: Flow<List<Song>> = musicDao.getAllFavorites().map { entities ->
        entities.map { it.toSong() }
    }

    fun isSongFavorite(songId: String): Flow<Boolean> {
        return musicDao.isFavorite(songId)
    }

    suspend fun addSongToFavorites(song: Song) {
        musicDao.insertFavorite(FavoriteSongEntity.fromSong(song))
    }

    suspend fun removeSongFromFavorites(songId: String) {
        musicDao.deleteFavorite(songId)
    }

    suspend fun toggleFavorite(song: Song, isFav: Boolean) {
        if (isFav) {
            removeSongFromFavorites(song.id)
        } else {
            addSongToFavorites(song)
        }
    }

    // --- Recently Played ---
    val recentlyPlayedSongs: Flow<List<Song>> = musicDao.getRecentlyPlayed().map { entities ->
        entities.map { it.toSong() }
    }

    suspend fun addSongToRecentlyPlayed(song: Song) {
        // First delete it if it already exists, so it bumps to the top
        musicDao.deleteRecentlyPlayed(song.id)
        musicDao.insertRecentlyPlayed(RecentlyPlayedEntity.fromSong(song))
    }

    suspend fun clearHistory() {
        musicDao.clearRecentlyPlayed()
    }
}
