package com.example.campusspace.ui

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.PlaceCardItemBinding
import com.google.android.material.chip.Chip

class PlacesListAdapter(
    private val onPlaceSelected: (Place) -> Unit
) : RecyclerView.Adapter<PlacesListAdapter.PlaceViewHolder>() {

    private var places: List<Place> = emptyList()
    private var selectedPlaceId: String? = null
    private var userLocation: Location? = null

    inner class PlaceViewHolder(val binding: PlaceCardItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = PlaceCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        val isSelected = place.id == selectedPlaceId

        with(holder.binding) {
            // ... (Basic binding: Name, Icon, Location remains same)
            tvPlaceIcon.text = place.type?.emoji
            tvPlaceName.text = place.name
            tvPlaceLocation.text = place.location
            tvOccupancyRatio.text = "${place.currentOccupancy ?: 0}/${place.capacity ?: 0}"

            val capacity = place.capacity ?: 0
            val occupancyPercentage = if (capacity > 0) {
                (((place.currentOccupancy ?: 0).toFloat() / capacity) * 100).toInt()
            } else { 0 }
            progressOccupancy.progress = occupancyPercentage

            // ... (Color coding logic remains same)
            when {
                occupancyPercentage < 30 -> {
                    chipCrowdLevel.text = "Low"
                    chipCrowdLevel.setTextColor(ContextCompat.getColor(chipCrowdLevel.context, R.color.white))
                    chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(chipCrowdLevel.context, R.color.btn_green)
                }
                occupancyPercentage < 70 -> {
                    chipCrowdLevel.text = "Medium"
                    chipCrowdLevel.setTextColor(ContextCompat.getColor(chipCrowdLevel.context, R.color.black))
                    chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(chipCrowdLevel.context, R.color.btn_yellow)
                }
                else -> {
                    chipCrowdLevel.text = "High"
                    chipCrowdLevel.setTextColor(ContextCompat.getColor(chipCrowdLevel.context, R.color.white))
                    chipCrowdLevel.chipBackgroundColor = ContextCompat.getColorStateList(chipCrowdLevel.context, R.color.btn_red)
                }
            }

            // Expanded View Logic
            val isExpanded = isSelected
            expandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnExpand.setImageResource(if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)

            if (isExpanded) {
                val currentOccupancy = place.currentOccupancy ?: 0
                val placeCapacity = place.capacity ?: 0
                val available = placeCapacity - currentOccupancy
                tvAvailableSpotsDetail.text = "Available Spots: $available ($currentOccupancy / $placeCapacity)"

                // --- NEW DISTANCE & TIME CALCULATION ---
                if (userLocation != null && place.latitude != 0.0 && place.longitude != 0.0) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        userLocation!!.latitude, userLocation!!.longitude,
                        place.latitude, place.longitude,
                        results
                    )
                    val distanceMeters = results[0]

                    // 1. Format Distance
                    val distanceText = if (distanceMeters > 1000) {
                        String.format("%.1f km", distanceMeters / 1000)
                    } else {
                        "${distanceMeters.toInt()} m"
                    }

                    // 2. Calculate Time (Avg walking speed ~ 80 meters/min)
                    val walkingMinutes = (distanceMeters / 80).toInt()
                    val timeText = if (walkingMinutes < 1) "< 1 min" else "$walkingMinutes min"

                    // 3. Set Combined Text
                    tvDistanceDetail.text = "$distanceText away • ~$timeText walk"
                    tvDistanceDetail.visibility = View.VISIBLE
                } else {
                    tvDistanceDetail.text = "Distance: --"
                    tvDistanceDetail.visibility = View.VISIBLE
                }
            }

            // ... (Amenities logic remains same)
            chipGroupAmenities.removeAllViews()
            val amenities = place.amenities ?: emptyList()
            val amenitiesToShow = if (isExpanded) amenities else amenities.take(3)
            amenitiesToShow.forEach { amenity ->
                val chip = Chip(chipGroupAmenities.context).apply { text = amenity }
                chipGroupAmenities.addView(chip)
            }
            if (!isExpanded && amenities.size > 3) {
                val chip = Chip(chipGroupAmenities.context).apply { text = "+${amenities.size - 3} more" }
                chipGroupAmenities.addView(chip)
            }

            btnExpand.setOnClickListener {
                val previousSelectedPosition = places.indexOfFirst { it.id == selectedPlaceId }
                selectedPlaceId = if (isExpanded) null else place.id
                notifyItemChanged(position)
                if (previousSelectedPosition != -1) notifyItemChanged(previousSelectedPosition)
            }

            btnViewOnMap.setOnClickListener { onPlaceSelected(place) }
        }
    }

    override fun getItemCount() = places.size

    fun updateData(newPlaces: List<Place>) {
        val diffCallback = PlaceDiffCallback(this.places, newPlaces)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.places = newPlaces
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateUserLocation(location: Location?) {
        this.userLocation = location
        notifyDataSetChanged()
    }
}

// Keep the DiffCallback class at the bottom
class PlaceDiffCallback(private val oldList: List<Place>, private val newList: List<Place>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = oldList[oldItemPosition].id == newList[newItemPosition].id
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = oldList[oldItemPosition] == newList[newItemPosition]
}
