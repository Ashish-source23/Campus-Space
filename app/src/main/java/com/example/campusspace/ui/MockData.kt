package com.example.campusspace.ui

import com.example.campusspace.data.Place
import com.example.campusspace.data.PlaceType

object MockData {
    fun getPlaces(): List<Place> {
        // check below details and edit accordingly.
        // all are demo till now.
        return listOf(
            //Study and Research
            Place("1", "Library", PlaceType.LIBRARY, 250, 100, 0, "Building A, Floor 2", 30.967264, 76.473251, 20, listOf("wifi", "quiet", "reading", "computers", "printing" )),
            Place("2", "S. Ramanujan Study Room", PlaceType.STUDY_ROOM, 30, 12, 0, "Computer Science Block, Floor 3", 30.967410, 76.473820, 15, listOf("wifi", "whiteboard", "projector", "air-conditioning")),
            Place("3", "Lecture Hall Complex", PlaceType.STUDY_ROOM, 500, 95, 0, "Behind Mechanical Block", 30.96775, 76.47201, 70, listOf("wifi","air-conditioning", "indoor seating", "lecture halls")),
            Place("4", "Super Academic Block", PlaceType.STUDY_ROOM, 400, 15, 0, "Mechanical Block, Floor 1", 30.968930, 76.472310, 20, listOf("wifi", "robots", "controllers", "specialized equipment")),
            Place("5", "S. Ramanujan Lab", PlaceType.LAB, 40, 28, 0, "Innovation Hub, Floor 1", 30.968220, 76.472910, 25, listOf("wifi", "computers", "3D printer", "specialized equipment")),
            Place("6", "Ampere LAB", PlaceType.LAB, 40, 45, 0, "South Gate Plaza", 30.966400, 76.475000, 20, listOf("wifi", "coffee", "food", "casual", "outdoor seating")),
            Place("7", "Auditorium", PlaceType.COMMON_AREA, 500, 40, 0, "Building B, Floor 1", 30.967710, 76.473710, 20, listOf("wifi", "printing", "audio-visual systems", "collaboration space")),
            Place("8", "Central Research Facility", PlaceType.LAB, 50, 80, 0, "North Campus", 30.969400, 76.475100, 50, listOf("wifi", "outdoor seating", "green space", "events area")),
            Place("9", "Renewable Power LAB", PlaceType.LAB, 40, 20, 0, "Tech Workshop, Floor 1", 30.968480, 76.472100, 25, listOf("wifi", "tools", "laser cutter", "collaboration area")),

            //BeveragesLocations
            Place("10", "Cafeteria", PlaceType.CAFE, 100, 75, 0, "Near Workshop", 30.96623, 76.47128, 30, listOf("wifi", "food stalls", "indoor seating", "air-conditioned")),
            Place("11", "Bake-o-Mocha Café", PlaceType.CAFE, 45, 20, 0, "Behind Chemical Block", 30.966950, 76.474000, 10, listOf("wifi", "coffee", "food", "outdoor seating", "ambiance")),
            Place("12", "Amul", PlaceType.CAFE, 20, 8, 0, "Behind Annapurna Mess", 30.96672, 76.46825, 10, listOf("dairy products")),
            Place("13", "Maggie Point", PlaceType.CAFE, 20, 25, 0, "In-front of Utility", 30.96793, 76.46933, 15, listOf("casual","open area", "evening snacks")),
            Place("14", "Utility Block", PlaceType.COMMON_AREA, 50, 18, 0, "Engineering Building, Basement", 30.968800, 76.472600, 25, listOf("wifi", "tools", "machines", "safety equipment")),
            Place("15", "Annapurna Mess", PlaceType.MESS, 200, 35, 0, "", 30.96702, 76.46783, 20, listOf("wifi", "mess", "air-conditioned", "food", "vending machine")),

            //Sports and Recreation
            Place("16", "Lawn", PlaceType.COMMON_AREA, 200, 10, 0, "Near Botanical Garden", 30.969050, 76.474250, 20, listOf("wifi", "nature", "garden", "outdoor seating")),
            Place("17", "VolleyBall Court", PlaceType.PLAYGROUND, 30, 5, 0, "Sports Arena", 30.96320, 76.47223, 30, listOf("playing area")),
            Place("18", "Tennis Court", PlaceType.PLAYGROUND, 10, 50, 0, "Sports Arena", 30.96284, 76.47240, 50, listOf("playing area")),
            Place("19", "Basketball Court", PlaceType.PLAYGROUND, 50, 20, 0, "Sports Arena", 30.96323, 76.47251, 30, listOf("playing area")),
            Place("20", "FootBall Ground", PlaceType.PLAYGROUND, 100, 50, 0, "Sports Arena", 30.96280, 76.47547, 100, listOf("playing area", "jogging track")),
            Place("21", "Cricket Ground", PlaceType.PLAYGROUND, 100, 15, 0, "Sports Arena", 30.96088, 76.47550, 150, listOf("playing area")),
        )
    }
}