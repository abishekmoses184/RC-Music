package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.viewmodel.PlaybackViewModel
import com.example.viewmodel.LibraryViewModel

/**
 * Parses a CSS gradient string like "linear-gradient(135deg, #FF5E62 0%, #FF9966 100%)"
 * and returns a Compose Brush. Linear direction is simplified to standard diagonal.
 */
fun parseGradient(gradientStr: String): Brush {
    val hexRegex = Regex("#([A-Fa-f0-9]{6})")
    val matches = hexRegex.findAll(gradientStr).map { it.value }.toList()
    return if (matches.size >= 2) {
        try {
            val color1 = Color(android.graphics.Color.parseColor(matches[0]))
            val color2 = Color(android.graphics.Color.parseColor(matches[1]))
            Brush.linearGradient(colors = listOf(color1, color2))
        } catch (e: Exception) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFA2D48),
                    Color(0xFFFF5E79)
                )
            )
        }
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFA2D48),
                Color(0xFFFF5E79)
            )
        )
    }
}

@Composable
fun AlbumArtwork(
    artwork: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (artwork.startsWith("linear-gradient")) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(parseGradient(artwork))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            AsyncImage(
                model = artwork,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center
            )
        }
    }
}

/**
 * Triggers a satisfying tactile haptic bump for playback controls
 */
@Composable
fun rememberHapticTrigger(): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    return {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun TrackOptionsMenu(
    song: Song,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var expanded by remember { mutableStateOf(false) }
    var showArtistDialog by remember { mutableStateOf(false) }
    var showAlbumDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    
    val hapticClick = rememberHapticTrigger()
    val isAdded = libraryViewModel?.recentlyAdded?.collectAsState()?.value?.any { it.id == song.id } ?: false

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                hapticClick()
                expanded = true
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = tint
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Go to Artist") },
                onClick = {
                    hapticClick()
                    expanded = false
                    playbackViewModel.selectArtist(song.artist)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Go to Album") },
                onClick = {
                    hapticClick()
                    expanded = false
                    showAlbumDialog = true
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Show Lyrics") },
                onClick = {
                    hapticClick()
                    expanded = false
                    showLyricsDialog = true
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            if (libraryViewModel != null) {
                DropdownMenuItem(
                    text = { Text(if (isAdded) "Remove from Library" else "Add to Library") },
                    onClick = {
                        hapticClick()
                        expanded = false
                        if (isAdded) {
                            libraryViewModel.removeFromRecentlyAdded(song.id)
                        } else {
                            libraryViewModel.addToRecentlyAdded(song)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isAdded) Icons.Default.Delete else Icons.Default.LibraryAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }

    // Legacy dialog removed. ArtistDetailView handles this as a full overlay screen.

    if (showAlbumDialog) {
        val albumSongs = remember(song.album) {
            com.example.data.model.MockMusicData.songs.filter { it.album.equals(song.album, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { showAlbumDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            containerColor = Color(0xFF151419).copy(alpha = 0.95f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    AlbumArtwork(
                        artwork = song.artwork,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ALBUM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "by ${song.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Album Tracklist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        IconButton(
                            onClick = {
                                hapticClick()
                                showAlbumDialog = false
                                playbackViewModel.playSong(albumSongs.first(), albumSongs)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(albumSongs) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .clickable {
                                        hapticClick()
                                        showAlbumDialog = false
                                        playbackViewModel.playSong(s, albumSongs)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${albumSongs.indexOf(s) + 1}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${s.duration / 60}:${String.format("%02d", s.duration % 60)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAlbumDialog = false },
                    modifier = Modifier.padding(end = 16.dp, bottom = 8.dp)
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            containerColor = Color(0xFF151419).copy(alpha = 0.95f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            text = {
                val lyricLines = remember(song) { song.parseLyrics() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    // Soft glowing orb behind lyrics
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFA2D48).copy(alpha = 0.08f), Color.Transparent),
                                    radius = 300f
                                )
                            )
                    )

                    if (lyricLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "Lyrics aren't available for this track.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(lyricLines) { line ->
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLyricsDialog = false },
                    modifier = Modifier.padding(end = 16.dp, bottom = 8.dp)
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
