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
import com.example.campusspace.R
import com.example.campusspace.databinding.FragmentCampusMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CampusMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    // We no longer use this to restrict bounds, but it's fine to keep
    private val IIT_ROPAR_BOUNDS = LatLngBounds(
        LatLng(30.9630, 76.4680), // Approximate South-West corner
        LatLng(30.9730, 76.4800)  // Approximate North-East corner
    )

    data class Place(
        val name: String? = null,
        val capacity: Long? = null,
        val currentOccupancy: Long? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    // --- ADDED COMPANION OBJECT for const values ---
    companion object {
        // Set min/max zoom to keep the user focused on the campus.
        private const val MIN_ZOOM = 15.5f
        private const val MAX_ZOOM = 18.0f // Optional: prevent zooming in too close
    }
    // --- END OF COMPANION OBJECT ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Zoom button listeners
        binding.btnZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.btnZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Set "Earth" (hybrid) view and 3D buildings
        googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap?.isBuildingsEnabled = true

        // --- SET RESTRICTIONS AND INITIAL TILT ---

        // --- NOTE: Line removed for smooth panning ---
        // We have removed setLatLngBoundsForCameraTarget as it
        // makes panning feel "sticky". Now movement is smooth.
        // googleMap?.setLatLngBoundsForCameraTarget(IIT_ROPAR_BOUNDS)

        // 2. Set min and max zoom levels (This still works)
        googleMap?.setMinZoomPreference(MIN_ZOOM)
        googleMap?.setMaxZoomPreference(MAX_ZOOM)

        // 3. SET INITIAL TILTED CAMERA POSITION
        val campusLocation = LatLng(30.9686169, 76.473305)
        val cameraPosition = CameraPosition.Builder()
            .target(campusLocation) // Your campus location
            .zoom(17f)               // Good starting zoom
            .tilt(45f)               // Tilt the camera 45 degrees
            .build()

        // Move camera immediately to the tilted position
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        // --- END OF NEW BLOCK ---

        // --- MANUALLY HANDLE MARKER CLICK TO CENTER (WITHOUT TILT CHANGE) ---
        googleMap?.setOnMarkerClickListener { marker ->
            // Get the current camera's tilt and zoom
            val currentCameraPosition = googleMap?.cameraPosition

            // Build a new camera position that centers on the marker
            // but keeps the current tilt and zoom.
            val newCameraPosition = CameraPosition.Builder()
                .target(marker.position) // Center on the marker
                .tilt(currentCameraPosition?.tilt ?: 45f) // Keep existing tilt (default to 45)
                .zoom(currentCameraPosition?.zoom ?: 17f) // Keep existing zoom (default to 17)
                .build()

            // Animate to the new position
            googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))

            // Show the info window
            marker.showInfoWindow()

            // By returning 'true', we consume the event and prevent
            // the default camera behavior
            true
        }
        // --- END OF MODIFIED BLOCK ---

        enableMyLocation()
        fetchAndPopulateMapData()
    }

    private fun enableMyLocation() {
        // ... (no changes in this function)
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
        }
    }

    private fun fetchAndPopulateMapData() {
        // ... (no changes in this function)
        val db = Firebase.firestore
        db.collection("places")
            .get()
            .addOnSuccessListener { result ->
                val places = result.toObjects(Place::class.java)
                populateMapMarkers(places)
                highlightAreas(places)
            }
            .addOnFailureListener { exception ->
                Log.w("CampusMapFragment", "Error getting documents.", exception)
            }
    }

    @SuppressLint("SuspiciousIndiciousIndentation")
    private fun populateMapMarkers(places: List<Place>) {
        val map = googleMap ?: return
        map.clear()

        // The camera is now moved in onMapReady, so we
        // no longer need the onGlobalLayoutListener here.

        places.forEach { place ->
            if (place.latitude != null && place.longitude != null) {
                val position = LatLng(place.latitude, place.longitude)

                // --- MODIFIED FOR "65%" FORMAT on marker ---
                val capacity = place.capacity ?: 0L
                val currentOccupancy = place.currentOccupancy ?: 0L

                val occupancyPercent = if (capacity > 0L) {
                    (currentOccupancy.toFloat() / capacity.toFloat()) * 100f
                } else {
                    0f
                }

                // New string for the marker (e.g., "65%")
                val markerText = "${occupancyPercent.toInt()}%"

                // Pass this text and the percentage (for color) to the icon creator
                val icon = createCustomMarkerIcon(markerText, occupancyPercent.toInt())

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(place.name)
                        // Snippet (on click) shows full details
                        .snippet("Occupancy: $currentOccupancy / $capacity (${occupancyPercent.toInt()}%)")
                        .icon(icon)
                )
                // --- END OF MODIFICATION ---
            }
        }
    }

    private fun highlightAreas(places: List<Place>) {
        // ... (no changes in this function)
        val map = googleMap ?: return

        places.forEach { place ->
            if (place.latitude != null && place.longitude != null) {
                val position = LatLng(place.latitude, place.longitude)
                val capacity = place.capacity?.toFloat() ?: 0f
                val occupancy = if (capacity > 0f) {
                    (place.currentOccupancy?.toFloat() ?: 0f) / capacity * 100f
                } else {
                    0f
                }

                val (fillColor, strokeColor) = when {
                    occupancy < 30 -> Pair(0x3300FF00, 0x9900FF00) // 20% transparent green fill, 60% stroke
                    occupancy < 70 -> Pair(0x33FFFF00, 0x99FFFF00) // 20% transparent yellow fill, 60% stroke
                    else -> Pair(0x33FF0000, 0x99FF0000)           // 20% transparent red fill, 60% stroke
                }

                map.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(50.0)
                        .fillColor(fillColor.toInt())
                        .strokeColor(strokeColor.toInt())
                        .strokeWidth(2f)
                )
            }
        }
    }


    // --- MODIFIED FUNCTION SIGNATURE ---
    private fun createCustomMarkerIcon(text: String, occupancyPercent: Int): BitmapDescriptor {
        // --- END OF MODIFICATION ---
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_layout, null)
        val markerTextView = markerView.findViewById<TextView>(R.id.marker_text)

        val backgroundResId = when {
            // Use occupancyPercent for color
            occupancyPercent < 30 -> R.drawable.shape_circle_green
            occupancyPercent < 70 -> R.drawable.shape_circle_yellow
            else -> R.drawable.shape_circle_red
        }

        // --- MODIFIED TEXT ---
        markerTextView.text = text // Set the new text (e.g., "65%")
        // --- END OF MODIFICATION ---
        markerTextView.background = ContextCompat.getDrawable(requireContext(), backgroundResId)

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // ... (no changes to lifecycle methods: onResume, onStart, onStop, etc.) ...

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
        googleMap = null
        _binding = null
    }
}

