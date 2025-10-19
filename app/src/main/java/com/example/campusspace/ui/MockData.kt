package com.example.campusspace.ui

import com.example.campusspace.data.Place
import com.example.campusspace.data.PlaceType

object MockData {
    fun getPlaces(): List<Place> {
        return listOf(
            Place("1", "Central Library", PlaceType.LIBRARY, 200, 45, "Building A, Floor 2", listOf("wifi", "quiet", "computers", "printing"), 0),
            Place("2", "S.Ramanujan Block Lab", PlaceType.STUDY_ROOM, 24, 18, "Computer Science Dept, Floor 3", listOf("wifi", "whiteboard", "projector", "Computer Systems"), 15),
            Place("3", "Utility", PlaceType.COMMON_AREA, 150, 95, "Student Center", listOf("wifi", "food", "collaborative"), 5),
            Place("4", "Engineering Lab", PlaceType.LAB, 30, 22, "Engineering Building", listOf("wifi", "computers", "specialized equipment"), 10),
            Place("5", "Bake-o-Mocha", PlaceType.CAFE, 40, 12, "Behind Chemical Block", listOf("ambiance", "food", "casual", "outdoor seating"), 0),
            Place("6", "Cafeteria", PlaceType.COMMON_AREA, 80, 65, "Above Ideal Mess", listOf("wifi", "food", "casual", "air-condition", "indoor seating"), 20)
        )
    }
}