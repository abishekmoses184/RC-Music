package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val artwork: String, // Dynamic gradient style
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String,
    val url: String,
    val duration: Int,
    val genre: String,
    val lyrics: String,
    val isLiveRadio: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
) {
    fun toSong(): Song = Song(
        id = songId,
        url = url,
        title = title,
        artist = artist,
        album = album,
        artwork = artwork,
        duration = duration,
        genre = genre,
        lyrics = lyrics,
        isLiveRadio = isLiveRadio
    )

    companion object {
        fun fromSong(playlistId: Long, song: Song, orderIndex: Int = 0): PlaylistTrackEntity = PlaylistTrackEntity(
            playlistId = playlistId,
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            artwork = song.artwork,
            url = song.url,
            duration = song.duration,
            genre = song.genre,
            lyrics = song.lyrics,
            isLiveRadio = song.isLiveRadio,
            orderIndex = orderIndex
        )
    }
}

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String,
    val url: String,
    val duration: Int,
    val genre: String,
    val lyrics: String,
    val isLiveRadio: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        url = url,
        title = title,
        artist = artist,
        album = album,
        artwork = artwork,
        duration = duration,
        genre = genre,
        lyrics = lyrics,
        isLiveRadio = isLiveRadio
    )

    companion object {
        fun fromSong(song: Song): FavoriteSongEntity = FavoriteSongEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            artwork = song.artwork,
            url = song.url,
            duration = song.duration,
            genre = song.genre,
            lyrics = song.lyrics,
            isLiveRadio = song.isLiveRadio
        )
    }
}

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String,
    val url: String,
    val duration: Int,
    val genre: String,
    val lyrics: String,
    val isLiveRadio: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        url = url,
        title = title,
        artist = artist,
        album = album,
        artwork = artwork,
        duration = duration,
        genre = genre,
        lyrics = lyrics,
        isLiveRadio = isLiveRadio
    )

    companion object {
        fun fromSong(song: Song): RecentlyPlayedEntity = RecentlyPlayedEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            artwork = song.artwork,
            url = song.url,
            duration = song.duration,
            genre = song.genre,
            lyrics = song.lyrics,
            isLiveRadio = song.isLiveRadio
        )
    }
}
