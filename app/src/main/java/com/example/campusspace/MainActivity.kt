package com.example.campusspace // Match your package name from the error

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.campusspace.databinding.ActivityMainBinding // Correct import
import com.example.campusspace.entity.GeofenceArea
import com.example.campusspace.services.GeofenceBroadcastReceiver
import com.example.campusspace.ui.CampusMapFragment
import com.example.campusspace.ui.PlacesListFragment
import com.example.campusspace.ui.MockData
import com.example.campusspace.ui.ViewPagerAdapter
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient
    private val geofenceList = mutableListOf<Geofence>()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.example.campusSpace.ACTION_GEOFENCE_EVENT"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // This is the correct line 14

        setupOverviewCards()
        setupViewPager()
        geofencingClient = LocationServices.getGeofencingClient(this)
        FirebaseAuth.getInstance().signInAnonymously()
        checkPermissionsAndLoadGeofences()
    }

    private fun setupOverviewCards() {
        val places = MockData.getPlaces()
        val totalCapacity = places.sumOf { it.capacity }
        val totalOccupancy = places.sumOf { it.currentOccupancy }
        val availableSpots = totalCapacity - totalOccupancy
        val occupancyPercentage = (totalOccupancy.toFloat() / totalCapacity * 100).toInt()

        binding.tvOccupancyPercentage.text = "$occupancyPercentage%"
        binding.tvOccupancyTotal.text = "$totalOccupancy/$totalCapacity people"
        binding.tvAvailableSpots.text = availableSpots.toString()
        binding.tvAvailableLocations.text = "Across ${places.size} locations"
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
        firestore.collection("areas").get()
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
