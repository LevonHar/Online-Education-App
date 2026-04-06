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
    private lateinit var dropdownLayout: TextInputLayout
    private val categoriesList = mutableListOf<String>()
    private val categoryIdMap = mutableMapOf<String, String>() // name -> id

    private lateinit var itemsRecyclerView: RecyclerView
    private lateinit var itemsAdapter: ItemsAdapter
    private val itemsList = mutableListOf<CourseItem>()

    private var currentCategoryId: String = ""
    private var isDropdownOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        categoryDropdown = findViewById(R.id.dropdown1)
        dropdownLayout = findViewById(R.id.dropdownLayout1)

        // Set default text
        categoryDropdown.setText("", false)
        categoryDropdown.hint = "Select language"

        // Setup dropdown toggle logic
        setupDropdownToggle()

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
            closeDropdown() // Close dropdown after selection
        }

        // Handle focus change to close dropdown when losing focus
        categoryDropdown.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeDropdown()
            }
        }
    }

    private fun setupDropdownToggle() {
        // Method 1: Click on the end icon (dropdown arrow)
        dropdownLayout.setEndIconOnClickListener {
            toggleDropdown()
        }

        // Method 2: Click on the AutoCompleteTextView itself
        categoryDropdown.setOnClickListener {
            toggleDropdown()
        }

        // Method 3: Click on the entire TextInputLayout area
        dropdownLayout.setOnClickListener {
            toggleDropdown()
        }
    }

    private fun toggleDropdown() {
        if (isDropdownOpen) {
            closeDropdown()
        } else {
            openDropdown()
        }
    }

    private fun openDropdown() {
        if (categoriesList.isNotEmpty()) {
            categoryDropdown.showDropDown()
            isDropdownOpen = true

            // Optional: Change icon to indicate dropdown is open
            dropdownLayout.endIconDrawable?.setTint(resources.getColor(android.R.color.holo_blue_dark))
        } else {
            Toast.makeText(this, "No languages available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeDropdown() {
        if (isDropdownOpen) {
            categoryDropdown.dismissDropDown()
            isDropdownOpen = false

            // Optional: Reset icon color
            dropdownLayout.endIconDrawable?.setTint(resources.getColor(R.color.gray))
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
                    // Don't auto-select first category anymore
                    categoryDropdown.setText("", false)
                    // Clear items view until user selects a category
                    itemsList.clear()
                    itemsAdapter.notifyDataSetChanged()
                    showEmptyState(true, "Select a language to view courses")
                } else {
                    // Show empty state for categories
                    showEmptyState(true, "No languages available")
                }
            }
    }

    private fun setupCategoryDropdown() {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categoriesList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                return view
            }
        }

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
                if (itemsList.isEmpty()) {
                    showEmptyState(true, "No courses available in this language")
                } else {
                    showEmptyState(false, "")
                }
            }
    }

    private fun showEmptyState(show: Boolean, message: String = "") {
        val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
        val itemsRecyclerView = findViewById<RecyclerView>(R.id.itemsRecyclerView)

        if (show) {
            emptyStateText.visibility = View.VISIBLE
            itemsRecyclerView.visibility = View.GONE
            if (message.isNotEmpty()) {
                emptyStateText.text = message
            }
        } else {
            emptyStateText.visibility = View.GONE
            itemsRecyclerView.visibility = View.VISIBLE
        }
    }

    // Optional: Handle back button to close dropdown if open
    override fun onBackPressed() {
        if (isDropdownOpen) {
            closeDropdown()
        } else {
            super.onBackPressed()
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
                    .into(holder.image)
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