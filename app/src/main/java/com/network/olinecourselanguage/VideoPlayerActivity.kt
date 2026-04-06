package com.network.olinecourselanguage

import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var videoTitle: String
    private lateinit var videoUrl: String
    private lateinit var gestureDetector: GestureDetector
    private lateinit var mediaController: MediaController
    private var isVideoReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoTitle = intent.getStringExtra("videoTitle") ?: "Video"
        videoUrl = intent.getStringExtra("videoUrl") ?: ""

        supportActionBar?.title = videoTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        videoView = findViewById(R.id.videoView)

        // Initialize MediaController as a class property
        mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val uri = Uri.parse(videoUrl)
        videoView.setVideoURI(uri)

        // Set up gesture detection
        gestureDetector = GestureDetector(this, DoubleTapGestureListener())

        // Set onTouchListener to capture touch events
        videoView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Wait for video to be prepared before enabling seek operations
        videoView.setOnPreparedListener { mp ->
            isVideoReady = true
            videoView.start()
        }

        videoView.start()
    }

    override fun onPause() {
        super.onPause()
        if (::videoView.isInitialized) {
            videoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release video resources
        if (::videoView.isInitialized) {
            videoView.suspend()
        }
    }

    private inner class DoubleTapGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isVideoReady) {
                Toast.makeText(this@VideoPlayerActivity, "Video is loading...", Toast.LENGTH_SHORT).show()
                return true
            }

            val screenWidth = videoView.width
            val tapX = e.x

            val currentPosition = videoView.currentPosition
            val duration = videoView.duration

            // Check if double tap is on left or right side
            if (tapX < screenWidth / 2) {
                // Left side - seek backward 5 seconds
                val newPosition = (currentPosition - 5000).coerceAtLeast(0)
                videoView.seekTo(newPosition)
                showSeekToast(-5, newPosition, duration)
            } else {
                // Right side - seek forward 5 seconds
                val newPosition = (currentPosition + 5000).coerceAtMost(duration)
                videoView.seekTo(newPosition)
                showSeekToast(5, newPosition, duration)
            }

            return true
        }

        private fun showSeekToast(seconds: Int, newPosition: Int, duration: Int) {
            val minutes = (newPosition / 1000) / 60
            val secs = (newPosition / 1000) % 60
            val timeString = String.format("%02d:%02d", minutes, secs)

            val totalMinutes = (duration / 1000) / 60
            val totalSecs = (duration / 1000) % 60
            val totalTimeString = String.format("%02d:%02d", totalMinutes, totalSecs)

            val direction = if (seconds > 0) "+$seconds" else "$seconds"
            Toast.makeText(
                this@VideoPlayerActivity,
                "$direction sec: $timeString / $totalTimeString",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Toggle media controller visibility on single tap
            if (mediaController.isShowing) {
                mediaController.hide()
            } else {
                mediaController.show()
            }
            return true
        }
    }
}