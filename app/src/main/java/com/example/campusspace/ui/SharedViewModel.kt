package com.example.campusspace.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.campusspace.data.Place // <-- Uses your real data class

class SharedViewModel : ViewModel() {

    private val _selectedPlace = MutableLiveData<Place?>()
    val selectedPlace: LiveData<Place?> = _selectedPlace

    /**
     * Called by PlacesListFragment when "View on Map" is clicked.
     */
    fun selectPlace(place: Place) {
        _selectedPlace.value = place
    }

    /**
     * Called by CampusMapFragment after it has focused on the place.
     */
    fun clearSelectedPlace() {
        _selectedPlace.value = null
    }
}