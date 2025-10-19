package com.example.campusspace.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.PlaceCardItemBinding
import com.google.android.material.chip.Chip

class PlacesListAdapter(private var places: List<Place>) : RecyclerView.Adapter<PlacesListAdapter.PlaceViewHolder>() {

    inner class PlaceViewHolder(val binding: PlaceCardItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = PlaceCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        with(holder.binding) {
            tvPlaceIcon.text = place.type.emoji
            tvPlaceName.text = place.name
            tvPlaceLocation.text = place.location
            tvOccupancyRatio.text = "${place.currentOccupancy}/${place.capacity}"

            val occupancyPercentage = (place.currentOccupancy.toFloat() / place.capacity * 100).toInt()
            progressOccupancy.progress = occupancyPercentage

            // Set crowd level chip
            when {
                occupancyPercentage < 30 -> chipCrowdLevel.text = "Low"
                occupancyPercentage < 70 -> chipCrowdLevel.text = "Medium"
                else -> chipCrowdLevel.text = "High"
            }

            // Add amenities chips
            chipGroupAmenities.removeAllViews()
            place.amenities.take(3).forEach { amenity ->
                val chip = Chip(chipGroupAmenities.context).apply {
                    text = amenity
                }
                chipGroupAmenities.addView(chip)
            }
        }
    }

    override fun getItemCount() = places.size

    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}