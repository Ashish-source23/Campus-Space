package com.example.campusspace.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusspace.MainActivity
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.FragmentPlacesListBinding
import com.example.campusspace.utils.FirebaseDB
import com.google.android.gms.location.* // Import all location services
import com.google.firebase.firestore.ListenerRegistration

class PlacesListFragment : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var placesListAdapter: PlacesListAdapter
    private var firestoreListener: ListenerRegistration? = null
    private var allPlaces: List<Place> = emptyList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // --- 1. ADD THESE VARIABLES ---
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupSearchListener()

        // --- 2. INITIALIZE LOCATION UPDATES LOGIC ---
        setupLocationUpdates()
    }

    // --- 3. DEFINE THE LOCATION REQUEST LOGIC ---
    private fun setupLocationUpdates() {
        // Create a request for high accuracy updates every 5 seconds
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update every 5 seconds
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Define what happens when a new location is found
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Pass the new live location to the adapter
                    placesListAdapter.updateUserLocation(location)
                }
            }
        }
    }

    // --- 4. START UPDATES WHEN FRAGMENT IS VISIBLE ---
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    // --- 5. STOP UPDATES WHEN FRAGMENT IS HIDDEN (SAVE BATTERY) ---
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            // Request continuous updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        // Stop updates to save battery when user isn't looking at the list
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allPlaces
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            allPlaces.filter { place ->
                place.name?.lowercase()?.contains(lowerCaseQuery) == true
            }
        }

        // --- PRESERVE EXPANDED STATE (OPTIONAL BUT RECOMMENDED) ---
        // If Firestore updates, we don't want expanded cards to suddenly close.
        // We copy the 'isExpanded' state from the old list to the new list.
        val currentList = placesListAdapter.currentList
        filteredList.forEach { newPlace ->
            val oldPlace = currentList.find { it.id == newPlace.id }
            if (oldPlace != null) {
                newPlace.isExpanded = oldPlace.isExpanded
            }
        }

        placesListAdapter.submitList(filteredList)
    }

    private fun setupRecyclerView() {
        placesListAdapter = PlacesListAdapter { selectedPlace ->
            sharedViewModel.selectPlace(selectedPlace)
            (activity as? MainActivity)?.switchToMapTab()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = placesListAdapter
            // This prevents the list from "blinking" too much on updates
            itemAnimator = null
        }
    }

    override fun onStart() {
        super.onStart()
        firestoreListener = FirebaseDB.instance.collection("places")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("PlacesListFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    allPlaces = querySnapshot.toObjects(Place::class.java)
                    filterList(binding.etSearch.text.toString())
                } else {
                    allPlaces = emptyList()
                    placesListAdapter.submitList(allPlaces)
                }
            }
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}