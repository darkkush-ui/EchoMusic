package com.echomusic

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.load
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var etSearchQuery: EditText
    private lateinit var btnSearch: Button
    private lateinit var ivAlbumArt: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView

    private val ytProvider = YTMusicProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        etSearchQuery = findViewById(R.id.etSearchQuery)
        btnSearch = findViewById(R.id.btnSearch)
        ivAlbumArt = findViewById(R.id.ivAlbumArt)
        tvTitle = findViewById(R.id.tvTitle)
        tvArtist = findViewById(R.id.tvArtist)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "Enter a search query", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            val results = ytProvider.searchSong(query)
            if (results.isNotEmpty()) {
                val firstResult = results.first()
                updateUI(firstResult)
                playSong(firstResult.videoId)
            } else {
                Toast.makeText(this@MainActivity, "No results found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(songInfo: SongInfo) {
        tvTitle.text = songInfo.title
        tvArtist.text = songInfo.artist
        songInfo.albumArtUrl?.let { url ->
            ivAlbumArt.load(url) {
                crossfade(true)
            }
        }
    }

    private fun playSong(videoId: String) {
        // This is a naive way to play youtube audio by hitting a stream URL.
        // Real implementation usually involves a full youtube stream extractor (like YoutubeExtractor or NewPipeExtractor).
        // Since we are only building a basic integration player, we will attempt playing the video via web intent or
        // simply showing the metadata. For ExoPlayer to play YouTube links natively, a complex extractor is needed.
        // For demonstration, we will just set a placeholder audio stream or toast.

        Toast.makeText(this, "Metadata fetched! Need a YT extractor to play stream.", Toast.LENGTH_LONG).show()

        // Example of playing a standard MP3 to prove ExoPlayer works
        val mediaItem = MediaItem.fromUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onStop() {
        super.onStop()
        if (player.isPlaying) {
            player.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}