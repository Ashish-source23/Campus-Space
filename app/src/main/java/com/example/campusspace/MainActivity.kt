package com.example.campusspace // Match your package name from the error

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.ActivityMainBinding // Correct import
import com.example.campusspace.entity.GeofenceArea
import com.example.campusspace.services.GeofenceBroadcastReceiver
import com.example.campusspace.ui.WelcomeActivity
import com.example.campusspace.ui.CampusMapFragment
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
import com.example.campusspace.utils.showLogoutDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient
    private val geofenceList = mutableListOf<Geofence>()

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

// In D:/branch/Campus-Space/app/src/main/java/com/example/campusspace/MainActivity.kt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        FirebaseAuth.getInstance().signInAnonymously()

        geofencingClient = LocationServices.getGeofencingClient(this)
        checkPermissionsAndLoadGeofences()
        setupOverviewCards()
        setupViewPager()
        setupToolbarMenu()

    }

//    Logout function

        private fun setupToolbarMenu() {
            binding.toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_logout -> {
                        showLogoutDialog(this)  // ✅ One line does it all
                        true
                    }
                    else -> false
                }
            }
        }

    //    Function to set the Dashboard Top Cards
    private fun setupOverviewCards() {
        FirebaseDB.instance.collection("places")
            .addSnapshotListener { querySnapshot, exception ->

                // Handle potential errors
                if (exception != null) {
                    Log.e("MainActivity", "Listen failed.", exception)
                    return@addSnapshotListener
                }

                // Handle case where there's data
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val places = querySnapshot.toObjects(Place::class.java)

                    val totalCapacity = places.sumOf { it.capacity ?: 0 } // Simpler safe call
                    val totalOccupancy = places.sumOf { it.currentOccupancy ?: 0 } // Simpler safe call

                    val availableSpots = totalCapacity - totalOccupancy
                    val occupancyPercentage = if (totalCapacity > 0) {
                        (totalOccupancy.toFloat() / totalCapacity * 100).toInt()
                    } else {
                        0
                    }

                    val occupancy_percentageView = findViewById<TextView>(R.id.tv_occupancy_percentage)
                    val tv_occupancy_totalView = findViewById<TextView>(R.id.tv_occupancy_total)
                    val tv_available_spotsView = findViewById<TextView>(R.id.tv_available_spots)
                    val tv_available_locationsView = findViewById<TextView>(R.id.tv_available_locations)


                    occupancy_percentageView.text = "$occupancyPercentage%"
                    tv_occupancy_totalView.text = "$totalOccupancy/$totalCapacity people"
                    tv_available_spotsView.text = availableSpots.toString()
                    tv_available_locationsView.text = "Across ${places.size} locations"



                    val spaceList = mutableListOf<Triple<String, Double, Int>>() //name + percentage + remaining

                    for (place in querySnapshot) {
                            val spot = place.toObject(Place::class.java)

                            if (spot!= null && (spot.capacity?:0)>0) {
                                val name= spot.name?:""
                                val availableSpots = spot.capacity!! - spot.currentOccupancy!!
                                val percentage =
                                    (spot.currentOccupancy!!.toDouble() / spot.capacity!!) * 100
                                spaceList.add(Triple(name, percentage, availableSpots))
                            }
                        }

                    if(spaceList.isNotEmpty()){
                        val busiest = spaceList.maxByOrNull { it.second }
                        val leastBusy = spaceList.minByOrNull { it.second }

                        // Display in your TextViews
                        val busiestView = findViewById<TextView>(R.id.tv_busiest)
                        val busiestByPercentage = findViewById<TextView>(R.id.tv_busiest_percentage)
                        val best = findViewById<TextView>(R.id.tv_bestOption)
                        val bestOption = findViewById<TextView>(R.id.tv_bestOption_available)

                        busiestView.text = "${busiest?.first}"
                        busiestByPercentage.text= "${busiest?.second?.toInt()}%"
                        best.text = "${leastBusy?.first}"
                        bestOption.text= "${leastBusy?.third} spots available"
                    }
                } else {
                    // Handle case where there's no data
                    Log.d("MainActivity", "No places found")
                    binding.tvOccupancyPercentage.text = "0%"
                    binding.tvOccupancyTotal.text = "0/0 people"
                    binding.tvAvailableSpots.text = "0"
                    binding.tvAvailableLocations.text = "Across 0 locations"
                }
            }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(PlacesListFragment(), "Study Locations")
        adapter.addFragment(CampusMapFragment(), "Campus Map")
        // You can add the CampusMapFragment here later
        // adapter.addFragment(CampusMapFragment(), "Campus Map")

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }

    fun switchToMapTab() {
        // 1 is the index of your map tab (0 is Study Locations)
        binding.viewPager.currentItem = 1
    }

    private fun checkPermissionsAndLoadGeofences() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else PackageManager.PERMISSION_GRANTED

        when {
            fine == PackageManager.PERMISSION_GRANTED && background == PackageManager.PERMISSION_GRANTED -> loadGeofencesFromFirebase()
            fine == PackageManager.PERMISSION_GRANTED -> requestBackgroundLocationPermission()
            else -> requestFineLocationPermission()
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
}