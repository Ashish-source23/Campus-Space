package com.example.campusspace.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // <-- 1. ADD IMPORT
import com.example.campusspace.R
import com.example.campusspace.data.Place // <-- 2. ADD IMPORT (for the shared class)
import com.example.campusspace.databinding.FragmentCampusMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ListenerRegistration // <-- Keep this
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // For permission request
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class CampusMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var userLocationMarker: Marker? = null
    private var userAccuracyCircle: Circle? = null // To track the radius circle
    private val currentUserName: String = "User"
    private var firestoreListener: ListenerRegistration? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your position", Toast.LENGTH_SHORT).show()
            }
        }

    // --- 3. ADD VIEWMODEL ---
    // Get the *same* Activity-scoped ViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var markersOnMap: MutableList<Marker> = mutableListOf()

    // --- 4. DELETED ---
    // The internal `data class Place` is GONE. We now use the imported one.

    // ... (originalMapParams, originalCardHeight, if you have them) ...

    companion object {
        private const val MIN_ZOOM = 15.5f
        private const val MAX_ZOOM = 18.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // ... (All your listeners for zoom, fullscreen, etc. remain here) ...
        binding.btnZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        binding.btnZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // (Fullscreen listeners, etc.)

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // ... (All your map setup: mapType, buildings, zoom, tilt, camera) ...
        googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap?.isBuildingsEnabled = true
        googleMap?.setMinZoomPreference(MIN_ZOOM)
        googleMap?.setMaxZoomPreference(MAX_ZOOM)

        enableMyLocation()

        val campusLocation = LatLng(30.9686169, 76.473305)
        val cameraPosition = CameraPosition.Builder()
            .target(campusLocation)
            .zoom(17f)
            .tilt(45f)
            .build()
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        // ... (Your existing setOnMarkerClickListener) ...
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
        // This fixes the "Button not working" issue by manually handling the click
        googleMap?.setOnMyLocationButtonClickListener {
            getDeviceLocationAndCenter()
            true // Return true to consume the event (we handle the move, not the map)
        }

        enableMyLocation()
        fetchAndPopulateMapData()

        // --- 5. ADD THIS ---
        // Start listening for focus requests
        observeViewModelForFocus()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocationAndCenter() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)

                // 1. GET THE NAME SAFELY
                val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val displayName = authUser?.displayName

                // Logic: Use Name -> If null use Email -> If null use "You"
                val nameToShow = when {
                    !displayName.isNullOrEmpty() -> displayName
                    !authUser?.email.isNullOrEmpty() -> authUser?.email?.substringBefore("@") // Use part before @
                    else -> "You"
                }

                // 2. CLEAR OLD MARKERS
                userLocationMarker?.remove()
                userAccuracyCircle?.remove()

                // 3. ADD RADIUS CIRCLE
                userAccuracyCircle = googleMap?.addCircle(
                    CircleOptions()
                        .center(userLatLng)
                        .radius(30.0)
                        .strokeWidth(2f)
                        .strokeColor(0xFF4285F4.toInt())
                        .fillColor(0x224285F4.toInt())
                )

                // 4. ADD DOT MARKER WITH NAME
                val markerOptions = MarkerOptions()
                    .position(userLatLng)
                    .title("$nameToShow is here") // Now guaranteed to have text
                    .icon(createCustomLocationDot())
                    .anchor(0.5f, 0.5f)
                    .zIndex(2.0f)

                userLocationMarker = googleMap?.addMarker(markerOptions)

                // 5. ANIMATE AND SHOW CARD
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 17.5f)
                )
                userLocationMarker?.showInfoWindow()

            } else {
                Toast.makeText(requireContext(), "Waiting for location signal...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createCustomLocationDot(): BitmapDescriptor {
        val diameter = 40 // Size of the dot in pixels (Adjust this to make it smaller/larger)
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        // 1. Draw the white border
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)

        // 2. Draw the inner colored dot (Theme Color)
        // You can change 0xFF4285F4 to ContextCompat.getColor(requireContext(), R.color.your_theme_color)
        paint.color = 0xFF4285F4.toInt() // Google Blue
        val innerRadius = (diameter / 2f) - 4 // 4px border width
        canvas.drawCircle(diameter / 2f, diameter / 2f, innerRadius, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // --- 6. ADD THIS NEW FUNCTION ---
    /**
     * Observes the SharedViewModel. When a place is selected,
     * this fragment will focus on it and then clear the event.
     */
    private fun observeViewModelForFocus() {
        sharedViewModel.selectedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null) {
                // We have a place to focus on
                focusOnPlace(place)
                // Clear the event so it doesn't re-trigger
                sharedViewModel.clearSelectedPlace()
            }
        }
    }

    // --- 7. ADD THIS NEW FUNCTION ---
    /**
     * Moves the camera to center on the given Place.
     */
    private fun focusOnPlace(place: Place) {
        val map = googleMap ?: return // Don't do anything if map isn't ready

        // Check against the 0.0 default, not null
        if (place.latitude == 0.0 || place.longitude == 0.0) {
            Log.w("CampusMapFragment", "Selected place has no valid coordinates: ${place.name}")
            return
        }

        val position = LatLng(place.latitude, place.longitude)

        // Get current camera settings to keep the tilt
        val currentCameraPosition = map.cameraPosition

        // Build a new camera position that centers on the place
        val newCameraPosition = CameraPosition.Builder()
            .target(position)
            .tilt(currentCameraPosition?.tilt ?: 45f) // Keep existing tilt
            .zoom(17.5f) // Zoom in a bit closer
            .build()

        // Animate to the new position (1 second duration)
        map.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 1000, null)

        val markerToClick = markersOnMap.find { marker ->
            val markerPlace = marker.tag as? Place
            markerPlace?.latitude == place.latitude && markerPlace?.longitude == place.longitude
        }

        // Show its info window, as if the user clicked it.
        markerToClick?.showInfoWindow()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        // ... (no changes)
        val map = googleMap ?: return
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        } else {
            Log.d("CampusMapFragment", "Location permission not granted. MyLocation button disabled.")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    }

    // --- 8. THIS FUNCTION IS MODIFIED ---
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
                    Log.d("CampusMapFragment", "Current data: null or empty")
                    googleMap?.clear()
                    return@addSnapshotListener
                }

                // This now correctly uses the imported `data.Place` class
                val places = snapshot.toObjects(Place::class.java)

                populateMapMarkers(places)
                highlightAreas(places)
            }
    }

    // --- 9. THIS FUNCTION IS MODIFIED ---
    @SuppressLint("SuspiciousIndiciousIndentation")
    private fun populateMapMarkers(places: List<Place>) {
        val map = googleMap ?: return
        map.clear() // Clear existing markers before repopulating
        markersOnMap.clear()

        places.forEach { place ->
            // Check against default 0.0, not null
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)

                // Use `Int?` fields, not `Long?`
                val capacity = place.capacity ?: 0
                val currentOccupancy = place.currentOccupancy ?: 0

                val occupancyPercent = if (capacity > 0) {
                    (currentOccupancy.toFloat() / capacity.toFloat()) * 100f
                } else {
                    0f
                }

                val markerText = "${occupancyPercent.toInt()}%"
                val icon = createCustomMarkerIcon(markerText, occupancyPercent.toInt())

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(place.name)
                        .snippet("Occupancy: $currentOccupancy / $capacity (${occupancyPercent.toInt()}%)")
                        .icon(icon)
                )

                // Now, tag the marker with its data and add it to our list
                if (marker != null) {
                    marker.tag = place // Tag the marker with the full Place object
                    markersOnMap.add(marker) // Add it to our tracking list
                }

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(place.name)
                        .snippet("Occupancy: $currentOccupancy / $capacity (${occupancyPercent.toInt()}%)")
                        .icon(icon)
                )
            }
        }
    }

    // --- 10. THIS FUNCTION IS MODIFIED ---
    private fun highlightAreas(places: List<Place>) {
        val map = googleMap ?: return

        // Note: We clear circles inside populateMapMarkers' map.clear() call

        places.forEach { place ->
            // Check against default 0.0, not null
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)

                // Use `Int?` fields
                val capacity = place.capacity?.toFloat() ?: 0f
                val occupancy = if (capacity > 0f) {
                    (place.currentOccupancy?.toFloat() ?: 0f) / capacity * 100f
                } else {
                    0f
                }

                val (fillColor, strokeColor) = when {
                    occupancy < 30 -> Pair(0x3300FF00, 0x9900FF00)
                    occupancy < 70 -> Pair(0x33FFFF00, 0x99FFFF00)
                    else -> Pair(0x33FF0000, 0x99FF0000)
                }

                // Use `radiusMeters` field
                val circleRadius = place.radiusMeters ?: 25.0 // Default to 25m

                map.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(circleRadius.toDouble()) // Use dynamic radius
                        .fillColor(fillColor.toInt())
                        .strokeColor(strokeColor.toInt())
                        .strokeWidth(2f)
                )
            }
        }
    }


    // --- (This function is fine, no changes) ---
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

    // ... (onResume, onStart, onStop, onPause, onLowMemory, onSaveInstanceState) ...
    // ... (No changes needed in these lifecycle methods) ...

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
        firestoreListener?.remove() // Make sure this is here
        googleMap = null
        _binding = null
    }
}