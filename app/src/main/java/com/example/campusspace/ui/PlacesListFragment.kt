package com.example.campusspace.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusspace.MainActivity // Import MainActivity
import com.example.campusspace.data.Place // Import Place
import com.example.campusspace.databinding.FragmentPlacesListBinding

class PlacesListFragment : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!
    private lateinit var placesAdapter: PlacesListAdapter

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
        setupSearch()
    }

    private fun setupRecyclerView() {
        // Pass the click handler to the adapter
        placesAdapter = PlacesListAdapter(MockData.getPlaces()) { place ->
            onPlaceCardSelected(place)
        }
        binding.rvPlaces.apply {
            adapter = placesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // This function is called when "View on Map" is clicked
    private fun onPlaceCardSelected(place: Place) {
        // Tell the MainActivity to switch tabs
        (activity as? MainActivity)?.switchToMapTab()

        // In a more advanced app, you'd pass the place.id to the map
        // using a Shared ViewModel so the map can highlight it.
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val allPlaces = MockData.getPlaces()
            val filteredList = if (query.isEmpty()) {
                allPlaces
            } else {
                allPlaces.filter {
                    it.name.lowercase().contains(query) || it.location.lowercase().contains(query)
                }
            }
            placesAdapter.updateData(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}