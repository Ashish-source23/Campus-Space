package com.example.campusspace.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.PlaceCardItemBinding
import com.google.android.material.chip.Chip

class PlacesListAdapter(
    private var places: List<Place>,
    private val onPlaceSelected: (Place) -> Unit // Click listener for "View on Map"
) : RecyclerView.Adapter<PlacesListAdapter.PlaceViewHolder>() {

    private var selectedPlaceId: String? = null

    inner class PlaceViewHolder(val binding: PlaceCardItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = PlaceCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        val isSelected = place.id == selectedPlaceId

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

            // --- EXPAND LOGIC ---
            if (isSelected) {
                expandedView.visibility = View.VISIBLE
                btnExpand.setImageResource(R.drawable.ic_chevron_up)

                // Populate expanded details
                val available = place.capacity - place.currentOccupancy
                tvAvailableSpotsDetail.text = "Available Spots: $available (${place.currentOccupancy} / ${place.capacity})"

                val waitTime = place.estimatedWaitTime ?: 0
                tvWaitTimeDetail.text = if (waitTime > 0) {
                    "Est. Wait: $waitTime min"
                } else {
                    "Est. Wait: No wait expected"
                }

            } else {
                expandedView.visibility = View.GONE
                btnExpand.setImageResource(R.drawable.ic_chevron_down)
            }

            // Show all amenities if selected, otherwise just 3
            chipGroupAmenities.removeAllViews()
            val amenitiesToShow = if (isSelected) place.amenities else place.amenities.take(3)
            amenitiesToShow.forEach { amenity ->
                val chip = Chip(chipGroupAmenities.context).apply {
                    text = amenity
                }
                chipGroupAmenities.addView(chip)
            }
            if (!isSelected && place.amenities.size > 3) {
                val chip = Chip(chipGroupAmenities.context).apply {
                    text = "+${place.amenities.size - 3} more"
                }
                chipGroupAmenities.addView(chip)
            }


            // --- CLICK LISTENERS ---
            btnExpand.setOnClickListener {
                selectedPlaceId = if (isSelected) null else place.id
                // This is simple but inefficient. For a short list, it's fine.
                // For a long list, you'd use notifyItemChanged.
                notifyDataSetChanged()
            }

            btnViewOnMap.setOnClickListener {
                onPlaceSelected(place) // Pass the click event to the fragment
            }
        }
    }

    override fun getItemCount() = places.size

    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}