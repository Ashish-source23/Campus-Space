package com.example.campusspace.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.campusspace.R
import com.example.campusspace.data.Place
import com.example.campusspace.databinding.PlaceCardItemBinding
import com.google.android.material.chip.Chip

// --- FIX: Constructor now only accepts the click listener ---
class PlacesListAdapter(
    private val onPlaceSelected: (Place) -> Unit // Click listener for "View on Map"
) : RecyclerView.Adapter<PlacesListAdapter.PlaceViewHolder>() {

    // --- FIX: The list is now a mutable property, initialized as empty ---
    private var places: List<Place> = emptyList()
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
            tvPlaceIcon.text = place.type?.emoji
            tvPlaceName.text = place.name
            tvPlaceLocation.text = place.location
            tvOccupancyRatio.text = "${place.currentOccupancy ?: 0}/${place.capacity ?: 0}"

            val capacity = place.capacity ?: 0
            val occupancyPercentage = if (capacity > 0) {
                (((place.currentOccupancy ?: 0).toFloat() / capacity) * 100).toInt()
            } else {
                0
            }
            progressOccupancy.progress = occupancyPercentage

            when {
                occupancyPercentage < 30 -> chipCrowdLevel.text = "Low"
                occupancyPercentage < 70 -> chipCrowdLevel.text = "Medium"
                else -> chipCrowdLevel.text = "High"
            }

            val isExpanded = isSelected
            expandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnExpand.setImageResource(if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)

            if (isExpanded) {
                val currentOccupancy = place.currentOccupancy ?: 0
                val placeCapacity = place.capacity ?: 0
                val available = placeCapacity - currentOccupancy
                tvAvailableSpotsDetail.text = "Available Spots: $available ($currentOccupancy / $placeCapacity)"

                val waitTime = place.estimatedWaitTime ?: 0
                tvWaitTimeDetail.text = if (waitTime > 0) "Est. Wait: $waitTime min" else "Est. Wait: No wait expected"
            }

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
                if (previousSelectedPosition != -1) {
                    notifyItemChanged(previousSelectedPosition)
                }
            }

            btnViewOnMap.setOnClickListener {
                onPlaceSelected(place)
            }
        }
    }

    override fun getItemCount() = places.size

    fun updateData(newPlaces: List<Place>) {
        val diffCallback = PlaceDiffCallback(this.places, newPlaces)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.places = newPlaces
        diffResult.dispatchUpdatesTo(this)
    }
}

class PlaceDiffCallback(
    private val oldList: List<Place>,
    private val newList: List<Place>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
