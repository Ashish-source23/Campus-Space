package com.example.campusspace // Match your package name from the error

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.ActivityMainBinding // Correct import
import com.example.campusspace.entity.GeofenceArea
import com.example.campusspace.services.GeofenceBroadcastReceiver
import com.example.campusspace.ui.CampusMapFragment
import com.example.campusspace.ui.MockData
import com.example.campusspace.ui.PlacesListFragment
import com.example.campusspace.ui.ViewPagerAdapter
import com.example.campusspace.utils.FirebaseDB
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.example.campusspace.ui.LoginActivity
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var geofencingClient: GeofencingClient
    private val geofenceList = mutableListOf<Geofence>()

    //GeoFencing Logic
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.example.campusspace.ACTION_GEOFENCE_EVENT"
        }
        PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    //Location Permission Logic
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                Log.d("MainActivity", "Background location granted")
                loadGeofencesFromFirebase()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                requestBackgroundLocationPermission()
            }
            else -> Log.e("MainActivity", "Location permissions not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        geofencingClient = LocationServices.getGeofencingClient(this)

        setupToolbarAndDrawer()
        checkPermissionsAndLoadGeofences()
        setupOverviewCards()
        setupViewPager()
        loadUserData()
        //loadSeededData()
    }

    private fun setupToolbarAndDrawer(){
        setSupportActionBar(binding.toolbar) // Set the toolbar to act as the ActionBar
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar, // Pass the toolbar here
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.title = "Campus Space"
        supportActionBar?.subtitle = "Campus Crowd Tracker"

        // --- Handle Clicks on Drawer Items ---
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    //  Handle Clicks on Drawer Menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                // Example: Intent to a ProfileActivity
                // val intent = Intent(this, ProfileActivity::class.java)
                // startActivity(intent)
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
            }
            R.id.action_logout -> {
                showLogoutDialog()
            }
            R.id.nav_share -> {
                Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show()
            }
        }
        // Close the drawer when an item is tapped
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupOverviewCards() {
        FirebaseDB.instance.collection("places")
            .addSnapshotListener { querySnapshot, exception ->

                if (exception != null) {
                    Log.e("MainActivity", "Listen failed.", exception)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val places = querySnapshot.toObjects(Place::class.java)

                    val totalCapacity = places.sumOf { it.capacity ?: 0 }
                    val totalOccupancy = places.sumOf { it.currentOccupancy ?: 0 }
                    val availableSpots = totalCapacity - totalOccupancy
                    val occupancyPercentage = if (totalCapacity > 0) {
                        (totalOccupancy.toFloat() / totalCapacity * 100).toInt()
                    } else {
                        0
                    }

                    // Use binding object for direct, safe, and fast view access
                    binding.tvOccupancyPercentage.text = "$occupancyPercentage%"
                    binding.tvOccupancyTotal.text = "$totalOccupancy/$totalCapacity spots"
                    binding.tvAvailableSpots.text = availableSpots.toString()
                    binding.tvAvailableLocations.text = "Across ${places.size} locations"

                    val spaceList = places.mapNotNull { place ->
                        if ((place.capacity ?: 0) > 0) {
                            val name = place.name ?: ""
                            val available = (place.capacity ?: 0) - (place.currentOccupancy ?: 0)
                            val percentage = (place.currentOccupancy?.toDouble() ?: 0.0) / (place.capacity ?: 1) * 100
                            Triple(name, percentage, available)
                        } else {
                            null
                        }
                    }

                    if (spaceList.isNotEmpty()) {
                        val busiest = spaceList.maxByOrNull { it.second }
                        val leastBusy = spaceList.minByOrNull { it.second }

                        // Use binding object here as well
                        binding.tvBusiest.text = busiest?.first
                        binding.tvBusiestPercentage.text = "${busiest?.second?.toInt()}%"
                        binding.tvBestOption.text = leastBusy?.first
                        binding.tvBestOptionAvailable.text = "${leastBusy?.third} spots available"
                    }
                } else {
                    Log.d("MainActivity", "No places found")
                    updateUIforEmptyState()
                }
            }
    }

    private fun updateUIforEmptyState(){
        binding.tvOccupancyPercentage.text = "0%"
        binding.tvOccupancyTotal.text = "0/0 people"
        binding.tvAvailableSpots.text = "0"
        binding.tvAvailableLocations.text = "Across 0 locations"
        binding.tvBusiest.text = "N/A"
        binding.tvBusiestPercentage.text = ""
        binding.tvBestOption.text = "N/A"
        binding.tvBestOptionAvailable.text = ""
    }
    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(PlacesListFragment(), "Locations")
        adapter.addFragment(CampusMapFragment(), "Campus Map")

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }

    fun switchToMapTab() {
        binding.viewPager.currentItem = 1
    }

    private fun checkPermissionsAndLoadGeofences() {
        val hasFineLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
        val hasBackgroundLocation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else true
//        else PackageManager.PERMISSION_GRANTED
//        when {
//            fine == PackageManager.PERMISSION_GRANTED && background == PackageManager.PERMISSION_GRANTED -> loadGeofencesFromFirebase()
//            fine == PackageManager.PERMISSION_GRANTED -> requestBackgroundLocationPermission()
//            else -> requestFineLocationPermission()
//        }
        if (hasFineLocation && hasBackgroundLocation) {
            loadGeofencesFromFirebase()
        } else if (hasFineLocation) {
            requestBackgroundLocationPermission()
        } else {
            requestFineLocationPermission()
        }
    }

    private fun requestFineLocationPermission() {
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
    }

    private fun loadGeofencesFromFirebase() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("places").get()
            .addOnSuccessListener { result ->
                geofenceList.clear()
                for (doc in result) {
                    val area = GeofenceArea(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        radiusMeters = doc.getDouble("radiusMeters") ?: 100.0
                    )

                    geofenceList.add(
                        Geofence.Builder()
                            .setRequestId(area.id)
                            .setCircularRegion(area.latitude, area.longitude, area.radiusMeters.toFloat())
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(
                                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                            )
                            .build()
                    )
                }
                Log.d("MainActivity", "Loaded ${geofenceList.size} geofences from DB")
                addGeofencesInternal()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to load geofences: ${e.message}")
            }
    }

    private fun getGeofencingRequest(): GeofencingRequest =
        GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()

    @SuppressLint("MissingPermission")
    private fun addGeofencesInternal() {
        if (geofenceList.isEmpty()) return
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)
            .addOnSuccessListener { Log.d("MainActivity", "Geofences added") }
            .addOnFailureListener { e -> Log.e("MainActivity", "Failed to add geofences: ${e.message}") }
    }

    private fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener { Log.d("MainActivity", "Geofences removed") }
            addOnFailureListener { e -> Log.e("MainActivity", "Failed to remove geofences: ${e.message}") }
        }
    }

    private fun loadSeededData(){
        val allPlaces = MockData.getPlaces()
        val db = FirebaseDB.instance
        for (place in allPlaces) {
            db.collection("places").document(place.id.toString()).set(place)
        }
    }

    private fun loadUserData() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        // Proceed only if the user is logged in
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Data fetch was successful
                        val fullName = document.getString("fullName") ?: "User Name"
                        val email = document.getString("email") ?: "user.email@example.com"

                        // Get a reference to the header view
                        val headerView = binding.navigationView.getHeaderView(0)
                        val userNameTextView = headerView.findViewById<TextView>(R.id.user_name)
                        val userEmailTextView = headerView.findViewById<TextView>(R.id.user_email)

                        // Update the TextViews with the fetched data
                        userNameTextView.text = fullName
                        userEmailTextView.text = email

                    } else {
                        Log.d("MainActivity", "No such document for user")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("MainActivity", "get failed with ", exception)
                }
        }
    }
}


