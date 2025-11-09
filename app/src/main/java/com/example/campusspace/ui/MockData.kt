package com.example.campusspace.ui

import com.example.campusspace.data.Place
import com.example.campusspace.data.PlaceType

object MockData {
    fun getPlaces(): List<Place> {
        // check below details and edit accordingly.
        // all are demo till now.
        return listOf(
            Place("1", "Library", PlaceType.LIBRARY, 250, 100, 0, "Building A, Floor 2", 30.967264, 76.473251, 20, listOf("wifi", "quiet", "computers", "printing")),
            Place("2", "S. Ramanujan Study Room", PlaceType.STUDY_ROOM, 30, 12, 0, "Computer Science Block, Floor 3", 30.967410, 76.473820, 15, listOf("wifi", "whiteboard", "projector", "air-conditioning")),
            Place("3", "S. Ramanujan Lab", PlaceType.LAB, 40, 28, 0, "Innovation Hub, Floor 1", 30.968220, 76.472910, 25, listOf("wifi", "computers", "3D printer", "specialized equipment")),
            Place("4", "Cafeteria", PlaceType.CAFE, 100, 75, 0, "Student Center", 30.967890, 76.473500, 30, listOf("wifi", "food", "indoor seating", "air-conditioned")),
            Place("5", "Bake-o-Mocha Café", PlaceType.CAFE, 45, 20, 0, "Behind Chemical Block", 30.966950, 76.474000, 10, listOf("wifi", "coffee", "food", "outdoor seating", "ambiance")),
            Place("6", "Utility Block", PlaceType.COMMON_AREA, 50, 18, 0, "Engineering Building, Basement", 30.968800, 76.472600, 25, listOf("wifi", "tools", "machines", "safety equipment")),
            Place("7", "Lawn", PlaceType.COMMON_AREA, 200, 10, 0, "Near Botanical Garden", 30.969050, 76.474250, 20, listOf("wifi", "quiet", "natural light", "outdoor seating")),
            Place("8", "Auditorium", PlaceType.COMMON_AREA, 500, 40, 0, "Building B, Floor 1", 30.967710, 76.473710, 20, listOf("wifi", "printing", "audio-visual systems", "collaboration space")),
            Place("9", "Annapurna Mess", PlaceType.MESS, 200, 35, 0, "Science Block, Floor 2", 30.968560, 76.472900, 20, listOf("wifi", "computers", "whiteboard", "quiet zone")),
            Place("10", "Lecture Hall Complex", PlaceType.STUDY_ROOM, 500, 95, 0, "Near Main Gate", 30.966700, 76.474400, 35, listOf("wifi", "food", "casual", "air-conditioning", "indoor seating")),
            Place("11", "Super Academic Block", PlaceType.STUDY_ROOM, 400, 15, 0, "Mechanical Block, Floor 1", 30.968930, 76.472310, 20, listOf("wifi", "robots", "controllers", "specialized equipment")),
            Place("12", "Central Research Facility", PlaceType.LAB, 50, 80, 0, "North Campus", 30.969400, 76.475100, 50, listOf("wifi", "outdoor seating", "green space", "events area")),
            Place("13", "VolleyBall Court", PlaceType.PLAYGROUND, 30, 5, 0, "Humanities Block, Floor 2", 30.968150, 76.474550, 10, listOf("wifi", "quiet", "lamps", "comfortable seating")),
            Place("14", "Tennis Court", PlaceType.PLAYGROUND, 10, 50, 0, "Sports Complex", 30.965900, 76.472950, 30, listOf("wifi", "equipment", "air-conditioned", "showers")),
            Place("15", "FootBall Ground", PlaceType.PLAYGROUND, 40, 90, 0, "Library Extension Wing", 30.967580, 76.473180, 20, listOf("wifi", "computers", "ebooks", "quiet")),
            Place("16", "Cricket Ground", PlaceType.PLAYGROUND, 40, 15, 0, "IT Department, Floor 2", 30.967920, 76.472830, 15, listOf("wifi", "computers", "whiteboard", "charging ports")),
            Place("17", "Ampe4re LAB", PlaceType.LAB, 40, 45, 0, "South Gate Plaza", 30.966400, 76.475000, 20, listOf("wifi", "coffee", "food", "casual", "outdoor seating")),
            Place("18", "Renewable Power LAB", PlaceType.LAB, 40, 20, 0, "Tech Workshop, Floor 1", 30.968480, 76.472100, 25, listOf("wifi", "tools", "laser cutter", "collaboration area")),
            Place("19", "Maggie Point", PlaceType.CAFE, 20, 25, 0, "Student Lounge, Floor 1", 30.967630, 76.473990, 15, listOf("wifi", "snacks", "casual", "gaming zone")),
            Place("20", "Amul", PlaceType.CAFE, 20, 8, 0, "Near East Walkway", 30.969200, 76.474900, 10, listOf("wifi", "quiet", "nature view", "outdoor seating"))
        )
    }
}