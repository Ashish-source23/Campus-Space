package com.example.campusspace.data

data class Place(
    val id: String,
    val name: String,
    val type: PlaceType,
    val capacity: Int,
    val currentOccupancy: Int,
    val location: String,
    val amenities: List<String>,
    val estimatedWaitTime: Int? = null
)

enum class PlaceType(val emoji: String, val displayName: String) {
    LIBRARY("📚", "Library"),
    STUDY_ROOM("🏫", "Study Room"),
    COMMON_AREA("🏛️", "Common Area"),
    LAB("🔬", "Lab"),
    CAFE("☕", "Cafe")
}