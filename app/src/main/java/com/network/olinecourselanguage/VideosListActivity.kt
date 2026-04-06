package com.network.olinecourselanguage

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class VideosListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var videosAdapter: VideosAdapter
    private val videosList = mutableListOf<Video>()

    private lateinit var categoryId: String
    private lateinit var itemId: String
    private lateinit var itemTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videos_list)

        setStatusBarColorWhite()

        categoryId = intent.getStringExtra("categoryId") ?: ""
        itemId = intent.getStringExtra("itemId") ?: ""
        itemTitle = intent.getStringExtra("itemTitle") ?: "Videos"

        db = FirebaseFirestore.getInstance()

        supportActionBar?.title = itemTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        videosRecyclerView = findViewById(R.id.videosRecyclerView)
        videosRecyclerView.layoutManager = LinearLayoutManager(this)
        videosAdapter = VideosAdapter(videosList) { video ->
            // Open video player activity
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("videoTitle", video.title)
            intent.putExtra("videoUrl", video.videoUrl)
            startActivity(intent)
        }
        videosRecyclerView.adapter = videosAdapter

        loadVideos()
    }

    private fun setStatusBarColorWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)

            // For Android 6.0 and above, set dark icons on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun loadVideos() {
        db.collection("categories").document(categoryId)
            .collection("items").document(itemId)
            .collection("videos")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                videosList.clear()
                snapshot?.documents?.forEach { doc ->
                    val video = Video(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        thumbnailUrl = doc.getString("thumbnailUrl") ?: ""
                    )
                    videosList.add(video)
                }
                videosAdapter.notifyDataSetChanged()

                findViewById<TextView>(R.id.emptyStateText).visibility =
                    if (videosList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    data class Video(
        val id: String = "",
        val title: String = "",
        val videoUrl: String = "",
        val thumbnailUrl: String = ""
    )

    class VideosAdapter(
        private val videos: List<Video>,
        private val onItemClick: (Video) -> Unit
    ) : RecyclerView.Adapter<VideosAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
            val title: TextView = view.findViewById(R.id.videoTitle)
            val playButton: ImageView = view.findViewById(R.id.playButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val video = videos[position]
            holder.title.text = video.title

            if (video.thumbnailUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(video.thumbnailUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.thumbnail)
            } else {
                holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            holder.itemView.setOnClickListener { onItemClick(video) }
            holder.playButton.setOnClickListener { onItemClick(video) }
        }

        override fun getItemCount() = videos.size
    }
}