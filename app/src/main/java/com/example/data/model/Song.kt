package com.example.data.model

import android.os.Parcelable

data class Song(
    val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String, // Hex gradients or remote URLs
    val duration: Int,   // in seconds (0 for live streams)
    val genre: String,
    val lyrics: String = "",
    val isLiveRadio: Boolean = false
) {
    // Parser for LRC lyrics format [mm:ss.xx] text or [mm:ss] text
    fun parseLyrics(): List<LyricLine> {
        if (lyrics.isBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?]\\s*(.*)")
        lyrics.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msOffset = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                val timeMs = (min * 60 + sec) * 1000 + msOffset
                val text = match.groupValues[4].trim()
                lines.add(LyricLine(timeMs, text))
            } else if (line.isNotBlank()) {
                // Fallback for untimestamped text
                lines.add(LyricLine(-1L, line.trim()))
            }
        }
        return lines
    }
}

data class LyricLine(
    val timeMs: Long, // -1 if untimestamped
    val text: String
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String,
    val releaseYear: Int,
    val tracks: List<Song>
)

object MockMusicData {
    val songs = listOf(
        Song(
            id = "yuke_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            title = "Yu-Ke",
            artist = "LiSA",
            album = "LEO-NiNE",
            artwork = "linear-gradient(135deg, #FF3B30 0%, #FF9500 100%)",
            duration = 240,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Upbeat J-Rock Intro)
                [00:08] Chasing the ghost of a neon sky...
                [00:16] Wondering if we will ever fly...
                [00:24] Yu-Ke! Let's go, let's run...
                [00:32] Underneath the rising sun!
                [00:40] Grab my hand, don't look back now...
                [00:48] We will make it work somehow.
            """.trimIndent()
        ),
        Song(
            id = "aamxn_reona",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            title = "AAMXN",
            artist = "ReoNa",
            album = "ANIMA Single",
            artwork = "linear-gradient(135deg, #5856D6 0%, #007AFF 100%)",
            duration = 265,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Atmospheric synth build-up)
                [00:10] In the quiet of the night, I hear a call...
                [00:18] Shadows dancing on the crumbling wall...
                [00:26] AAMXN, find your soul, ignite the light!
                [00:34] We will shatter through this dark and endless night.
            """.trimIndent()
        ),
        Song(
            id = "unleashing_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            title = "Unleashing",
            artist = "LiSA",
            album = "LEO-NiNE",
            artwork = "linear-gradient(135deg, #FF2D55 0%, #FF5E3A 100%)",
            duration = 282,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Somber piano intro)
                [00:08] Coping with the pain of letting go...
                [00:16] Memories in black and white they show...
                [00:24] Unleashing all the tears inside my mind...
                [00:32] Leaving all the broken dreams behind.
            """.trimIndent()
        ),
        Song(
            id = "resolution_tomatsu",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            title = "Resolution",
            artist = "Haruka Tomatsu",
            album = "Resolution Single",
            artwork = "linear-gradient(135deg, #AF52DE 0%, #FF2D55 100%)",
            duration = 251,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Dramatic guitar intro)
                [00:08] A resolve that shines so bright and clear...
                [00:16] Overcoming every doubt and fear...
                [00:24] Resolution! Stand up and be strong...
                [00:32] To the place where we both belong!
            """.trimIndent()
        ),
        Song(
            id = "forgetmenot_reona",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            title = "Forget-me-not",
            artist = "ReoNa",
            album = "forget-me-not Single",
            artwork = "linear-gradient(135deg, #34C759 0%, #007AFF 100%)",
            duration = 238,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Acoustic guitar intro)
                [00:08] Tiny blue petals in the autumn wind...
                [00:16] Reminding me of where we did begin...
                [00:24] Forget-me-not, promise you'll stay true...
                [00:32] No matter where the path is leading you.
            """.trimIndent()
        ),
        Song(
            id = "iris_eir",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            title = "Iris (Instrumental)",
            artist = "Eir Aoi",
            album = "AUBE",
            artwork = "linear-gradient(135deg, #FF9500 0%, #FFCC00 100%)",
            duration = 290,
            genre = "J-Pop",
            lyrics = "[00:00] (Beautiful Instrumental - No lyrics for this instrumental track)"
        ),
        Song(
            id = "adamas_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            title = "ADAMAS",
            artist = "LiSA",
            album = "ADAMAS Single",
            artwork = "linear-gradient(135deg, #FF2D55 0%, #5856D6 100%)",
            duration = 225,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Fast heavy J-Rock Intro)
                [00:06] Shiny sword of crystal in my hand...
                [00:12] We will carve a path across this land...
                [00:18] ADAMAS! Shatter through the stone!
                [00:24] Claim the destiny that is our own!
            """.trimIndent()
        ),
        Song(
            id = "anima_reona",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            title = "Anima",
            artist = "ReoNa",
            album = "ANIMA Single",
            artwork = "linear-gradient(135deg, #007AFF 0%, #34C759 100%)",
            duration = 265,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Ethereal keys and build-up)
                [00:08] Uncovering the soul beneath the shell...
                [00:16] Stories that we never dared to tell...
                [00:24] Anima, let your spirit bloom...
                [00:32] Shedding light inside this quiet room.
            """.trimIndent()
        ),
        Song(
            id = "catchmoment_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            title = "Catch the Moment",
            artist = "LiSA",
            album = "LiTTLE DEViL PARADE",
            artwork = "linear-gradient(135deg, #FF5E3A 0%, #AF52DE 100%)",
            duration = 277,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Epic rock build-up)
                [00:08] Every single second that we share...
                [00:16] Feels like magic floating in the air...
                [00:24] Catch the moment! Hold it in your hand...
                [00:32] Together we will take a stand!
            """.trimIndent()
        ),
        Song(
            id = "launcher_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            title = "Launcher",
            artist = "LiSA",
            album = "Launcher",
            artwork = "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)",
            duration = 243,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Heavy bass-driven J-Rock intro)
                [00:08] Launching into orbit high above...
                [00:16] Sending out a signal of our love...
                [00:24] Ready, set, let the engines roar...
                [00:32] Flying higher than we did before!
            """.trimIndent()
        ),
        Song(
            id = "courage_tomatsu",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
            title = "Courage (Album Mix)",
            artist = "Haruka Tomatsu",
            album = "courage",
            artwork = "linear-gradient(135deg, #f857a6 0%, #ff5858 100%)",
            duration = 258,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Bright positive J-Pop intro)
                [00:08] Finding strength when everything is dark...
                [00:16] Deep within my heart there is a spark...
                [00:24] With courage, we'll face the brand new day...
                [00:32] Lighting up the shadows on our way!
            """.trimIndent()
        ),
        Song(
            id = "innocence_eir",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            title = "Innocence (Instrumental)",
            artist = "Eir Aoi",
            album = "Innocence",
            artwork = "linear-gradient(135deg, #00c6ff 0%, #0072ff 100%)",
            duration = 285,
            genre = "J-Pop",
            lyrics = "[00:00] (Energetic Instrumental - No lyrics for this instrumental track)"
        ),
        Song(
            id = "ryusei_eir",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
            title = "Ryusei (Instrumental)",
            artist = "Eir Aoi",
            album = "Ryusei",
            artwork = "linear-gradient(135deg, #F3904F 0%, #3B4371 100%)",
            duration = 264,
            genre = "J-Pop",
            lyrics = "[00:00] (Fast-paced Rock Instrumental - No lyrics)"
        ),
        Song(
            id = "overfly_haruna",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
            title = "Overfly",
            artist = "Luna Haruna",
            album = "Overfly",
            artwork = "linear-gradient(135deg, #4A00E0 0%, #8E2DE2 100%)",
            duration = 252,
            genre = "J-Pop",
            lyrics = """
                [00:00] (Melodic orchestral opening)
                [00:08] Flying over clouds of blue and white...
                [00:16] Searching for your hand in pure delight...
                [00:24] Overfly! Across the starry sky...
                [00:32] We will learn to stretch our wings and fly!
            """.trimIndent()
        ),
        Song(
            id = "sign_flow",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
            title = "Sign",
            artist = "FLOW",
            album = "Sign Single",
            artwork = "linear-gradient(135deg, #f12711 0%, #f5af19 100%)",
            duration = 237,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Famous Anime Rock Opening)
                [00:08] I realize the screaming pain...
                [00:14] Hearing loud rains of shame...
                [00:20] Kizuato wo nazotte mo...
                [00:26] This is the sign we will rise again!
            """.trimIndent()
        ),
        Song(
            id = "crossingfield_lisa",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3",
            title = "Crossing Field",
            artist = "LiSA",
            album = "LANDSPACE",
            artwork = "linear-gradient(135deg, #FF3B30 0%, #8E2DE2 100%)",
            duration = 249,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Synth Arpeggio & Power Chords)
                [00:06] Mitomete ita hazu no tsuyosa sae...
                [00:12] I always wanna be with you...
                [00:18] Crossing field of hopes and fears...
                [00:24] Washing away all of our tears!
            """.trimIndent()
        ),
        Song(
            id = "rain_pitbull",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            title = "Rain Over Me (feat. Marc Anthony)",
            artist = "Pitbull",
            album = "Planet Pit",
            artwork = "linear-gradient(135deg, #e52d27 0%, #b31217 100%)",
            duration = 231,
            genre = "Pop",
            lyrics = """
                [00:00] (Dance Club Beat)
                [00:08] Ay ay ay... Let it rain over me!
                [00:16] Girl, my body is your castle, let's make history...
                [00:24] Rain over me, wash it all down...
                [00:32] We're the king and queen of the town!
            """.trimIndent()
        ),
        Song(
            id = "closer_chainsmokers",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            title = "Closer (feat. Halsey)",
            artist = "The Chainsmokers",
            album = "Collage",
            artwork = "linear-gradient(135deg, #1D976C 0%, #93F9B9 100%)",
            duration = 244,
            genre = "Pop",
            lyrics = """
                [00:00] (Soft synth chord progression)
                [00:08] Hey, I was doing just fine before I met you...
                [00:16] I drink too much and that's an issue...
                [00:24] So baby pull me closer in the backseat of your Rover...
                [00:32] That I know you can't afford, bite that tattoo on your shoulder.
            """.trimIndent()
        ),
        Song(
            id = "wedonttalk_puth",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            title = "We Don't Talk Anymore (feat. Selena Gomez)",
            artist = "Charlie Puth",
            album = "Nine Track Mind",
            artwork = "linear-gradient(135deg, #4ca1af 0%, #c4e0e5 100%)",
            duration = 217,
            genre = "Pop",
            lyrics = """
                [00:00] (Funky muted guitar riff)
                [00:08] We don't talk anymore, we don't talk anymore...
                [00:16] Like we used to do...
                [00:24] I just hope you're lying next to somebody...
                [00:32] Who knows how to love you like me.
            """.trimIndent()
        ),
        Song(
            id = "legends_atc",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            title = "Legends Never Die",
            artist = "League of Legends Music & Against The Current",
            album = "Worlds 2017",
            artwork = "linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)",
            duration = 235,
            genre = "Alternative Rock",
            lyrics = """
                [00:00] (Orchestral fantasy atmosphere)
                [00:08] Legends never die, when the world is calling you...
                [00:16] Can you hear them screaming out your name?
                [00:24] Begging you to fight, through the fire and the pain...
                [00:32] Legends never die, they become a part of you!
            """.trimIndent()
        ),
        Song(
            id = "ignite_eir",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            title = "Ignite (Instrumental)",
            artist = "Eir Aoi",
            album = "Ignite",
            artwork = "linear-gradient(135deg, #f7971e 0%, #ffd200 100%)",
            duration = 244,
            genre = "J-Pop",
            lyrics = "[00:00] (Thrilling Rock Instrumental - No lyrics)"
        ),
        Song(
            id = "unravel_tk",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            title = "Unravel",
            artist = "TK from Ling tosite sigure",
            album = "unravel",
            artwork = "linear-gradient(135deg, #2c3e50 0%, #3498db 100%)",
            duration = 238,
            genre = "J-Rock",
            lyrics = """
                [00:00] (Oshiete oshiete yo... Whisper Intro)
                [00:06] Oshiete oshiete yo, sono shikumi wo...
                [00:12] Boku no naka ni, dare ga iru no?
                [00:18] Kowareta kowareta yo, kono sekai de...
                [00:24] Unravel ghoul in the shadows!
            """.trimIndent()
        ),
        Song(
            id = "heartattack_demi",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            title = "Heart Attack",
            artist = "Demi Lovato",
            album = "Demi",
            artwork = "linear-gradient(135deg, #f4c4f3 0%, #fc67fa 100%)",
            duration = 210,
            genre = "Pop",
            lyrics = """
                [00:00] (Electronic pop pulse)
                [00:08] Putting my defenses up, 'cause I don't wanna fall in love...
                [00:16] If I ever did that, I think I'd have a heart attack!
                [00:24] Never put my guard down, but you make me wanna try...
                [00:32] Shot me like a star right out of the sky.
            """.trimIndent()
        ),
        Song(
            id = "daidai_shonka",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            title = "Dai Dai",
            artist = "Shonka & Boona Boy",
            album = "Dai Dai Single",
            artwork = "linear-gradient(135deg, #30cfd0 0%, #330867 100%)",
            duration = 222,
            genre = "Afrobeats",
            lyrics = """
                [00:00] (Warm African percussion)
                [00:08] Dai Dai... rhythm of the motherland...
                [00:16] Dance with me, come on take my hand...
                [00:24] Feel the vibrations, rising up high...
                [00:32] Reaching all the way up to the sky.
            """.trimIndent()
        ),
        Song(
            id = "kalma_mun",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            title = "KALMA",
            artist = "MUN, EGE, PIT10 & ADOS",
            album = "KALMA",
            artwork = "linear-gradient(135deg, #85144b 0%, #F012BE 100%)",
            duration = 275,
            genre = "Rap",
            lyrics = """
                [00:00] (Deep trap beat and Turkish samples)
                [00:08] Kalma yanımda, git uzaklara...
                [00:16] Rhymes flowing heavy through the block...
                [00:24] Turkey to the world, keeping it locked...
                [00:32] Turkish Hip-hop never gonna stop.
            """.trimIndent()
        ),
        Song(
            id = "exotic_priyanka",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            title = "Exotic (feat. Pitbull)",
            artist = "Priyanka Chopra",
            album = "Exotic Single",
            artwork = "linear-gradient(135deg, #FF851B 0%, #FF4136 100%)",
            duration = 228,
            genre = "Pop",
            lyrics = """
                [00:00] (Bollywood Fusion Club Beat)
                [00:08] I'm feeling exotic, oceanic, atomic...
                [00:16] From Mumbai to LA, we party all day...
                [00:24] Exotic, feel the heat of the night...
                [00:32] Dancing till the early morning light!
            """.trimIndent()
        ),
        Song(
            id = "intruder_transtor",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
            title = "Intruder (Original Mix)",
            artist = "Transtor",
            album = "Intruder",
            artwork = "linear-gradient(135deg, #7F00FF 0%, #E100FF 100%)",
            duration = 312,
            genre = "Electronic",
            lyrics = "[00:00] (Heavy Progressive Trance - No lyrics)"
        ),
        Song(
            id = "shazam_tracks",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            title = "My Shazam Tracks",
            artist = "Shazam Discoveries",
            album = "Shazam",
            artwork = "linear-gradient(135deg, #001f3f 0%, #0074D9 100%)",
            duration = 245,
            genre = "Mixed Genres",
            lyrics = "[00:00] (Dynamic Playlist - Discoveries compiled from Shazam history)"
        )
    )

    val radioStations = listOf(
        Song(
            id = "radio_groove_salad",
            url = "https://ice1.somafm.com/groovesalad-128-mp3",
            title = "Groove Salad",
            artist = "SomaFM Live Radio",
            album = "SomaFM Curated Ambient",
            artwork = "linear-gradient(135deg, #3a7bd5 0%, #3a6073 100%)",
            duration = 0,
            genre = "Ambient / Downtempo",
            lyrics = "Live Stream - Dynamic lyrics not available for live radio broadcasts.",
            isLiveRadio = true
        ),
        Song(
            id = "radio_illstreet",
            url = "https://ice1.somafm.com/illstreet-128-mp3",
            title = "Illinois Street Lounge",
            artist = "SomaFM Live Radio",
            album = "Vintage Lounge & Exotica",
            artwork = "linear-gradient(135deg, #F3904F 0%, #3B4371 100%)",
            duration = 0,
            genre = "Lounge / Retro",
            lyrics = "Live Stream - Dynamic lyrics not available for live radio broadcasts.",
            isLiveRadio = true
        ),
        Song(
            id = "radio_indie_pop",
            url = "https://ice1.somafm.com/indiepop-128-mp3",
            title = "Indie Pop Rocks!",
            artist = "SomaFM Live Radio",
            album = "The Best Indie Pop/Rock",
            artwork = "linear-gradient(135deg, #e52d27 0%, #b31217 100%)",
            duration = 0,
            genre = "Indie Pop / Rock",
            lyrics = "Live Stream - Dynamic lyrics not available for live radio broadcasts.",
            isLiveRadio = true
        )
    )

    val albums = listOf(
        Album(
            id = "album_leoni_ne",
            title = "LEO-NiNE",
            artist = "LiSA",
            artwork = "linear-gradient(135deg, #FF3B30 0%, #FF9500 100%)",
            releaseYear = 2020,
            tracks = listOf(songs[0], songs[2])
        ),
        Album(
            id = "album_anima",
            title = "ANIMA Single",
            artist = "ReoNa",
            artwork = "linear-gradient(135deg, #5856D6 0%, #007AFF 100%)",
            releaseYear = 2020,
            tracks = listOf(songs[1], songs[4], songs[7])
        ),
        Album(
            id = "album_resolution",
            title = "Resolution Single",
            artist = "Haruka Tomatsu",
            artwork = "linear-gradient(135deg, #AF52DE 0%, #FF2D55 100%)",
            releaseYear = 2019,
            tracks = listOf(songs[3], songs[10])
        ),
        Album(
            id = "album_aube",
            title = "AUBE",
            artist = "Eir Aoi",
            artwork = "linear-gradient(135deg, #FF9500 0%, #FFCC00 100%)",
            releaseYear = 2014,
            tracks = listOf(songs[5], songs[11], songs[12], songs[20])
        ),
        Album(
            id = "album_landspace",
            title = "LANDSPACE",
            artist = "LiSA",
            artwork = "linear-gradient(135deg, #FF3B30 0%, #8E2DE2 100%)",
            releaseYear = 2013,
            tracks = listOf(songs[15], songs[8], songs[9])
        ),
        Album(
            id = "album_planet_pit",
            title = "Planet Pit",
            artist = "Pitbull",
            artwork = "linear-gradient(135deg, #e52d27 0%, #b31217 100%)",
            releaseYear = 2011,
            tracks = listOf(songs[16])
        ),
        Album(
            id = "album_pop_hits",
            title = "Pop Hits",
            artist = "Various Artists",
            artwork = "linear-gradient(135deg, #1D976C 0%, #93F9B9 100%)",
            releaseYear = 2018,
            tracks = listOf(songs[17], songs[18], songs[19], songs[22], songs[25])
        )
    )
}
