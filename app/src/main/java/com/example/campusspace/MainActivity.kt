package com.example.campusspace // Match your package name from the error

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusspace.databinding.ActivityMainBinding // Correct import
import com.example.campusspace.ui.PlacesListFragment
import com.example.campusspace.ui.MockData
import com.example.campusspace.ui.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // This is the correct line 14

        setupOverviewCards()
        setupViewPager()
    }

    private fun setupOverviewCards() {
        val places = MockData.getPlaces()
        val totalCapacity = places.sumOf { it.capacity }
        val totalOccupancy = places.sumOf { it.currentOccupancy }
        val availableSpots = totalCapacity - totalOccupancy
        val occupancyPercentage = (totalOccupancy.toFloat() / totalCapacity * 100).toInt()

        binding.tvOccupancyPercentage.text = "$occupancyPercentage%"
        binding.tvOccupancyTotal.text = "$totalOccupancy/$totalCapacity people"
        binding.tvAvailableSpots.text = availableSpots.toString()
        binding.tvAvailableLocations.text = "Across ${places.size} locations"
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(PlacesListFragment(), "Study Locations")
        // You can add the CampusMapFragment here later
        // adapter.addFragment(CampusMapFragment(), "Campus Map")

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }
}