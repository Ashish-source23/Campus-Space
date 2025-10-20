package com.pgsl.campusSpace.utils

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseRTDB {

    val instance: FirebaseDatabase by lazy {
        Firebase.database
    }
}
