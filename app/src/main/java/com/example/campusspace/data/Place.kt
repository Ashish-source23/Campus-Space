package com.example.campusspace.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Place(
    val id: String? = null,
    val name: String? = null,
    val type: PlaceType? = null,
    val capacity: Int? = null,
    val currentOccupancy: Int? = null,
    val location: String? = null,
    val amenities: List<String>? = null,
    val estimatedWaitTime: Int? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Double = 0.0
)

fun Place() {
}
enum class PlaceType(val emoji: String, val displayName: String) {
    LIBRARY("📚", "Library"),
    STUDY_ROOM("🏫", "Study Room"),
    COMMON_AREA("🏛️", "Common Area"),
    LAB("🔬", "Lab"),
    CAFE("☕", "Cafe")
}