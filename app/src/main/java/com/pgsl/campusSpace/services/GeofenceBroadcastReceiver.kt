package com.pgsl.campusSpace.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.pgsl.campusSpace.utils.FirebaseRTDB
import com.pgsl.campusSpace.utils.FirebaseAuthUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }
        val transition = geofencingEvent.geofenceTransition
        val triggering = geofencingEvent.triggeringGeofences
        val firestore = FirebaseFirestore.getInstance()
        val rtdb = FirebaseRTDB.instance
        val currentUser = FirebaseAuthUtil.instance.currentUser?.uid

        if (triggering == null || triggering.isEmpty()) return

        for (gf in triggering) {
            val areaId = gf.requestId
            when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "Entered geofence: $areaId")
                    // Increment RTDB count atomically
                    incrementAreaCount(rtdb, areaId)
                    // Update user's currentArea in Firestore
                    if (currentUser != null) {
                        firestore.collection("users").document(currentUser)
                            .update("currentArea", areaId)
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "Exited geofence: $areaId")
                    decrementAreaCount(rtdb, areaId)
                    if (currentUser != null) {
                        firestore.collection("users").document(currentUser)
                            .update("currentArea", null)
                    }
                }
                else -> {
                    Log.d(TAG, "Other geofence transition: $transition for $areaId")
                }
            }
        }
    }

    private fun incrementAreaCount(rtdb: com.google.firebase.database.FirebaseDatabase, areaId: String) {
        val ref = rtdb.getReference("area_counts/$areaId")
        // Use transaction to increment atomically
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = mutableData.getValue(Int::class.java) ?: 0
                mutableData.value = current + 1
                return com.google.firebase.database.Transaction.success(mutableData)
            }
            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Failed to increment count for $areaId: ${error.message}")
                } else {
                    Log.d(TAG, "Incremented count for $areaId")
                }
            }
        })
    }

    private fun decrementAreaCount(rtdb: com.google.firebase.database.FirebaseDatabase, areaId: String) {
        val ref = rtdb.getReference("area_counts/$areaId")
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = mutableData.getValue(Int::class.java) ?: 0
                val updated = if (current <= 0) 0 else current - 1
                mutableData.value = updated
                return com.google.firebase.database.Transaction.success(mutableData)
            }
            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Failed to decrement count for $areaId: ${error.message}")
                } else {
                    Log.d(TAG, "Decremented count for $areaId")
                }
            }
        })
    }
}
