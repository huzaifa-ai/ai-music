package com.holisticapp.fitness.repository

import android.util.Base64
import android.util.Log
import com.holisticapp.fitness.api.*
import com.holisticapp.fitness.ui.music.MusicPlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SpotifyRepository {
    
    companion object {
        private const val CLIENT_ID = "564509612023414f9beb9691c260fc67"
        private const val CLIENT_SECRET = "9829d091cd004110b51b5cfbaae00153"
        private const val SPOTIFY_ACCOUNTS_BASE_URL = "https://accounts.spotify.com/"
        private const val SPOTIFY_API_BASE_URL = "https://api.spotify.com/"
        private const val TAG = "SpotifyRepository"
    }
    
    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0
    
    // Enhanced emotion-based search queries with artists known to have previews
    private val emotionQueries = mapOf(
        "Happy" to listOf(
            "happy pop music year:2020-2024",
            "upbeat dance songs artist:Dua Lipa OR artist:Bruno Mars",
            "feel good hits artist:Pharrell Williams OR artist:Justin Timberlake",
            "cheerful songs artist:Taylor Swift OR artist:Ed Sheeran",
            "positive vibes music genre:pop"
        ),
        "Sad" to listOf(
            "sad ballads artist:Adele OR artist:Sam Smith",
            "emotional songs artist:Billie Eilish OR artist:Lewis Capaldi",
            "heartbreak music artist:Taylor Swift OR artist:The Weeknd",
            "melancholy songs genre:indie",
            "slow emotional music artist:John Mayer"
        ),
        "Calm" to listOf(
            "relaxing music artist:Ludovico Einaudi OR artist:Max Richter",
            "chill ambient genre:ambient",
            "peaceful instrumental artist:Ólafur Arnalds",
            "meditation music genre:new-age",
            "lo-fi chill music genre:lo-fi"
        ),
        "Angry" to listOf(
            "rock music artist:Foo Fighters OR artist:Green Day",
            "metal songs artist:Metallica OR artist:Linkin Park", 
            "intense music genre:rock",
            "aggressive rock artist:Rage Against The Machine",
            "hard rock music artist:AC/DC OR artist:Queen"
        )
    )
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val authService = Retrofit.Builder()
        .baseUrl(SPOTIFY_ACCOUNTS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApiService::class.java)
    
    private val apiService = Retrofit.Builder()
        .baseUrl(SPOTIFY_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApiService::class.java)
    
    suspend fun getEmotionBasedPlaylist(emotion: String): List<MusicPlayerActivity.Song> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting to fetch $emotion playlist from Spotify")
                
                // Get access token if needed
                if (!isTokenValid()) {
                    Log.d(TAG, "Getting new access token")
                    getAccessToken()
                }
                
                val queries = emotionQueries[emotion] ?: emotionQueries["Happy"]!!
                val allTracks = mutableListOf<SpotifyTrack>()
                
                // Search for tracks using different emotion-related queries
                for (query in queries.take(5)) {
                    try {
                        Log.d(TAG, "Searching for: $query")
                        val response = apiService.searchTracks(
                            authorization = "Bearer $accessToken",
                            query = query,
                            limit = 20,
                            market = "US"
                        )
                        
                        if (response.isSuccessful) {
                            response.body()?.tracks?.items?.let { tracks ->
                                Log.d(TAG, "Found ${tracks.size} tracks for query: $query")
                                // Filter to only include tracks with preview URLs first
                                val tracksWithPreviews = tracks.filter { it.preview_url != null }
                                val tracksWithoutPreviews = tracks.filter { it.preview_url == null }
                                
                                Log.d(TAG, "Tracks with previews: ${tracksWithPreviews.size}, without: ${tracksWithoutPreviews.size}")
                                
                                // Add tracks with previews first, then others
                                allTracks.addAll(tracksWithPreviews)
                                allTracks.addAll(tracksWithoutPreviews)
                            }
                        } else {
                            Log.e(TAG, "Search failed for query: $query, Code: ${response.code()}")
                            response.errorBody()?.string()?.let { error ->
                                Log.e(TAG, "Error body: $error")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error searching for $query: ${e.message}", e)
                    }
                }
                
                Log.d(TAG, "Total tracks found: ${allTracks.size}")
                
                // Convert Spotify tracks to our Song format, prioritizing tracks with preview URLs
                val songs = allTracks
                    .distinctBy { it.id }
                    .sortedByDescending { it.preview_url != null }
                    .take(25)
                    .map { track ->
                        MusicPlayerActivity.Song(
                            title = track.name,
                            artist = track.artists.joinToString(", ") { it.name },
                            emoji = getEmotionEmoji(emotion),
                            duration = track.duration_ms / 1000,
                            url = track.preview_url ?: "",
                            spotifyUrl = track.external_urls.spotify,
                            albumArt = track.album.images.firstOrNull()?.url ?: ""
                        )
                    }
                
                val songsWithPreviews = songs.count { it.url.isNotEmpty() }
                Log.d(TAG, "Final playlist: ${songs.size} songs, $songsWithPreviews with previews")
                
                if (songs.isNotEmpty()) {
                    songs
                } else {
                    Log.w(TAG, "No songs found, using fallback")
                    getFallbackPlaylist(emotion)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting emotion-based playlist: ${e.message}", e)
                // Return fallback playlist
                getFallbackPlaylist(emotion)
            }
        }
    }
    
    private suspend fun getAccessToken() {
        try {
            val credentials = "$CLIENT_ID:$CLIENT_SECRET"
            val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            
            Log.d(TAG, "Requesting access token from Spotify")
            val response = authService.getAccessToken(
                authorization = "Basic $encodedCredentials"
            )
            
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    accessToken = tokenResponse.access_token
                    tokenExpiryTime = System.currentTimeMillis() + (tokenResponse.expires_in * 1000)
                    Log.d(TAG, "Access token obtained successfully, expires in ${tokenResponse.expires_in} seconds")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Failed to get access token: ${response.code()} - $errorBody")
                throw Exception("Failed to authenticate with Spotify: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}", e)
            throw e
        }
    }
    
    private fun isTokenValid(): Boolean {
        val valid = accessToken != null && System.currentTimeMillis() < tokenExpiryTime - 60000 // 1 minute buffer
        Log.d(TAG, "Token valid: $valid")
        return valid
    }
    
    private fun getEmotionEmoji(emotion: String): String {
        return when (emotion) {
            "Happy" -> "😊"
            "Sad" -> "😢"
            "Calm" -> "😌"
            "Angry" -> "😠"
            else -> "🎵"
        }
    }
    
    private fun getFallbackPlaylist(emotion: String): List<MusicPlayerActivity.Song> {
        Log.d(TAG, "Using fallback playlist for $emotion")
        // Enhanced fallback playlists with sample audio URLs for demo
        val fallbackPlaylists = mapOf(
            "Happy" to listOf(
                MusicPlayerActivity.Song("Happy", "Pharrell Williams", "😊", 232, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_700KB.mp3", "https://open.spotify.com/track/60nZcImufyMA1MKQY3dcCH", ""),
                MusicPlayerActivity.Song("Good as Hell", "Lizzo", "🎉", 219, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_1MG.mp3", "https://open.spotify.com/track/1xK1Gg9SxG1ECwfYXbAF1H", ""),
                MusicPlayerActivity.Song("Can't Stop the Feeling!", "Justin Timberlake", "☀️", 236, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_2MG.mp3", "https://open.spotify.com/track/6KuQTIu1KoTTkLXKrwlLPV", ""),
                MusicPlayerActivity.Song("Uptown Funk", "Mark Ronson ft. Bruno Mars", "🌈", 270, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_5MG.mp3", "https://open.spotify.com/track/32OlwWuMpZ6b0aN2RZOeMS", "")
            ),
            "Sad" to listOf(
                MusicPlayerActivity.Song("Someone Like You", "Adele", "😢", 285, "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3", "https://open.spotify.com/track/1zwMYTA5nlNjZxYrvBB2pV", ""),
                MusicPlayerActivity.Song("Hurt", "Johnny Cash", "💧", 218, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_700KB.mp3", "https://open.spotify.com/track/5Ej1G1QjkSeJtFCLNocPIm", ""),
                MusicPlayerActivity.Song("Mad World", "Gary Jules", "💙", 189, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_1MG.mp3", "https://open.spotify.com/track/3JOVTQ5h8HGFnDdp4VT3MP", ""),
                MusicPlayerActivity.Song("Black", "Pearl Jam", "🌙", 343, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_2MG.mp3", "https://open.spotify.com/track/6nTiIhLmQ3FWhvrGafw2zj", "")
            ),
            "Calm" to listOf(
                MusicPlayerActivity.Song("Weightless", "Marconi Union", "😌", 485, "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3", "https://open.spotify.com/track/6p0q6tRT8H2lYOAgLSOKto", ""),
                MusicPlayerActivity.Song("Clair de Lune", "Claude Debussy", "🌊", 300, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_700KB.mp3", "https://open.spotify.com/track/2HHtWyy5CgaQbC7XSoOb0e", ""),
                MusicPlayerActivity.Song("River", "Joni Mitchell", "🌲", 240, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_1MG.mp3", "https://open.spotify.com/track/3SdTKo2uVsxFblQjpScoHy", ""),
                MusicPlayerActivity.Song("Mad About You", "Sting", "🫁", 275, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_2MG.mp3", "https://open.spotify.com/track/0FDzzruyVECATHXKHFs9M0", "")
            ),
            "Angry" to listOf(
                MusicPlayerActivity.Song("Break Stuff", "Limp Bizkit", "😠", 167, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_5MG.mp3", "https://open.spotify.com/track/3UmaczJpikHgJFyBTAJVoz", ""),
                MusicPlayerActivity.Song("Bodies", "Drowning Pool", "🔥", 203, "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3", "https://open.spotify.com/track/0IuslXpMROHdEPvSl1fTQK", ""),
                MusicPlayerActivity.Song("Chop Suey!", "System of a Down", "⚡", 210, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_700KB.mp3", "https://open.spotify.com/track/2DlHlPMa4M17kufBvI2lEN", ""),
                MusicPlayerActivity.Song("Killing in the Name", "Rage Against the Machine", "💪", 314, "https://file-examples.com/storage/fe68c1b7b786c8a9c4b1b93/2017/11/file_example_MP3_1MG.mp3", "https://open.spotify.com/track/59WN2psjkt1tyaxjspN8fp", "")
            )
        )
        
        return fallbackPlaylists[emotion] ?: fallbackPlaylists["Happy"]!!
    }
} 