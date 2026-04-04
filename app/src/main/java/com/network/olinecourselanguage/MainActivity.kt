package com.network.olinecourselanguage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var categoryDropdown: AutoCompleteTextView
    private val categoriesList = mutableListOf<String>()
    private val categoryIdMap = mutableMapOf<String, String>() // name -> id

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemsAdapter: ItemsAdapter
    private val itemsList = mutableListOf<CourseItem>()

    private var currentCategoryId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        categoryDropdown = findViewById(R.id.dropdown1)

        // Setup dropdown layout
        val textInputLayout = findViewById<TextInputLayout>(R.id.dropdownLayout1)
        textInputLayout.setEndIconOnClickListener {
            categoryDropdown.showDropDown()
        }

        // Setup items RecyclerView
        itemsRecyclerView = findViewById(R.id.itemsRecyclerView)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsAdapter = ItemsAdapter(itemsList) { item ->
            // Open VideosListActivity when an item is clicked
            val intent = Intent(this, VideosListActivity::class.java)
            intent.putExtra("categoryId", currentCategoryId)
            intent.putExtra("itemId", item.id)
            intent.putExtra("itemTitle", item.title)
            startActivity(intent)
        }
        itemsRecyclerView.adapter = itemsAdapter

        loadCategories()

        categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoriesList[position]
            val selectedCategoryId = categoryIdMap[selectedCategory] ?: return@setOnItemClickListener
            currentCategoryId = selectedCategoryId
            loadItemsForCategory(selectedCategoryId)
        }
    }

    private fun loadCategories() {
        db.collection("categories")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading categories: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                categoriesList.clear()
                categoryIdMap.clear()

                snapshot?.documents?.forEach { document ->
                    val name = document.getString("name") ?: return@forEach
                    categoriesList.add(name)
                    categoryIdMap[name] = document.id
                }

                if (categoriesList.isNotEmpty()) {
                    setupCategoryDropdown()
                    // Auto-load first category
                    val firstCategoryName = categoriesList[0]
                    currentCategoryId = categoryIdMap[firstCategoryName] ?: ""
                    categoryDropdown.setText(firstCategoryName, false)
                    loadItemsForCategory(currentCategoryId)
                } else {
                    // Show empty state for categories
                    findViewById<TextView>(R.id.emptyStateText).visibility = View.VISIBLE
                    itemsRecyclerView.visibility = View.GONE
                }
            }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoriesList)
        categoryDropdown.setAdapter(adapter)
        categoryDropdown.threshold = 1 // Start showing after 1 character
    }

    private fun loadItemsForCategory(categoryId: String) {
        if (categoryId.isEmpty()) return

        db.collection("categories").document(categoryId)
            .collection("items")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading items: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                itemsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val item = CourseItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                    itemsList.add(item)
                }
                itemsAdapter.notifyDataSetChanged()

                // Show/hide empty state
                val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
                val itemsRecyclerView = findViewById<RecyclerView>(R.id.itemsRecyclerView)

                if (itemsList.isEmpty()) {
                    emptyStateText.visibility = View.VISIBLE
                    itemsRecyclerView.visibility = View.GONE
                    emptyStateText.text = "No items available in this category"
                } else {
                    emptyStateText.visibility = View.GONE
                    itemsRecyclerView.visibility = View.VISIBLE
                }
            }
    }

    data class CourseItem(
        val id: String = "",
        val title: String = "",
        val imageUrl: String = ""
    )

    class ItemsAdapter(
        private val items: List<CourseItem>,
        private val onItemClick: (CourseItem) -> Unit
    ) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.itemImage)
            val title: TextView = view.findViewById(R.id.itemTitle)
            val cardView: LinearLayout = view.findViewById(R.id.cardView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title

            // Load image without changing size - keep original layout dimensions
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(item.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.image)  // Removed centerCrop() and circleCrop() to maintain original sizing
            } else {
                holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Set click listener on the entire card
            holder.cardView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount() = items.size
    }
}