package com.example.campusspace.ui

import com.example.campusspace.data.Place
import com.example.campusspace.data.PlaceType

object MockData {
    fun getPlaces(): List<Place> {
        return listOf(
            //Study and Research
            Place("1", "Library", PlaceType.LIBRARY, 250, 100, 0, "Building A, Floor 2", 30.967264, 76.473251, 20, listOf("wifi", "quiet", "reading", "computers", "printing" )),
            Place("2", "CS LAB 1", PlaceType.STUDY_ROOM, 45, 10, 0, "S. Ramanujan Block", 30.969029078079142, 76.47553299356396, 5, listOf("wifi", "whiteboard", "projector", "air-conditioning", "computers")),
            Place("3", "CS LAB 2", PlaceType.STUDY_ROOM, 45, 10, 0, "S. Ramanujan Block", 30.969032527851393, 76.47563759971143, 5, listOf("wifi", "whiteboard", "projector", "air-conditioning", "computers")),
            Place("4", "CS 1", PlaceType.STUDY_ROOM, 40, 10, 0, "S. Ramanujan Block", 30.968937659068235, 76.476025849462, 7, listOf("wifi", "whiteboard", "projector", "air-conditioning", "chairs", "tables")),
            Place("5", "Lecture Hall Complex", PlaceType.STUDY_ROOM, 500, 95, 0, "Behind Mechanical Block", 30.96775, 76.47201, 30, listOf("wifi","air-conditioning", "indoor seating", "lecture halls")),
            Place("6", "Super Academic Block", PlaceType.STUDY_ROOM, 400, 15, 0, "Besides Auditorium", 30.96738, 76.47465, 60, listOf("wifi", "labs", "controllers", "specialized equipment" , "Fountain")),
            Place("7", "Ampe4re LAB", PlaceType.LAB, 40, 15, 0, "J.C.Bose Block", 30.96876, 76.47457, 20, listOf("wifi", "coffee", "food", "casual", "outdoor seating")),
            Place("8", "Auditorium", PlaceType.COMMON_AREA, 600, 40, 0, "Near Library", 30.96771, 76.47329, 20, listOf("wifi","audio-visual systems", "collaboration space")),
            Place("9", "Central Research Facility", PlaceType.LAB, 50, 20, 0, "Near SAB", 30.96685, 76.47583, 15, listOf("wifi", "research", "labs", "equipments")),
            Place("10", "Renewable Power LAB", PlaceType.LAB, 40, 20, 0, "Tech Workshop, Floor 1", 30.968480, 76.472100, 25, listOf("wifi", "tools", "laser cutter", "collaboration area")),

            //Beverages Location
            Place("11", "Cafeteria", PlaceType.CAFE, 200, 75, 0, "Near Workshop", 30.96616, 76.47138, 25, listOf("wifi", "food stalls", "indoor seating", "air-conditioned")),
            Place("12", "Bake-o-Mocha CafÃ©", PlaceType.CAFE, 30, 20, 0, "Behind Chemical Block", 30.96820, 76.47085, 6, listOf("wifi", "coffee", "food", "outdoor seating", "ambiance")),
            Place("13", "Amul", PlaceType.CAFE, 20, 8, 0, "Behind Annapurna Mess", 30.96672, 76.46825, 10, listOf("dairy products")),
            Place("14", "Maggie Point", PlaceType.CAFE, 20, 5, 0, "In-front of Utility", 30.96793, 76.46933, 10, listOf("casual","open area", "evening snacks")),
            Place("15", "Utility Block", PlaceType.COMMON_AREA, 50, 18, 0, "In-front of Ravi Hostel", 30.96774, 76.47001, 8, listOf("wifi", "stationary", "printing", "saloon", "daily essentials")),
            Place("16", "Annapurna Mess", PlaceType.MESS, 200, 35, 0, "Near Bramhaputra Hostel", 30.96687, 76.46791, 30, listOf("wifi", "mess", "air-conditioned", "food", "vending machine")),

            //Sports and Recreation
            Place("17", "Lawn", PlaceType.COMMON_AREA, 200, 10, 0, "Near Ravi Hostel", 30.96756, 76.47091, 35, listOf("nature", "garden", "outdoor seating")),
            Place("18", "VolleyBall Court", PlaceType.PLAYGROUND, 30, 5, 0, "Sports Arena", 30.96320, 76.47223, 13, listOf("playing area")),
            Place("19", "Lawn Tennis Court", PlaceType.PLAYGROUND, 10, 2, 0, "Sports Arena", 30.96291, 76.47240, 16, listOf("playing area")),
            Place("20", "Basketball Court", PlaceType.PLAYGROUND, 50, 20, 0, "Sports Arena", 30.96323, 76.47251, 13, listOf("playing area")),
            Place("21", "FootBall Ground", PlaceType.PLAYGROUND, 100, 50, 0, "Sports Arena", 30.96280, 76.47547, 50, listOf("playing area", "jogging track")),
            Place("22", "Cricket Ground", PlaceType.PLAYGROUND, 100, 15, 0, "Sports Arena", 30.96088, 76.47550, 65, listOf("playing area")),
            Place("23", "Hokey Ground", PlaceType.PLAYGROUND, 100, 15, 0, "Sports Arena", 30.96130, 76.47392, 35, listOf("playing area")),
        )
    }
}