package com.example.campusspace.utils

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

object FirebaseAuthUtil {

    val instance: FirebaseAuth by lazy {
        Firebase.auth
    }
}
