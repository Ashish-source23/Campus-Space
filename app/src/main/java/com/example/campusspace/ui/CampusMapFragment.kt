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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.FragmentCampusMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CampusMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    // User location markers
    private var userLocationMarker: Marker? = null
    private var userAccuracyCircle: Circle? = null

    // Firestore & Logic
    private var firestoreListener: ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var markersOnMap: MutableList<Marker> = mutableListOf()

    // ViewModel for communicating with other fragments
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
                getDeviceLocationAndCenter()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your position", Toast.LENGTH_SHORT).show()
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

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize MapView
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // --- Button Listeners ---

        // Zoom In
        binding.btnZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        // Zoom Out
        binding.btnZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // Refresh Location (The new button you added in XML)
        // Using findViewById because it might be manually added inside the FrameLayout in XML
        val refreshBtn = binding.root.findViewById<View>(R.id.btnRefreshLocation)
        refreshBtn?.setOnClickListener {
            Log.d("CampusMapFragment", "Refresh Button Clicked!")
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

        // Initial Camera Position (Campus Center)
        val campusLocation = LatLng(30.9686169, 76.473305)
        val cameraPosition = CameraPosition.Builder()
            .target(campusLocation)
            .zoom(17f)
            .tilt(45f)
            .build()

        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        // Handle Marker Clicks
        googleMap?.setOnMarkerClickListener { marker ->
            // Logic: Center on marker but keep tilt/zoom
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

        // Handle Default "My Location" button click (if enabled)
        googleMap?.setOnMyLocationButtonClickListener {
            getDeviceLocationAndCenter()
            true
        }

        // Setup User Location features
        enableMyLocation()

        // Attempt to center location immediately on load
        getDeviceLocationAndCenter()

        // Start fetching data from Firebase
        fetchAndPopulateMapData()

        // Observe ViewModel for focus requests (e.g. from Search)
        observeViewModelForFocus()
    }

    /**
     * Logic to get current GPS location, draw a custom marker, and center the camera.
     * Tries lastLocation first for speed, falls back to getCurrentLocation.
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceLocationAndCenter() {
        // 1. Check Permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        Toast.makeText(requireContext(), "Updating location...", Toast.LENGTH_SHORT).show()

        // 2. Try getting the LAST known location first (Instant)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateUserMarker(location)
            } else {
                // 3. If last location is null (rare), force a new update
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                fusedLocationClient.getCurrentLocation(priority, null)
                    .addOnSuccessListener { newLocation ->
                        if (newLocation != null) {
                            updateUserMarker(newLocation)
                        } else {
                            Toast.makeText(requireContext(), "GPS signal not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CampusMapFragment", "Error getting fresh location: ${e.message}")
                        Toast.makeText(requireContext(), "Error getting location", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("CampusMapFragment", "Error getting last location: ${e.message}")
        }
    }

    /**
     * Helper function to draw the custom marker on the map.
     */
    private fun updateUserMarker(location: android.location.Location) {
        val userLatLng = LatLng(location.latitude, location.longitude)

        // Determine User Name
        val authUser = FirebaseAuth.getInstance().currentUser
        val displayName = authUser?.displayName
        val nameToShow = when {
            !displayName.isNullOrEmpty() -> displayName
            !authUser?.email.isNullOrEmpty() -> authUser?.email?.substringBefore("@")
            else -> "You"
        }

        // Remove old markers
        userLocationMarker?.remove()
        userAccuracyCircle?.remove()

        // Add Accuracy Circle
        userAccuracyCircle = googleMap?.addCircle(
            CircleOptions()
                .center(userLatLng)
                .radius(30.0) // Fixed radius for visual effect
                .strokeWidth(2f)
                .strokeColor(0xFF4285F4.toInt())
                .fillColor(0x224285F4.toInt())
        )

        // Add Custom Dot Marker
        val markerOptions = MarkerOptions()
            .position(userLatLng)
            .title("$nameToShow")
            .icon(createCustomLocationDot())
            .anchor(0.5f, 0.5f)
            .zIndex(2.0f)

        userLocationMarker = googleMap?.addMarker(markerOptions)

        // Animate Camera
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(userLatLng, 17.5f)
        )
        userLocationMarker?.showInfoWindow()
    }

    /**
     * Creates a custom blue dot bitmap for the user location.
     */
    private fun createCustomLocationDot(): BitmapDescriptor {
        val diameter = 40
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        // Draw White Border
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)

        // Draw Blue Inner Dot
        paint.color = 0xFF4285F4.toInt()
        val innerRadius = (diameter / 2f) - 4
        canvas.drawCircle(diameter / 2f, diameter / 2f, innerRadius, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /**
     * Observes the SharedViewModel for place selection events.
     */
    private fun observeViewModelForFocus() {
        sharedViewModel.selectedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null) {
                focusOnPlace(place)
                sharedViewModel.clearSelectedPlace()
            }
        }
    }

    /**
     * Focuses the camera on a specific Place object.
     */
    private fun focusOnPlace(place: Place) {
        val map = googleMap ?: return

        if (place.latitude == 0.0 || place.longitude == 0.0) {
            Log.w("CampusMapFragment", "Invalid coordinates for: ${place.name}")
            return
        }

        val position = LatLng(place.latitude, place.longitude)
        val currentCameraPosition = map.cameraPosition

        val newCameraPosition = CameraPosition.Builder()
            .target(position)
            .tilt(currentCameraPosition?.tilt ?: 45f)
            .zoom(17.5f)
            .build()

        map.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 1000, null)

        // Find and open the marker for this place
        val markerToClick = markersOnMap.find { marker ->
            val markerPlace = marker.tag as? Place
            // Safe check if tag is null or different type
            if (markerPlace != null) {
                markerPlace.latitude == place.latitude && markerPlace.longitude == place.longitude
            } else {
                false
            }
        }
        markerToClick?.showInfoWindow()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val map = googleMap ?: return
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enable the layer so we get the Blue Dot
            map.isMyLocationEnabled = true
            // Hide the default Google button because we made our own FAB
            map.uiSettings.isMyLocationButtonEnabled = false
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Fetches places from Firebase Firestore and adds them to the map.
     */
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
                    markersOnMap.clear()
                    return@addSnapshotListener
                }

                val places = snapshot.toObjects(Place::class.java)
                populateMapMarkers(places)
                highlightAreas(places)
            }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun populateMapMarkers(places: List<Place>) {
        val map = googleMap ?: return

        // Clear markers list logic, but careful not to wipe user location marker if not intended
        // map.clear() wipes everything including circles and user marker if managed by map.addMarker
        // Since we manage userLocationMarker separately, we might need to re-add it if we call map.clear()

        // Strategy: Clear map completely, then re-add data markers.
        // Note: This removes the user custom marker too, so we won't call map.clear() broadly if we want to keep user marker.
        // Instead, let's just remove tracked markers.

        for (marker in markersOnMap) {
            marker.remove()
        }
        markersOnMap.clear()

        // Note: If you used map.clear() before, you'd lose the user dot.
        // If you really want a fresh slate, use map.clear() and then call getDeviceLocationAndCenter() again silently.
        // For now, we just iterate places.

        places.forEach { place ->
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)
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

                if (marker != null) {
                    marker.tag = place
                    markersOnMap.add(marker)
                }
            }
        }
    }

    private fun highlightAreas(places: List<Place>) {
        val map = googleMap ?: return

        // Note: If we aren't using map.clear(), previous circles might persist.
        // Ideally, store circles in a list and remove them here like we did with markers.

        places.forEach { place ->
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                val position = LatLng(place.latitude, place.longitude)
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

    private fun createCustomMarkerIcon(text: String, occupancyPercent: Int): BitmapDescriptor {
        // Inflate custom view
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


    // --- MapView Lifecycle Methods ---

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        firestoreListener?.remove()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
