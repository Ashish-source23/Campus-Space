package com.example.campusspace.ui

import android.Manifest // <-- ADD IMPORT
import android.content.pm.PackageManager // <-- ADD IMPORT
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // <-- ADD IMPORT
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusspace.MainActivity
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.FragmentPlacesListBinding
import com.example.campusspace.utils.FirebaseDB
import com.google.android.gms.location.FusedLocationProviderClient // <-- ADD IMPORT
import com.google.android.gms.location.LocationServices // <-- ADD IMPORT
import com.google.firebase.firestore.ListenerRegistration

class PlacesListFragment : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var placesListAdapter: PlacesListAdapter
    private var firestoreListener: ListenerRegistration? = null
    private var allPlaces: List<Place> = emptyList()

    // --- 1. ADD THIS VARIABLE ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 2. INITIALIZE LOCATION CLIENT ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupSearchListener()

        // --- 3. FETCH LOCATION AND UPDATE ADAPTER ---
        fetchLocationForList()
    }

    // --- 4. ADD THIS FUNCTION ---
    private fun fetchLocationForList() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Send the location to the adapter to calculate distances
                    placesListAdapter.updateUserLocation(location)
                }
            }
        }
    }

    //Function to set up search bar logic
    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on the new text
                filterList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })
    }

    private fun filterList(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            // If query is empty, show the full list
            allPlaces
        } else {
            // Otherwise, filter the list
            val lowerCaseQuery = query.lowercase().trim()
            allPlaces.filter { place ->
                // Check if the place name (case-insensitive) contains the query
                place.name?.lowercase()?.contains(lowerCaseQuery) == true
            }
        }
        // Submit the new filtered list to the adapter
        placesListAdapter.updateData(filteredList)
    }

    private fun setupRecyclerView() {
        placesListAdapter = PlacesListAdapter { selectedPlace ->
            // 1. Tell the ViewModel which place was selected
            sharedViewModel.selectPlace(selectedPlace)

            // 2. Tell MainActivity to switch to the map tab
            (activity as? MainActivity)?.switchToMapTab()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = placesListAdapter
        }
    }


    override fun onStart() {
        super.onStart()
        // Start listening for real-time data changes from Firestore
        firestoreListener = FirebaseDB.instance.collection("places")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("PlacesListFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    // Convert the Firestore documents to our master list
                    allPlaces = querySnapshot.toObjects(Place::class.java)

                    // Re-apply the current filter to the new data
                    filterList(binding.etSearch.text.toString())
                } else {
                    Log.d("PlacesListFragment", "Current data: null")
                    allPlaces = emptyList()
                    placesListAdapter.updateData(allPlaces)
                }
            }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening for changes to prevent memory leaks and save resources
        firestoreListener?.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding reference to avoid memory leaks
        _binding = null
    }
}