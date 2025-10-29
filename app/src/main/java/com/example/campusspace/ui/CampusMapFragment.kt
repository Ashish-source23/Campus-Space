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
    data class Place(
        val name: String? = null,
        val capacity: Long? = null,
        val currentOccupancy: Long? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        fetchAndPopulateMapData()
    }

    private fun enableMyLocation() {
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

    @SuppressLint("SuspiciousIndentation")
    private fun populateMapMarkers(places: List<Place>) {
        val map = googleMap ?: return
        map.clear()

        val campusLocation = LatLng(30.9686169, 76.473305)
            mapView.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocation, 15f))
            }
        })

        places.forEach { place ->
            if (place.latitude != null && place.longitude != null) {
                val position = LatLng(place.latitude, place.longitude)
                val capacity = place.capacity?.toFloat() ?: 0f
                val occupancy = if (capacity > 0f) {
                    (place.currentOccupancy?.toFloat() ?: 0f) / capacity * 100f
                } else {
                    0f
                }
                val icon = createCustomMarkerIcon(occupancy.toInt())

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(place.name)
                        .snippet("Occupancy: ${occupancy.toInt()}%")
                        .icon(icon)
                )
            }
        }
    }

    private fun highlightAreas(places: List<Place>) {
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


    private fun createCustomMarkerIcon(occupancy: Int): BitmapDescriptor {
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_layout, null)
        val markerTextView = markerView.findViewById<TextView>(R.id.marker_text)

        val backgroundResId = when {
            occupancy < 30 -> R.drawable.shape_circle_green
            occupancy < 70 -> R.drawable.shape_circle_yellow
            else -> R.drawable.shape_circle_red
        }

        markerTextView.text = occupancy.toString()
        markerTextView.background = ContextCompat.getDrawable(requireContext(), backgroundResId)

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

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
