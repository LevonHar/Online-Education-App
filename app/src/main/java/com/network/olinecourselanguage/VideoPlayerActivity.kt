package com.network.olinecourselanguage

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var videoTitle: String
    private lateinit var videoUrl: String
    private lateinit var gestureDetector: GestureDetector
    private lateinit var mediaController: MediaController
    private var isVideoReady = false
    private var videoAspectRatio = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Hide status bar and make activity full screen
        hideStatusBar()
        setStatusBarColorBlack()

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

            // Get video dimensions
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

            // Adjust video size based on orientation
            adjustVideoSize()

            videoView.start()
        }

        videoView.start()
    }

    private fun setStatusBarColorBlack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)

            // For Android 6.0 and above, set dark icons on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            controller?.hide(android.view.WindowInsets.Type.statusBars())
            controller?.hide(android.view.WindowInsets.Type.navigationBars())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // For Android 4.4 to Android 10
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            // For older Android versions
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Hide action bar
        supportActionBar?.hide()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Adjust video size when orientation changes
        adjustVideoSize()

        // Keep status bar hidden when orientation changes
        hideStatusBar()

        // Change screen orientation mode based on current orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Make video full screen with immersive mode
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        } else {
            // Keep status bar hidden in portrait too
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun adjustVideoSize() {
        if (!::videoView.isInitialized || videoAspectRatio == 0f) return

        val layoutParams = videoView.layoutParams
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val orientation = resources.configuration.orientation

        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // In landscape, make video fill the screen
                layoutParams.width = screenWidth
                layoutParams.height = screenHeight
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                // In portrait, calculate appropriate size to maintain aspect ratio
                val videoHeight = (screenWidth / videoAspectRatio).toInt()

                if (videoHeight <= screenHeight) {
                    // Video is smaller than screen height - center it
                    layoutParams.width = screenWidth
                    layoutParams.height = videoHeight
                } else {
                    // Video is taller than screen - fit to height
                    layoutParams.width = (screenHeight * videoAspectRatio).toInt()
                    layoutParams.height = screenHeight
                }
            }
        }

        videoView.layoutParams = layoutParams
        videoView.requestLayout()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-hide status bar when window gains focus
            hideStatusBar()
        }
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