package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockMusicData
import com.example.data.model.Song
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.rememberHapticTrigger
import com.example.ui.components.TrackOptionsMenu
import com.example.viewmodel.PlaybackViewModel
import com.example.viewmodel.LibraryViewModel

data class SearchCategory(val title: String, val brush: Brush)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val hapticClick = rememberHapticTrigger()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val searchPrefs = remember { context.getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Historical queries stored locally in SharedPreferences
    val searchHistory = remember {
        val savedString = searchPrefs.getString("search_history_csv", "acoustic,chillwave,synth") ?: "acoustic,chillwave,synth"
        val list = if (savedString.isBlank()) emptyList() else savedString.split(",")
        mutableStateListOf<String>().apply {
            addAll(list)
        }
    }

    // Save search history helper
    val saveSearchHistory = {
        val csv = searchHistory.joinToString(",")
        searchPrefs.edit().putString("search_history_csv", csv).apply()
    }

    val filteredSongs = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            MockMusicData.songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true)
            }
        }
    }

    val categories = remember {
        listOf(
            SearchCategory("Pop", Brush.linearGradient(colors = listOf(Color(0xFFf857a6), Color(0xFFff5858)))),
            SearchCategory("Electronic", Brush.linearGradient(colors = listOf(Color(0xFF1D976C), Color(0xFF93F9B9)))),
            SearchCategory("Chillwave", Brush.linearGradient(colors = listOf(Color(0xFF00c6ff), Color(0xFF0072ff)))),
            SearchCategory("Acoustic", Brush.linearGradient(colors = listOf(Color(0xFFF3904F), Color(0xFF3B4371)))),
            SearchCategory("Jazz & Lounge", Brush.linearGradient(colors = listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)))),
            SearchCategory("Alternative", Brush.linearGradient(colors = listOf(Color(0xFFf12711), Color(0xFFf5af19))))
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title - Beautiful Liquid Glass Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF5E79).copy(alpha = 0.22f), Color.Transparent),
                                radius = size.width * 0.5f
                            ),
                            radius = size.width * 0.5f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.5f)
                        )
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        modifier = Modifier.testTag("search_header")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Find your favorite songs, artists, and global genres instantly",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Search Bar Text Field
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                placeholder = { Text(text = "Artists, Songs, Lyrics, More") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }

        // Search Results / Suggestions
        if (query.isNotBlank()) {
            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for \"$query\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredSongs) { song ->
                    SearchResultRow(
                        song = song,
                        playbackViewModel = playbackViewModel,
                        libraryViewModel = libraryViewModel
                    ) {
                        hapticClick()
                        // Add query to history
                        if (!searchHistory.contains(query.trim())) {
                            searchHistory.add(0, query.trim())
                            saveSearchHistory()
                        }
                        playbackViewModel.playSong(song, filteredSongs)
                    }
                }
            }
        } else {
            // History section
            if (searchHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(searchHistory.take(5)) { hist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hapticClick()
                                query = hist
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = hist,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                hapticClick()
                                searchHistory.remove(hist)
                                saveSearchHistory()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Genre Cards grid
            item {
                Text(
                    text = "Search Categories",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val chunked = categories.chunked(2)
                    chunked.forEach { rowCategories ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowCategories.forEach { category ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(category.brush)
                                        .clickable {
                                            hapticClick()
                                            query = category.title
                                            if (!searchHistory.contains(category.title)) {
                                                searchHistory.add(0, category.title)
                                                saveSearchHistory()
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = category.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    song: Song,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_${song.id}")
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtwork(
            artwork = song.artwork,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = song.genre,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TrackOptionsMenu(
            song = song,
            playbackViewModel = playbackViewModel,
            libraryViewModel = libraryViewModel
        )
    }
}
