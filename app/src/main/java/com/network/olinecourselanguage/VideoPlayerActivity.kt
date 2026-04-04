package com.network.olinecourselanguage

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var videoTitle: String
    private lateinit var videoUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoTitle = intent.getStringExtra("videoTitle") ?: "Video"
        videoUrl = intent.getStringExtra("videoUrl") ?: ""

        supportActionBar?.title = videoTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        videoView = findViewById(R.id.videoView)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val uri = Uri.parse(videoUrl)
        videoView.setVideoURI(uri)
        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        if (::videoView.isInitialized) {
            videoView.pause()
        }
    }
}