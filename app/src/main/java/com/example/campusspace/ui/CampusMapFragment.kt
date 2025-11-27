package com.example.campusspace.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.FragmentCampusMapBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CampusMapFragment : Fragment(), OnMapReadyCallback {

    // --- Binding & UI Variables ---
    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    // --- Location Variables ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Markers for the user (Blue Dot & Accuracy Circle)
    private var userLocationMarker: Marker? = null
    private var userAccuracyCircle: Circle? = null
    private var lastKnownLocation: Location? = null
    private var isFirstLocationUpdate = true

    // --- Data Variables ---
    private var currentPlaceList: List<Place> = emptyList()
    private var markersOnMap: MutableList<Marker> = mutableListOf()
    private var firestoreListener: ListenerRegistration? = null

    // --- ViewModel ---
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // --- Permission Launcher ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required for navigation", Toast.LENGTH_SHORT).show()
            }
        }

    companion object {
        private const val MIN_ZOOM = 15.5f
        private const val MAX_ZOOM = 21.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)

        // 1. Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 2. Setup the Logic for Live Updates
        setupLocationUpdatesLogic()

        // 3. Initialize Map
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 4. Setup Buttons
        binding.btnZoomIn.setOnClickListener { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) }
        binding.btnZoomOut.setOnClickListener { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }

        // Your specific MyLocation button (FAB)
        binding.fabMyLocation?.setOnClickListener {
            getDeviceLocationAndCenter()
        }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Map Configuration
        googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap?.isBuildingsEnabled = true
        googleMap?.setMinZoomPreference(MIN_ZOOM)
        googleMap?.setMaxZoomPreference(MAX_ZOOM)

        // Disable the default Google "Blue Dot" because we are drawing a custom one
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap?.isMyLocationEnabled = false
            }
        } catch (e: Exception) {
            Log.e("CampusMapFragment", "Error setting isMyLocationEnabled", e)
        }

        // Default Camera Position (Campus Center)
        val campusLocation = LatLng(30.9686169, 76.473305)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocation, 17f))

        // Handle native GPS button click (if enabled later)
        googleMap?.setOnMyLocationButtonClickListener {
            getDeviceLocationAndCenter()
            true
        }

        // Marker Click Listener
        googleMap?.setOnMarkerClickListener { marker ->
            val currentCameraPosition = googleMap?.cameraPosition
            val newCameraPosition = CameraPosition.Builder()
                .target(marker.position)
                .tilt(currentCameraPosition?.tilt ?: 45f)
                .zoom(currentCameraPosition?.zoom ?: 17f)
                .build()
            googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
            marker.showInfoWindow()
            true
        }

        // Fade out loading screen when map loads
        googleMap?.setOnMapLoadedCallback {
            _binding?.loadingOverlay?.animate()?.alpha(0f)?.setDuration(500)?.withEndAction {
                _binding?.loadingOverlay?.visibility = View.GONE
            }
        }

        // Start Processes
        enableMyLocation()
        fetchAndPopulateMapData()
        observeViewModelForFocus()
    }

    // --- LIVE LOCATION LOGIC ---

    private fun setupLocationUpdatesLogic() {
        // Request high accuracy updates every 5 seconds
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // 1. Update the Blue Dot and Circle
                    updateUserMarkerUI(location)

                    // 2. If this is the first time we found the user, zoom to them
                    if (isFirstLocationUpdate) {
                        isFirstLocationUpdate = false
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 17.5f
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Updates the custom blue dot marker and the accuracy circle.
     * Also refreshes distance snippets on building markers.
     */
    private fun updateUserMarkerUI(location: Location) {
        lastKnownLocation = location
        val userLatLng = LatLng(location.latitude, location.longitude)
        val map = googleMap ?: return

        // Update Accuracy Circle
        if (userAccuracyCircle == null) {
            userAccuracyCircle = map.addCircle(
                CircleOptions()
                    .center(userLatLng)
                    .radius(30.0) // Fixed radius for visibility, or use location.accuracy
                    .strokeWidth(2f)
                    .strokeColor(0xFF4285F4.toInt())
                    .fillColor(0x224285F4.toInt())
            )
        } else {
            userAccuracyCircle?.center = userLatLng
        }

        // Update User Dot Marker
        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(userLatLng)
                .title("You are here")
                .icon(createCustomLocationDot())
                .anchor(0.5f, 0.5f)
                .zIndex(2.0f) // Keep it above other markers
            userLocationMarker = map.addMarker(markerOptions)
        } else {
            // Smoothly update position without removing the marker
            userLocationMarker?.position = userLatLng
        }

        // Update distances on building markers as user moves
        updateMarkerSnippetsOnly()
    }

    /**
     * Loops through existing markers and updates their snippet text (Distance)
     * without reloading the whole map.
     */
    private fun updateMarkerSnippetsOnly() {
        markersOnMap.forEach { marker ->
            val place = marker.tag as? Place
            if (place != null) {
                val distText = calculateDistanceText(place.latitude, place.longitude)
                val capacity = place.capacity ?: 0
                val current = place.currentOccupancy ?: 0

                val snippetText = if (distText.isNotEmpty()) {
                    "$distText away | Occ: $current/$capacity"
                } else {
                    "Occupancy: $current/$capacity"
                }

                marker.snippet = snippetText

                // If the user has this marker clicked open, refresh the window
                if (marker.isInfoWindowShown) {
                    marker.showInfoWindow()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocationAndCenter() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        // Force an immediate single update
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateUserMarkerUI(location)
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 17.5f
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Disable default layer so we can use our custom marker
            googleMap?.isMyLocationEnabled = false
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // --- FIRESTORE & MAP POPULATION ---

    private fun fetchAndPopulateMapData() {
        val db = Firebase.firestore
        firestoreListener?.remove()

        firestoreListener = db.collection("places")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("CampusMapFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    googleMap?.clear()
                    return@addSnapshotListener
                }

                val places = snapshot.toObjects(Place::class.java)
                populateMapMarkers(places)
            }
    }

    private fun populateMapMarkers(places: List<Place>) {
        val map = googleMap ?: return

        // 1. Clear Map but SAVE user marker logic
        map.clear()
        markersOnMap.clear()

        // 2. Reset user marker references because map.clear() removed them from the map
        userLocationMarker = null
        userAccuracyCircle = null

        // 3. Re-draw user marker if we have a last known location
        lastKnownLocation?.let { updateUserMarkerUI(it) }

        currentPlaceList = places

        // 4. Draw Building Markers
        places.forEach { place ->
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)
                val capacity = place.capacity ?: 0
                val currentOccupancy = place.currentOccupancy ?: 0

                val occupancyPercent = if (capacity > 0) {
                    (currentOccupancy.toFloat() / capacity.toFloat()) * 100f
                } else { 0f }

                val distanceText = calculateDistanceText(place.latitude, place.longitude)
                val snippetText = if (distanceText.isNotEmpty()) {
                    "$distanceText away | Occ: $currentOccupancy/$capacity"
                } else {
                    "Occupancy: $currentOccupancy/$capacity"
                }

                val markerText = "${occupancyPercent.toInt()}%"
                val icon = createCustomMarkerIcon(markerText, occupancyPercent.toInt())

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(place.name)
                        .snippet(snippetText)
                        .icon(icon)
                )

                if (marker != null) {
                    marker.tag = place
                    markersOnMap.add(marker)
                }
            }
        }

        // 5. Draw Circles
        highlightAreas(places)
    }

    private fun highlightAreas(places: List<Place>) {
        val map = googleMap ?: return
        places.forEach { place ->
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)
                val capacity = place.capacity?.toFloat() ?: 0f
                val occupancy = if (capacity > 0f) {
                    (place.currentOccupancy?.toFloat() ?: 0f) / capacity * 100f
                } else { 0f }

                val (fillColor, strokeColor) = when {
                    occupancy < 30 -> Pair(0x3300FF00, 0x9900FF00)
                    occupancy < 70 -> Pair(0x33FFFF00, 0x99FFFF00)
                    else -> Pair(0x33FF0000, 0x99FF0000)
                }

                val circleRadius = place.radiusMeters ?: 25.0

                map.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(circleRadius.toDouble())
                        .fillColor(fillColor.toInt())
                        .strokeColor(strokeColor.toInt())
                        .strokeWidth(2f)
                )
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun observeViewModelForFocus() {
        sharedViewModel.selectedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null) {
                focusOnPlace(place)
                sharedViewModel.clearSelectedPlace()
            }
        }
    }

    private fun focusOnPlace(place: Place) {
        val map = googleMap ?: return
        if (place.latitude == 0.0 || place.longitude == 0.0) return

        val position = LatLng(place.latitude, place.longitude)
        val currentCameraPosition = map.cameraPosition

        val newCameraPosition = CameraPosition.Builder()
            .target(position)
            .tilt(currentCameraPosition?.tilt ?: 45f)
            .zoom(17.5f)
            .build()

        map.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 1000, null)

        val markerToClick = markersOnMap.find { marker ->
            val markerPlace = marker.tag as? Place
            markerPlace?.latitude == place.latitude && markerPlace?.longitude == place.longitude
        }
        markerToClick?.showInfoWindow()
    }

    private fun calculateDistanceText(destLat: Double, destLng: Double): String {
        val userLoc = lastKnownLocation ?: return ""
        val results = FloatArray(1)
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, destLat, destLng, results)
        val distanceInMeters = results[0]
        return if (distanceInMeters > 1000) {
            String.format("%.1f km", distanceInMeters / 1000)
        } else {
            "${distanceInMeters.toInt()} m"
        }
    }

    private fun createCustomLocationDot(): BitmapDescriptor {
        val diameter = 40
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        // White border
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)

        // Blue inner dot
        paint.color = 0xFF4285F4.toInt()
        val innerRadius = (diameter / 2f) - 4
        canvas.drawCircle(diameter / 2f, diameter / 2f, innerRadius, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun createCustomMarkerIcon(text: String, occupancyPercent: Int): BitmapDescriptor {
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_layout, null)
        val markerTextView = markerView.findViewById<TextView>(R.id.marker_text)

        val backgroundResId = when {
            occupancyPercent < 30 -> R.drawable.shape_circle_green
            occupancyPercent < 70 -> R.drawable.shape_circle_yellow
            else -> R.drawable.shape_circle_red
        }
        markerTextView.text = text
        markerTextView.background = ContextCompat.getDrawable(requireContext(), backgroundResId)

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // --- LIFECYCLE ---

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdates() // Start tracking when visible
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates() // Save battery when backgrounded
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
        stopLocationUpdates()
        firestoreListener?.remove()
        googleMap = null
        _binding = null
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}