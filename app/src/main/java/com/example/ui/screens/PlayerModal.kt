package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricLine
import com.example.data.model.Song
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.parseGradient
import com.example.ui.components.rememberHapticTrigger
import com.example.viewmodel.PlaybackViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerModal(
    playbackViewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    libraryViewModel: com.example.viewmodel.LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentSong by playbackViewModel.currentSong.collectAsState()
    val isPlaying by playbackViewModel.isPlaying.collectAsState()
    val isLoading by playbackViewModel.isLoading.collectAsState()
    val progress by playbackViewModel.playbackProgress.collectAsState()
    val duration by playbackViewModel.duration.collectAsState()
    val isShuffle by playbackViewModel.isShuffle.collectAsState()
    val isRepeat by playbackViewModel.isRepeat.collectAsState()
    val isFav by playbackViewModel.isCurrentSongFavorite.collectAsState()
    
    val sleepTimerRemaining by playbackViewModel.sleepTimerRemainingSeconds.collectAsState()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    val playlists by libraryViewModel.playlists.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val lyricLines by playbackViewModel.lyricLines.collectAsState()
    val activeLyricIndex by playbackViewModel.activeLyricIndex.collectAsState()

    val hapticClick = rememberHapticTrigger()
    val coroutineScope = rememberCoroutineScope()
    
    val density = LocalDensity.current
    val translationYAnim = remember { Animatable(0f) }
    var screenHeightPx by remember { mutableStateOf(1000f) }
    var dragStartTime by remember { mutableStateOf(0L) }

    // Tab state: 0 for Album art, 1 for Scrolling Lyrics, 2 for Up Next queue, 3 for Settings
    var showLyricsTab by remember { mutableStateOf(false) }
    var showQueueTab by remember { mutableStateOf(false) }
    var showSettingsTab by remember { mutableStateOf(false) }

    val lyricsListState = rememberLazyListState()

    val dynamicBlurRadius by remember {
        derivedStateOf {
            if (!showLyricsTab) {
                25.dp
            } else {
                val firstVisibleIndex = lyricsListState.firstVisibleItemIndex
                val firstVisibleScrollOffset = lyricsListState.firstVisibleItemScrollOffset
                val totalScrollOffset = firstVisibleIndex * 150f + firstVisibleScrollOffset
                val calculatedRadius = (25f + (totalScrollOffset / 40f)).coerceIn(25f, 60f)
                calculatedRadius.dp
            }
        }
    }

    val dynamicOverlayAlpha by remember {
        derivedStateOf {
            if (!showLyricsTab) {
                0.35f
            } else {
                val firstVisibleIndex = lyricsListState.firstVisibleItemIndex
                val firstVisibleScrollOffset = lyricsListState.firstVisibleItemScrollOffset
                val totalScrollOffset = firstVisibleIndex * 150f + firstVisibleScrollOffset
                (0.3f + (totalScrollOffset / 3000f)).coerceIn(0.3f, 0.7f)
            }
        }
    }

    if (currentSong == null) return

    val song = currentSong!!

    // Smooth background gradient based on the song's artwork
    val backgroundBrush = remember(song.artwork) {
        if (song.artwork.startsWith("linear-gradient")) {
            parseGradient(song.artwork)
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF33080F), Color(0xFF101010))
            )
        }
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Text(
                    text = "Set Sleep Timer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val options = listOf(
                        "Off" to 0,
                        "5 Minutes" to 5,
                        "15 Minutes" to 15,
                        "30 Minutes" to 30,
                        "45 Minutes" to 45,
                        "60 Minutes" to 60
                    )
                    options.forEach { (label, minutes) ->
                        Button(
                            onClick = {
                                hapticClick()
                                if (minutes == 0) {
                                    playbackViewModel.cancelSleepTimer()
                                } else {
                                    playbackViewModel.setSleepTimer(minutes)
                                }
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sleep_timer_option_$minutes"),
                            colors = if ((minutes == 0 && sleepTimerRemaining == null) || 
                                         (minutes > 0 && sleepTimerRemaining != null && sleepTimerRemaining!! / 60 == minutes)) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        ) {
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                    modifier = Modifier.testTag("sleep_timer_dialog_close")
                ) {
                    Text("Close")
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_modal_sheet")
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        LaunchedEffect(heightPx) {
            screenHeightPx = heightPx
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = if (screenHeightPx > 0f) (translationYAnim.value / screenHeightPx).coerceIn(0f, 1f) else 0f
                    translationY = translationYAnim.value
                    // Create an elegant, physics-based layered card deck depth effect
                    val scaleFactor = 1f - (progress * 0.08f)
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    alpha = 1f - (progress * 0.5f)
                }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragStartTime = System.currentTimeMillis()
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Only allow downwards swipe for dismiss (positive translationY)
                                val newValue = (translationYAnim.value + dragAmount).coerceAtLeast(0f)
                                translationYAnim.snapTo(newValue)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val dragVelocity = if (dragDuration > 0) translationYAnim.value / dragDuration else 0f
                                
                                // High fidelity: dismisses if swiped down > 25% height or flung quickly (> 1.2 px/ms)
                                if (translationYAnim.value > screenHeightPx * 0.25f || dragVelocity > 1.2f) {
                                    translationYAnim.animateTo(
                                        targetValue = screenHeightPx,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onDismiss()
                                } else {
                                    // Bounces back to resting position with classic iOS style spring physics
                                    translationYAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            // Translucent/glowing background layer with dynamic blur and dim overlay based on scroll position
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(dynamicBlurRadius)
            ) {
                // 1. Base Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                )
                // 2. Overlapping glowing liquid accent blobs to simulate active fluids
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Top left liquid blob
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFA2D48).copy(alpha = 0.35f), Color.Transparent),
                                    radius = size.width * 0.9f
                                ),
                                radius = size.width * 0.9f,
                                center = androidx.compose.ui.geometry.Offset(0f, 0f)
                            )
                            // Bottom right liquid blob
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF5E79).copy(alpha = 0.25f), Color.Transparent),
                                    radius = size.width * 1.1f
                                ),
                                radius = size.width * 1.1f,
                                center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            )
                        }
                )
            }

            // Satin translucent gloss overlay sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = dynamicOverlayAlpha)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Visual Drag Handle Pill for premium iOS feel
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .testTag("player_drag_handle")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            hapticClick()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("dismiss_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize Player",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Queue / Up Next button
                        IconButton(
                            onClick = {
                                hapticClick()
                                showQueueTab = !showQueueTab
                                if (showQueueTab) {
                                    showLyricsTab = false
                                    showSettingsTab = false
                                }
                            },
                            modifier = Modifier.testTag("toggle_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Up Next Queue",
                                tint = if (showQueueTab) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Lyrics toggle button
                        IconButton(
                            onClick = {
                                hapticClick()
                                showLyricsTab = !showLyricsTab
                                if (showLyricsTab) {
                                    showQueueTab = false
                                    showSettingsTab = false
                                }
                            },
                            modifier = Modifier.testTag("toggle_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatAlignLeft,
                                contentDescription = "Lyrics Toggle",
                                tint = if (showLyricsTab) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Settings / Equalizer toggle button
                        IconButton(
                            onClick = {
                                hapticClick()
                                showSettingsTab = !showSettingsTab
                                if (showSettingsTab) {
                                    showQueueTab = false
                                    showLyricsTab = false
                                }
                            },
                            modifier = Modifier.testTag("toggle_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings & EQ",
                                tint = if (showSettingsTab) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }

                // 2. Middle panel (Art OR Lyrics OR Queue)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = when {
                            showQueueTab -> 2
                            showLyricsTab -> 1
                            showSettingsTab -> 3
                            else -> 0
                        },
                        transitionSpec = {
                            fadeIn(spring()) togetherWith fadeOut(spring())
                        },
                        label = "PlayerContentTransition"
                    ) { activeScreen ->
                        when (activeScreen) {
                            2 -> {
                                UpNextQueueView(
                                    playbackViewModel = playbackViewModel,
                                    onSelectSong = { targetSong ->
                                        playbackViewModel.playSong(targetSong, playbackViewModel.queue.value)
                                    }
                                )
                            }
                            1 -> {
                                PlayerLyricsView(
                                    lyricLines = lyricLines,
                                    activeLyricIndex = activeLyricIndex,
                                    listState = lyricsListState,
                                    onSelectLine = { line ->
                                        if (line.timeMs != -1L) {
                                            hapticClick()
                                            playbackViewModel.seekTo(line.timeMs.toInt())
                                        }
                                    }
                                )
                            }
                            3 -> {
                                PlayerSettingsView(
                                    playbackViewModel = playbackViewModel
                                )
                            }
                            else -> {
                                PlayerArtworkView(
                                    artwork = song.artwork,
                                    isPlaying = isPlaying && !isLoading
                                )
                            }
                        }
                    }
                }

                // 3. Audio Controls & Information details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Song Title, Artist & Favorite heart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 24.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("player_song_title")
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val preferredFormat by playbackViewModel.preferredAudioFormat.collectAsState()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val badgeText = when {
                                    preferredFormat.contains("Hi-Res") -> "Hi-Res Lossless"
                                    preferredFormat.contains("Lossless") -> "Lossless"
                                    preferredFormat.contains("Dolby") -> "Dolby Atmos"
                                    else -> "AAC 256kbps"
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (badgeText != "AAC 256kbps") {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Apple Digital Master",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    hapticClick()
                                    playbackViewModel.toggleFavoriteCurrentSong()
                                },
                                modifier = Modifier.testTag("player_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite Song",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    hapticClick()
                                    showPlaylistSheet = true
                                },
                                modifier = Modifier.testTag("player_playlist_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Save to Playlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    hapticClick()
                                    try {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sharing Song: ${song.title}")
                                            val shareMessage = "Check out this song!\n\nTitle: ${song.title}\nArtist: ${song.artist}\nAlbum: ${song.album}\nListen here: ${song.url}"
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Song via"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Sharing is not supported on this device.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("player_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Song",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar Seek Slider
                    if (!song.isLiveRadio) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = progress.toFloat().coerceAtMost(duration.toFloat()),
                                onValueChange = {
                                    coroutineScope.launch {
                                        playbackViewModel.seekTo(it.toInt())
                                    }
                                },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                    thumbColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("player_slider")
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(progress),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // Live radio stream visualizer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LIVE BROADCAST",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Control Bar Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                hapticClick()
                                playbackViewModel.toggleShuffle()
                            },
                            modifier = Modifier.testTag("player_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                hapticClick()
                                playbackViewModel.prevTrack()
                            },
                            modifier = Modifier.testTag("player_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Large central play capsule button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    hapticClick()
                                    playbackViewModel.togglePlayPause()
                                }
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                hapticClick()
                                playbackViewModel.nextTrack()
                            },
                            modifier = Modifier.testTag("player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                hapticClick()
                                playbackViewModel.toggleRepeat()
                            },
                            modifier = Modifier.testTag("player_repeat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeat) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Control Row (Device Router & Sleep Timer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Device selection button (Cast Icon)
                        IconButton(
                            onClick = {
                                hapticClick()
                                try {
                                    val intent = android.content.Intent("android.settings.panel.action.MEDIA_OUTPUT").apply {
                                        putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                        context.startActivity(fallbackIntent)
                                    } catch (ex: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "System audio switcher is not supported.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("player_device_route_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Select Device",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Sleep Timer Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable {
                                    hapticClick()
                                    showSleepTimerDialog = true
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("player_sleep_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sleepTimerRemaining != null) {
                                    val mins = sleepTimerRemaining!! / 60
                                    val secs = sleepTimerRemaining!! % 60
                                    String.format("%02d:%02d", mins, secs)
                                } else {
                                    "Sleep Timer"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    fontWeight = if (sleepTimerRemaining != null) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showPlaylistSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("save_to_playlist_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Save to Playlist",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(
                            onClick = {
                                showCreatePlaylistDialog = true
                            },
                            modifier = Modifier.testTag("create_playlist_from_sheet_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Playlist")
                        }
                    }
                    
                    if (playlists.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No playlists found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                modifier = Modifier.testTag("create_playlist_empty_state_button")
                            ) {
                                Text("Create Playlist")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            hapticClick()
                                            currentSong?.let { song ->
                                                libraryViewModel.addSongToPlaylist(playlist.id, song)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Added \"${song.title}\" to \"${playlist.title}\"",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            showPlaylistSheet = false
                                        }
                                        .padding(12.dp)
                                        .testTag("playlist_item_${playlist.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (!playlist.description.isNullOrEmpty()) {
                                            Text(
                                                text = playlist.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        if (showCreatePlaylistDialog) {
            var newPlaylistName by remember { mutableStateOf("") }
            var newPlaylistDesc by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Title") },
                            modifier = Modifier.fillMaxWidth().testTag("new_playlist_name_input")
                        )
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_playlist_desc_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                libraryViewModel.createPlaylist(newPlaylistName, newPlaylistDesc)
                                showCreatePlaylistDialog = false
                            }
                        },
                        enabled = newPlaylistName.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_playlist_button")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreatePlaylistDialog = false },
                        modifier = Modifier.testTag("dismiss_create_playlist_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private data class VisualizerBandConfig(
    val minVal: Float,
    val maxVal: Float,
    val minDuration: Int,
    val maxDuration: Int
)

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val barValues = remember { List(barCount) { Animatable(0.2f) } }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            barValues.forEachIndexed { index, animatable ->
                launch {
                    val delayTime = (index * 20).toLong()
                    delay(delayTime)
                    while (isActive) {
                        // Simulate different frequency bands (Bass, Mid-range, Treble)
                        val config = when {
                            index < 5 -> { // Bass: High amplitude, thumping rhythm
                                VisualizerBandConfig(0.25f, 0.95f, 130, 240)
                            }
                            index in 5..12 -> { // Mids: Active voice, medium-high amplitude
                                VisualizerBandConfig(0.2f, 0.75f, 90, 160)
                            }
                            else -> { // Treble: Fast, smaller spikes
                                VisualizerBandConfig(0.15f, 0.55f, 60, 110)
                            }
                        }
                        val target = (config.minVal + Math.random().toFloat() * (config.maxVal - config.minVal)).coerceIn(0.15f, 1.0f)
                        val duration = config.minDuration + (Math.random() * (config.maxDuration - config.minDuration)).toInt()
                        
                        animatable.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                        )
                        // Bass has a tiny beat pause sometimes
                        if (index < 5 && Math.random() > 0.7) {
                            delay((40 + Math.random() * 80).toLong())
                        }
                    }
                }
            }
        } else {
            barValues.forEach { animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 0.15f,
                        animationSpec = tween(durationMillis = 400)
                    )
                }
            }
        }
    }

    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barValues.forEachIndexed { index, animatable ->
            // Beautiful dynamic gradient mapping: Left/Bass is Hot Pink, Mid is Purple, Right/Treble is Cyan!
            val barColor = remember(index) {
                when {
                    index < 5 -> Color(0xFFFA2D48) // Vibrant Pink-Red
                    index in 5..12 -> Color(0xFF9F2DF0) // Electrifying Violet
                    else -> Color(0xFF00E5FF) // Cyberspace Cyan
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatable.value)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun PlayerArtworkView(
    artwork: String,
    isPlaying: Boolean
) {
    // Dynamic scaling: expands and gains a deep shadow on play, shrinks slightly on pause
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "ArtworkSizing"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = artwork,
            animationSpec = tween(500),
            label = "ArtworkCrossfade"
        ) { currentArtwork ->
            AlbumArtwork(
                artwork = currentArtwork,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(artworkScale)
                    .clip(RoundedCornerShape(20.dp)),
                elevation = 16.dp
            )
        }

        // Animated Audio Visualizer overlay at the bottom-center of the Artwork
        AudioVisualizer(
            isPlaying = isPlaying,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.55f)
                .height(36.dp)
                .padding(bottom = 12.dp)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                }
        )
    }
}

@Composable
fun PlayerLyricsView(
    lyricLines: List<LyricLine>,
    activeLyricIndex: Int,
    listState: LazyListState,
    onSelectLine: (LyricLine) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Automatically scrolls to keep the active lyric line centered smoothly
    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex >= 0 && lyricLines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = activeLyricIndex,
                    scrollOffset = -150 // offsets scrolling to center of screen
                )
            }
        }
    }

    if (lyricLines.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lyrics not available",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(vertical = 120.dp)
        ) {
            itemsIndexed(lyricLines) { index, line ->
                val isActive = index == activeLyricIndex
                
                // Highlight active lyrics line, fade secondary lines
                val alpha by animateFloatAsState(targetValue = if (isActive) 1f else 0.4f)
                val scale by animateFloatAsState(targetValue = if (isActive) 1.05f else 1f)

                Text(
                    text = line.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = if (isActive) Color.White else Color.White.copy(alpha = alpha),
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = if (isActive) 22.sp else 18.sp,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onSelectLine(line) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "$mins:${String.format("%02d", secs)}"
}

@Composable
fun UpNextQueueView(
    playbackViewModel: PlaybackViewModel,
    onSelectSong: (Song) -> Unit
) {
    val queue by playbackViewModel.queue.collectAsState()
    val currentIndex by playbackViewModel.currentIndex.collectAsState()
    val haptic = rememberHapticTrigger()
    
    // Upcoming songs are those after the current index
    val upcomingSongs = remember(queue, currentIndex) {
        if (currentIndex + 1 < queue.size) {
            queue.subList(currentIndex + 1, queue.size)
        } else {
            emptyList()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (upcomingSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming songs in queue",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = upcomingSongs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val absoluteIndex = currentIndex + 1 + index
                    var dragOffsetY by remember { mutableStateOf(0f) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = dragOffsetY
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectSong(song) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtwork(
                                artwork = song.artwork,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.6f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Tactile move actions (up/down) for robust fallback in all environments
                            if (index > 0) {
                                IconButton(
                                    onClick = {
                                        haptic()
                                        playbackViewModel.reorderQueue(absoluteIndex, absoluteIndex - 1)
                                    },
                                    modifier = Modifier.size(32.dp).testTag("queue_move_up_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < upcomingSongs.lastIndex) {
                                IconButton(
                                    onClick = {
                                        haptic()
                                        playbackViewModel.reorderQueue(absoluteIndex, absoluteIndex + 1)
                                    },
                                    modifier = Modifier.size(32.dp).testTag("queue_move_down_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Drag reorder handle
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = { haptic() },
                                            onDragEnd = {
                                                val rowHeightPx = 68.dp.toPx()
                                                val deltaRows = Math.round(dragOffsetY / rowHeightPx)
                                                if (deltaRows != 0) {
                                                    val targetIndex = (absoluteIndex + deltaRows).coerceIn(
                                                        currentIndex + 1,
                                                        queue.size - 1
                                                    )
                                                    playbackViewModel.reorderQueue(absoluteIndex, targetIndex)
                                                }
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount
                                            }
                                        )
                                    }
                                    .testTag("queue_drag_handle_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSettingsView(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val isCrossfadeEnabled by playbackViewModel.isCrossfadeEnabled.collectAsState()
    val crossfadeDuration by playbackViewModel.crossfadeDurationSeconds.collectAsState()
    val isOfflineMode by playbackViewModel.isOfflineMode.collectAsState()

    var isEqEnabled by remember { mutableStateOf(playbackViewModel.isEqEnabled) }
    var bandLevels by remember { mutableStateOf(playbackViewModel.getBandLevels()) }
    val presets = remember { playbackViewModel.getEqualizerPresets() }
    val frequencies = remember { playbackViewModel.getBandFrequencies() }
    val levelRange = remember { playbackViewModel.getBandLevelRange() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Offline Mode Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Mode",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Play downloaded songs without internet",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isOfflineMode,
                            onCheckedChange = {
                                playbackViewModel.toggleOfflineMode()
                            },
                            modifier = Modifier.testTag("offline_mode_switch")
                        )
                    }
                }
            }
        }

        // --- 2. Crossfade Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Crossfade Transitions",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Smoothly blend tracks together",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isCrossfadeEnabled,
                            onCheckedChange = {
                                playbackViewModel.toggleCrossfade(it)
                            },
                            modifier = Modifier.testTag("crossfade_switch")
                        )
                    }

                    if (isCrossfadeEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Crossfade Duration",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${crossfadeDuration}s",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = crossfadeDuration.toFloat(),
                            onValueChange = {
                                playbackViewModel.setCrossfadeDuration(it.toInt())
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.testTag("crossfade_duration_slider")
                        )
                    }
                }
            }
        }

        // --- 3. Equalizer Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Equalizer",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Enable custom DSP sound tuning",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = isEqEnabled,
                            onCheckedChange = {
                                isEqEnabled = it
                                playbackViewModel.toggleEqualizer(it)
                            },
                            modifier = Modifier.testTag("equalizer_switch")
                        )
                    }

                    if (isEqEnabled) {
                        // Presets
                        if (presets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Presets",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(presets.size) { index ->
                                    val presetName = presets[index]
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            playbackViewModel.usePreset(index.toShort())
                                            bandLevels = playbackViewModel.getBandLevels()
                                        },
                                        label = { Text(text = presetName, color = Color.White) }
                                    )
                                }
                            }
                        }

                        // Slider per band
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Frequency Bands",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        frequencies.forEachIndexed { index, freq ->
                            val bandKey = index.toShort()
                            val level = bandLevels[bandKey] ?: 0.toShort()
                            val levelDb = level.toFloat() / 100f
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (freq >= 1000) "${freq / 1000} kHz" else "${freq} Hz",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(64.dp)
                                )
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { value ->
                                        playbackViewModel.setBandLevel(bandKey, value.toInt().toShort())
                                        bandLevels = playbackViewModel.getBandLevels()
                                    },
                                    valueRange = levelRange.first.toFloat()..levelRange.second.toFloat(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_band_${index}_slider")
                                )
                                Text(
                                    text = String.format("%.1f dB", levelDb),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(54.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Preferred Audio Format Section ---
        item {
            val preferredFormat by playbackViewModel.preferredAudioFormat.collectAsState()
            val formats = listOf("High Quality (AAC)", "Lossless (ALAC)", "Hi-Res Lossless (ALAC)", "Dolby Atmos (Spatial Audio)")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Preferred Streaming Format",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configure high fidelity ALAC or spatial streaming properties",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    formats.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackViewModel.setPreferredAudioFormat(format)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = format,
                                color = if (preferredFormat == format) MaterialTheme.colorScheme.primary else Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (preferredFormat == format) FontWeight.Bold else FontWeight.Normal
                            )
                            RadioButton(
                                selected = preferredFormat == format,
                                onClick = {
                                    playbackViewModel.setPreferredAudioFormat(format)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
