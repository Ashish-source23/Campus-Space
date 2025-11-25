package com.pgsl.campusSpace.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirebaseDB {

    val instance: FirebaseFirestore by lazy {
        Firebase.firestore
    }
}