package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockMusicData
import com.example.data.model.Song
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.rememberHapticTrigger
import com.example.viewmodel.PlaybackViewModel

@Composable
fun ArtistDetailView(
    artistName: String,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticClick = rememberHapticTrigger()

    // 1. Fetch artist tracks
    val topTracks = remember(artistName) {
        MockMusicData.songs.filter { it.artist.contains(artistName, ignoreCase = true) }
    }

    // 2. Fetch artist albums
    val artistAlbums = remember(artistName) {
        MockMusicData.albums.filter { album ->
            album.artist.contains(artistName, ignoreCase = true) ||
                    album.tracks.any { it.artist.contains(artistName, ignoreCase = true) }
        }
    }

    // 3. Similar Artists Logic
    val similarArtists = remember(artistName) {
        getSimilarArtists(artistName)
    }

    // Generate a unique banner gradient based on artist name hash code
    val bannerGradient = remember(artistName) {
        val hash = artistName.hashCode()
        val colors = listOf(
            Color(0xFF000000L or (hash.toLong() and 0xFFFFFFL)),
            Color(0xFF000000L or ((hash * 31).toLong() and 0xFFFFFFL)).copy(alpha = 0.6f),
            Color(0xFF151419)
        )
        Brush.verticalGradient(colors)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E13))
            .testTag("artist_detail_screen")
    ) {
        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Image/Gradient Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Gradient Background representation of artist identity
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bannerGradient)
                    )
                    
                    // Dark scrim overlay for high-contrast typography
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Transparent,
                                        Color(0xFF0F0E13)
                                    )
                                )
                            )
                    )

                    // Hero Text Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "VERIFIED ARTIST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${topTracks.size} songs • ${artistAlbums.size} albums",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Quick Actions: Play & Shuffle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            hapticClick()
                            if (topTracks.isNotEmpty()) {
                                playbackViewModel.playSong(topTracks.first(), topTracks)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("artist_play_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            hapticClick()
                            if (topTracks.isNotEmpty()) {
                                val shuffled = topTracks.shuffled()
                                playbackViewModel.playSong(shuffled.first(), shuffled)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("artist_shuffle_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section: Top Tracks
            item {
                Text(
                    text = "Top Tracks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
                )
            }

            if (topTracks.isEmpty()) {
                item {
                    Text(
                        text = "No tracks available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(topTracks.take(5)) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hapticClick()
                                playbackViewModel.playSong(song, topTracks)
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtwork(
                            artwork = song.artwork,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Subtle track index / rating circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Section: Albums
            if (artistAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp)
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(artistAlbums) { album ->
                            Column(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        hapticClick()
                                        if (album.tracks.isNotEmpty()) {
                                            playbackViewModel.playSong(album.tracks.first(), album.tracks)
                                        }
                                    }
                            ) {
                                AlbumArtwork(
                                    artwork = album.artwork,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.releaseYear.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Section: Similar Artists
            if (similarArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "Similar Artists",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(similarArtists) { name ->
                            Column(
                                modifier = Modifier
                                    .width(100.dp)
                                    .clickable {
                                        hapticClick()
                                        playbackViewModel.selectArtist(name)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Dynamic avatar representation using hash-gradient circle
                                val avatarHash = name.hashCode()
                                val avatarGradient = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF000000L or (avatarHash.toLong() and 0xFFFFFFL)),
                                        Color(0xFF000000L or ((avatarHash * 7).toLong() and 0xFFFFFFL)),
                                        Color(0xFF000000L or (avatarHash.toLong() and 0xFFFFFFL))
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(avatarGradient)
                                        .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(2).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            FilledIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
}

private fun getSimilarArtists(artistName: String): List<String> {
    val jArtists = listOf("LiSA", "ReoNa", "Haruka Tomatsu", "Eir Aoi", "Luna Haruna", "FLOW")
    val popArtists = listOf("Pitbull", "The Chainsmokers", "Charlie Puth", "Priyanka Chopra", "Demi Lovato")

    return if (jArtists.any { it.equals(artistName, ignoreCase = true) }) {
        jArtists.filter { !it.equals(artistName, ignoreCase = true) }.shuffled().take(3)
    } else if (popArtists.any { it.equals(artistName, ignoreCase = true) }) {
        popArtists.filter { !it.equals(artistName, ignoreCase = true) }.shuffled().take(3)
    } else {
        (jArtists + popArtists).filter { !it.equals(artistName, ignoreCase = true) }.shuffled().take(3)
    }
}
