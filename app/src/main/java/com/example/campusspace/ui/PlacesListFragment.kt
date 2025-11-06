package com.example.campusspace.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusspace.MainActivity
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.FragmentPlacesListBinding
import com.example.campusspace.utils.FirebaseDB
import com.google.firebase.firestore.ListenerRegistration

class PlacesListFragment : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var placesListAdapter: PlacesListAdapter
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        placesListAdapter = PlacesListAdapter { selectedPlace ->
            // --- MODIFIED BLOCK ---
            // 1. Tell the ViewModel which place was selected
            sharedViewModel.selectPlace(selectedPlace)

            // 2. Tell MainActivity to switch to the map tab
            (activity as? MainActivity)?.switchToMapTab()
            // --- END OF MODIFICATION ---
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
                    // Convert the Firestore documents to a list of Place objects
                    val places = querySnapshot.toObjects(Place::class.java)
                    // Update the adapter with the new list using DiffUtil
                    placesListAdapter.updateData(places)
                } else {
                    Log.d("PlacesListFragment", "Current data: null")
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
