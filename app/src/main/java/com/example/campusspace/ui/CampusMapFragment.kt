package com.example.campusspace.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.campusspace.R
import com.example.campusspace.databinding.FragmentCampusMapBinding

class CampusMapFragment : Fragment() {

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateMapMarkers()

        // TODO: Add logic for zoom buttons
        // binding.btnZoomIn.setOnClickListener { ... }
    }

    private fun populateMapMarkers() {
        val places = MockData.getPlaces()
        val mapContainer = binding.mapContainer
        val density = requireContext().resources.displayMetrics.density

        places.forEachIndexed { index, place ->
            // 1. Calculate occupancy percentage and get the correct color
            val occupancy = (place.currentOccupancy.toFloat() / place.capacity.toFloat()) * 100f
            val backgroundResId = when {
                occupancy < 30 -> R.drawable.shape_circle_green
                occupancy < 70 -> R.drawable.shape_circle_yellow
                else -> R.drawable.shape_circle_red
            }

            // 2. Calculate position, converting Figma's percentages to ConstraintLayout bias (0.0 to 1.0)
            val xBias = (20 + (index % 3) * 30) / 100.0f
            val yBias = (25 + (index / 3) * 25) / 100.0f

            // 3. Convert 24dp (from Figma's w-6 h-6) to pixels
            val sizeDp = 24
            val sizePx = (sizeDp * density).toInt()

            // 4. Create the new marker (which is a TextView)
            val marker = TextView(requireContext()).apply {
                id = View.generateViewId() // Important for ConstraintLayout
                text = "${occupancy.toInt()}"
                setTextColor(Color.WHITE)
                textSize = 10f // Corresponds to text-xs
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(requireContext(), backgroundResId)

                // 5. Set its size and position on the map
                val params = ConstraintLayout.LayoutParams(sizePx, sizePx)
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

                params.horizontalBias = xBias
                params.verticalBias = yBias

                layoutParams = params
            }

            // 6. Add the newly created marker to the map container
            mapContainer.addView(marker)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}