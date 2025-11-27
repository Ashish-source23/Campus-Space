package com.example.campusspace.ui

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.PlaceCardItemBinding
import com.google.android.material.chip.Chip

class PlacesListAdapter(
    private val onPlaceSelected: (Place) -> Unit
) : ListAdapter<Place, PlacesListAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    private var userLocation: Location? = null

    // ViewHolder holds the binding and sets up initial listeners.
    inner class PlaceViewHolder(val binding: PlaceCardItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Set the main click listener on the entire card.
            binding.rootCardView.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val place = getItem(position)
                    place.isExpanded = !place.isExpanded

                    // Notify the adapter that this specific item has changed.
                    notifyItemChanged(position)
                }
            }

            // Set the listener for the "View on Map" button inside the expanded view.
            binding.btnViewOnMap.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaceSelected(getItem(position))
                }
            }
        }

        fun bind(place: Place) {
            with(binding) {
                // --- Basic Info ---
                tvPlaceIcon.text = place.type?.emoji
                tvPlaceName.text = place.name
                tvPlaceLocation.text = place.location
                tvOccupancyRatio.text = "${place.currentOccupancy ?: 0}/${place.capacity ?: 0}"

                // --- Progress and Crowd Level Chip ---
                val capacity = place.capacity ?: 0
                val occupancyPercentage = if (capacity > 0) {
                    (((place.currentOccupancy ?: 0).toFloat() / capacity) * 100).toInt()
                } else {
                    0
                }
                progressOccupancy.progress = occupancyPercentage

                when {
                    occupancyPercentage < 30 -> {
                        chipCrowdLevel.text = "Low"
                        chipCrowdLevel.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                        chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(root.context, R.color.btn_green)
                    }
                    occupancyPercentage < 70 -> {
                        chipCrowdLevel.text = "Medium"
                        chipCrowdLevel.setTextColor(ContextCompat.getColor(root.context, R.color.black))
                        chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(root.context, R.color.btn_yellow)
                    }
                    else -> {
                        chipCrowdLevel.text = "High"
                        chipCrowdLevel.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                        chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(root.context, R.color.btn_red)
                    }
                }

                // --- Expansion State Logic ---
                expandedView.visibility = if (place.isExpanded) View.VISIBLE else View.GONE

                if (place.isExpanded) {
                    // --- Populate Expanded View Details ---
                    val currentOccupancy = place.currentOccupancy ?: 0
                    val available = (place.capacity ?: 0) - currentOccupancy
                    tvAvailableSpotsDetail.text = "Available Spots: $available ($currentOccupancy / ${place.capacity ?: 0})"

                    // --- Distance & Time Calculation ---
                    if (userLocation != null && place.latitude != 0.0 && place.longitude != 0.0) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            userLocation!!.latitude, userLocation!!.longitude,
                            place.latitude, place.longitude,
                            results
                        )
                        val distanceMeters = results[0]
                        val distanceText = if (distanceMeters > 1000) String.format("%.1f km", distanceMeters / 1000) else "${distanceMeters.toInt()} m"
                        val walkingMinutes = (distanceMeters / 80).toInt()
                        val timeText = if (walkingMinutes < 1) "< 1 min" else "$walkingMinutes min"
                        tvDistanceDetail.text = "$distanceText away • ~$timeText walk"
                        tvDistanceDetail.visibility = View.VISIBLE
                    } else {
                        tvDistanceDetail.visibility = View.GONE
                    }
                }

                // --- Amenities Chip Group Logic ---
                chipGroupAmenities.removeAllViews()
                val amenities = place.amenities ?: emptyList()
                val amenitiesToShow = if (place.isExpanded) amenities else amenities.take(3)
                amenitiesToShow.forEach { amenity ->
                    val chip = Chip(root.context).apply { text = amenity }
                    chipGroupAmenities.addView(chip)
                }
                if (!place.isExpanded && amenities.size > 3) {
                    val moreChip = Chip(root.context).apply { text = "+${amenities.size - 3} more" }
                    chipGroupAmenities.addView(moreChip)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = PlaceCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Call this from your fragment to update the user's location
    fun updateUserLocation(location: Location?) {
        if (this.userLocation != location) {
            this.userLocation = location
            // Redraw all visible items to update distances
            notifyDataSetChanged()
        }
    }
}

// DiffUtil helps ListAdapter animate and efficiently update the list.
class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
    override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
        return oldItem == newItem
    }
}
