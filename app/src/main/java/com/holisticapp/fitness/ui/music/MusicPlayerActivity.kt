package com.holisticapp.fitness.ui.music

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.holisticapp.fitness.R
import com.holisticapp.fitness.repository.SpotifyRepository
import kotlinx.coroutines.launch
import java.io.IOException

class MusicPlayerActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MusicPlayerActivity"
    }
    
    private lateinit var playPauseButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var songTitleText: TextView
    private lateinit var artistText: TextView
    private lateinit var albumArt: ImageView
    private lateinit var playlistRecyclerView: RecyclerView
    
    // Mood selection cards
    private lateinit var happyMoodCard: CardView
    private lateinit var sadMoodCard: CardView
    private lateinit var calmMoodCard: CardView
    private lateinit var angryMoodCard: CardView
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isPreparing = false
    private var currentSongIndex = 0
    private var currentPosition = 0
    private var totalDuration = 180000 // 3 minutes in milliseconds
    private var currentMood = "Happy"
    private var currentPlaylist = listOf<Song>()
    private var isLoadingPlaylist = false
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null
    private val spotifyRepository = SpotifyRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        
        initViews()
        setupClickListeners()
        setupSeekBar()
        setupMoodSelection()
        
        // Start with Happy mood
        selectMood("Happy")
    }
    
    private fun initViews() {
        playPauseButton = findViewById(R.id.playPauseButton)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        shuffleButton = findViewById(R.id.shuffleButton)
        repeatButton = findViewById(R.id.repeatButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        songTitleText = findViewById(R.id.songTitleText)
        artistText = findViewById(R.id.artistText)
        albumArt = findViewById(R.id.albumArt)
        playlistRecyclerView = findViewById(R.id.playlistRecyclerView)
        
        // Mood cards
        happyMoodCard = findViewById(R.id.happyMoodCard)
        sadMoodCard = findViewById(R.id.sadMoodCard)
        calmMoodCard = findViewById(R.id.calmMoodCard)
        angryMoodCard = findViewById(R.id.angryMoodCard)
        
        // Back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Initialize UI
        songTitleText.text = "Select a mood to load music"
        artistText.text = "Spotify Integration Ready"
    }
    
    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            if (currentPlaylist.isNotEmpty()) {
                togglePlayPause()
            } else {
                Toast.makeText(this, "Please select a mood first", Toast.LENGTH_SHORT).show()
            }
        }
        
        previousButton.setOnClickListener {
            if (currentPlaylist.isNotEmpty()) {
                playPreviousSong()
            }
        }
        
        nextButton.setOnClickListener {
            if (currentPlaylist.isNotEmpty()) {
                playNextSong()
            }
        }
        
        shuffleButton.setOnClickListener {
            shuffleButton.alpha = if (shuffleButton.alpha == 1.0f) 0.5f else 1.0f
            Toast.makeText(this, if (shuffleButton.alpha == 1.0f) "Shuffle ON" else "Shuffle OFF", Toast.LENGTH_SHORT).show()
        }
        
        repeatButton.setOnClickListener {
            repeatButton.alpha = if (repeatButton.alpha == 1.0f) 0.5f else 1.0f
            Toast.makeText(this, if (repeatButton.alpha == 1.0f) "Repeat ON" else "Repeat OFF", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupMoodSelection() {
        happyMoodCard.setOnClickListener { selectMood("Happy") }
        sadMoodCard.setOnClickListener { selectMood("Sad") }
        calmMoodCard.setOnClickListener { selectMood("Calm") }
        angryMoodCard.setOnClickListener { selectMood("Angry") }
    }
    
    private fun selectMood(mood: String) {
        if (isLoadingPlaylist) {
            Toast.makeText(this, "Please wait, loading playlist...", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Selecting mood: $mood")
        currentMood = mood
        currentSongIndex = 0
        
        // Stop current playback
        stopMusic()
        
        // Update mood card appearances
        resetMoodCards()
        when (mood) {
            "Happy" -> happyMoodCard.setCardBackgroundColor(getColor(R.color.mood_selected))
            "Sad" -> sadMoodCard.setCardBackgroundColor(getColor(R.color.mood_selected))
            "Calm" -> calmMoodCard.setCardBackgroundColor(getColor(R.color.mood_selected))
            "Angry" -> angryMoodCard.setCardBackgroundColor(getColor(R.color.mood_selected))
        }
        
        // Load Spotify playlist for the selected mood
        isLoadingPlaylist = true
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MusicPlayerActivity, "🎵 Loading $mood music from Spotify...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Fetching $mood playlist from Spotify")
                
                currentPlaylist = spotifyRepository.getEmotionBasedPlaylist(mood)
                
                runOnUiThread {
                    if (currentPlaylist.isNotEmpty()) {
                        updatePlaylistForMood()
                        updateCurrentSong()
                        resetPosition()
                        
                        val songsWithPreviews = currentPlaylist.count { it.url.isNotEmpty() }
                        Toast.makeText(this@MusicPlayerActivity, "✅ Loaded ${currentPlaylist.size} $mood songs! ($songsWithPreviews with previews)", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Successfully loaded ${currentPlaylist.size} songs for $mood")
                    } else {
                        Toast.makeText(this@MusicPlayerActivity, "❌ No songs found for $mood", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "No songs found for $mood")
                    }
                    isLoadingPlaylist = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading $mood playlist: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@MusicPlayerActivity, "❌ Error loading playlist: ${e.message}", Toast.LENGTH_LONG).show()
                    isLoadingPlaylist = false
                }
            }
        }
    }
    
    private fun resetMoodCards() {
        val defaultColor = getColor(R.color.mood_default)
        happyMoodCard.setCardBackgroundColor(defaultColor)
        sadMoodCard.setCardBackgroundColor(defaultColor)
        calmMoodCard.setCardBackgroundColor(defaultColor)
        angryMoodCard.setCardBackgroundColor(defaultColor)
    }
    
    private fun setupSeekBar() {
        seekBar.max = totalDuration
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null && !isPreparing) {
                    currentPosition = progress
                    mediaPlayer?.seekTo(progress)
                    updateTimeTexts()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updatePlaylistForMood() {
        val adapter = PlaylistAdapter(currentPlaylist) { song, position ->
            Log.d(TAG, "Song selected: ${song.title} at position $position")
            currentSongIndex = position
            updateCurrentSong()
            
            // Auto-play the selected song
            if (isPlaying) {
                stopMusic()
            }
            playMusic()
        }
        
        playlistRecyclerView.layoutManager = LinearLayoutManager(this)
        playlistRecyclerView.adapter = adapter
    }
    
    private fun togglePlayPause() {
        if (isPlaying) {
            pauseMusic()
        } else {
            playMusic()
        }
    }
    
    private fun playMusic() {
        if (currentPlaylist.isEmpty()) {
            Toast.makeText(this, "No songs available", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isPreparing) {
            Toast.makeText(this, "Please wait, preparing audio...", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentSong = currentPlaylist[currentSongIndex]
        Log.d(TAG, "Attempting to play: ${currentSong.title} - URL: ${currentSong.url}")
        
        try {
            // Release previous MediaPlayer
            mediaPlayer?.release()
            mediaPlayer = null
            
            if (currentSong.url.isNotEmpty()) {
                // Try to play Spotify preview
                isPreparing = true
                Toast.makeText(this, "🎵 Loading ${currentSong.title}...", Toast.LENGTH_SHORT).show()
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(currentSong.url)
                    
                    setOnPreparedListener { mediaPlayerInstance ->
                        Log.d(TAG, "MediaPlayer prepared for ${currentSong.title}")
                        this@MusicPlayerActivity.isPreparing = false
                        mediaPlayerInstance.start()
                        this@MusicPlayerActivity.isPlaying = true
                        this@MusicPlayerActivity.totalDuration = mediaPlayerInstance.duration
                        this@MusicPlayerActivity.seekBar.max = this@MusicPlayerActivity.totalDuration
                        this@MusicPlayerActivity.updatePlayPauseButton()
                        this@MusicPlayerActivity.startSeekBarUpdate()
                        Toast.makeText(this@MusicPlayerActivity, "▶️ Playing: ${currentSong.title}", Toast.LENGTH_SHORT).show()
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "Song completed: ${currentSong.title}")
                        playNextSong()
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for ${currentSong.title}")
                        isPreparing = false
                        Toast.makeText(this@MusicPlayerActivity, "❌ Cannot play preview for ${currentSong.title}", Toast.LENGTH_LONG).show()
                        simulatePlayback()
                        true
                    }
                    
                    prepareAsync()
                }
            } else {
                // No preview URL available, try fallback playlist or simulate playback
                Log.d(TAG, "No preview URL for ${currentSong.title}, trying fallback or simulating playback")
                
                // Check if this is from fallback playlist (which should have demo audio)
                if (currentSong.url.isEmpty()) {
                    Toast.makeText(this, "🎵 ${currentSong.title} (Demo mode - simulated playback)", Toast.LENGTH_LONG).show()
                    simulatePlayback()
                } else {
                    // This shouldn't happen, but just in case
                    Toast.makeText(this, "🎵 ${currentSong.title} (Demo mode - no preview available)", Toast.LENGTH_LONG).show()
                    simulatePlayback()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music: ${e.message}", e)
            isPreparing = false
            Toast.makeText(this, "❌ Error playing ${currentSong.title}: ${e.message}", Toast.LENGTH_LONG).show()
            simulatePlayback()
        }
    }
    
    private fun simulatePlayback() {
        Log.d(TAG, "Starting simulated playback")
        isPlaying = true
        totalDuration = currentPlaylist[currentSongIndex].duration * 1000
        seekBar.max = totalDuration
        updatePlayPauseButton()
        startSeekBarUpdate()
    }
    
    private fun pauseMusic() {
        Log.d(TAG, "Pausing music")
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
        stopSeekBarUpdate()
        Toast.makeText(this, "⏸️ Paused", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopMusic() {
        Log.d(TAG, "Stopping music")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        isPreparing = false
        updatePlayPauseButton()
        stopSeekBarUpdate()
        resetPosition()
    }
    
    private fun playPreviousSong() {
        if (currentPlaylist.isEmpty()) return
        
        Log.d(TAG, "Playing previous song")
        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else currentPlaylist.size - 1
        updateCurrentSong()
        resetPosition()
        
        if (isPlaying) {
            stopMusic()
        }
        playMusic()
    }
    
    private fun playNextSong() {
        if (currentPlaylist.isEmpty()) return
        
        Log.d(TAG, "Playing next song")
        currentSongIndex = if (currentSongIndex < currentPlaylist.size - 1) currentSongIndex + 1 else 0
        updateCurrentSong()
        resetPosition()
        
        if (isPlaying) {
            stopMusic()
        }
        playMusic()
    }
    
    private fun updateCurrentSong() {
        if (currentPlaylist.isEmpty()) return
        
        val currentSong = currentPlaylist[currentSongIndex]
        Log.d(TAG, "Updating current song to: ${currentSong.title}")
        
        songTitleText.text = currentSong.title
        artistText.text = currentSong.artist
        totalDuration = currentSong.duration * 1000
        seekBar.max = totalDuration
        updateTimeTexts()
        
        // Update playlist adapter to highlight current song
        (playlistRecyclerView.adapter as? PlaylistAdapter)?.setCurrentPlaying(currentSongIndex)
    }
    
    private fun resetPosition() {
        currentPosition = 0
        seekBar.progress = 0
        updateTimeTexts()
    }
    
    private fun updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play)
        }
    }
    
    private fun startSeekBarUpdate() {
        stopSeekBarUpdate() // Stop any existing updates
        
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            currentPosition = mp.currentPosition
                            seekBar.progress = currentPosition
                            updateTimeTexts()
                        }
                    } ?: run {
                        // Simulation mode
                        if (currentPosition < totalDuration) {
                            currentPosition += 1000
                            seekBar.progress = currentPosition
                            updateTimeTexts()
                        } else {
                            // Song finished in simulation mode
                            playNextSong()
                            return
                        }
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }
    
    private fun stopSeekBarUpdate() {
        updateSeekBarRunnable?.let { 
            handler.removeCallbacks(it)
            updateSeekBarRunnable = null
        }
    }
    
    private fun updateTimeTexts() {
        currentTimeText.text = formatTime(currentPosition)
        totalTimeText.text = formatTime(totalDuration)
    }
    
    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed, cleaning up")
        stopSeekBarUpdate()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    override fun onPause() {
        super.onPause()
        // Pause music when activity is paused
        if (isPlaying) {
            pauseMusic()
        }
    }
    
    data class Song(
        val title: String,
        val artist: String,
        val emoji: String,
        val duration: Int, // in seconds
        val url: String,
        val spotifyUrl: String = "",
        val albumArt: String = ""
    )
} 