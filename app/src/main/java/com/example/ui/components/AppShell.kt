package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.viewmodel.LibraryViewModel
import com.example.viewmodel.PlaybackViewModel

enum class NavigationTab(val title: String) {
    LISTEN_NOW("Listen Now"),
    BROWSE("Browse"),
    RADIO("Radio"),
    LIBRARY("Library"),
    SEARCH("Search")
}

@Composable
fun AppShell(
    playbackViewModel: PlaybackViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.LISTEN_NOW) }
    var showPlayerModal by remember { mutableStateOf(false) }

    val currentSong by playbackViewModel.currentSong.collectAsState()
    val hapticClick = rememberHapticTrigger()

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Persistent Floating MiniPlayer with dynamic slide-in/fade-in entering transition
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 350)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 350)
                    ) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        MiniPlayer(
                            playbackViewModel = playbackViewModel,
                            onClick = {
                                hapticClick()
                                showPlayerModal = true
                            }
                        )
                    }
                }

                // Apple Music styled modern Navigation Bar with premium Liquid Glass finish
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar"),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.LISTEN_NOW,
                            onClick = {
                                hapticClick()
                                selectedTab = NavigationTab.LISTEN_NOW
                            },
                            icon = { Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "Listen Now") },
                            label = { Text(text = "Listen Now") },
                            modifier = Modifier.testTag("nav_tab_listen_now")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.BROWSE,
                            onClick = {
                                hapticClick()
                                selectedTab = NavigationTab.BROWSE
                            },
                            icon = { Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Browse") },
                            label = { Text(text = "Browse") },
                            modifier = Modifier.testTag("nav_tab_browse")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.RADIO,
                            onClick = {
                                hapticClick()
                                selectedTab = NavigationTab.RADIO
                            },
                            icon = { Icon(imageVector = Icons.Default.Radio, contentDescription = "Radio") },
                            label = { Text(text = "Radio") },
                            modifier = Modifier.testTag("nav_tab_radio")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.LIBRARY,
                            onClick = {
                                hapticClick()
                                selectedTab = NavigationTab.LIBRARY
                            },
                            icon = { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text(text = "Library") },
                            modifier = Modifier.testTag("nav_tab_library")
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavigationTab.SEARCH,
                            onClick = {
                                hapticClick()
                                selectedTab = NavigationTab.SEARCH
                            },
                            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                            label = { Text(text = "Search") },
                            modifier = Modifier.testTag("nav_tab_search")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // offsets to avoid overlapping the bottom tabs
        ) {
            // Screen Switcher
            when (selectedTab) {
                NavigationTab.LISTEN_NOW -> ListenNowScreen(
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel
                )
                NavigationTab.BROWSE -> BrowseScreen(
                    playbackViewModel = playbackViewModel
                )
                NavigationTab.RADIO -> RadioScreen(
                    playbackViewModel = playbackViewModel
                )
                NavigationTab.LIBRARY -> LibraryScreen(
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel
                )
                NavigationTab.SEARCH -> SearchScreen(
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel
                )
            }
        }
    }

    // Full-Screen sliding modal player with Apple Music design
    AnimatedVisibility(
        visible = showPlayerModal,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = tween(300))
    ) {
        PlayerModal(
            playbackViewModel = playbackViewModel,
            onDismiss = {
                showPlayerModal = false
            }
        )
    }

    // Artist Detail Horizontal Slide-in Overlay
    val selectedArtistName by playbackViewModel.selectedArtistName.collectAsState()
    AnimatedVisibility(
        visible = selectedArtistName != null,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut()
    ) {
        selectedArtistName?.let { artist ->
            ArtistDetailView(
                artistName = artist,
                playbackViewModel = playbackViewModel,
                onBack = {
                    playbackViewModel.selectArtist(null)
                }
            )
        }
    }
}
