package com.pgsl.campusSpace.entity

data class GeofenceArea(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMetres: Double = 0.0
)