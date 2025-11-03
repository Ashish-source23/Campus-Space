package com.pgsl.campusSpace.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "GeofenceBroadcastReceiver triggered! Action=${intent.action}")

        if (intent.action != "com.pgsl.campusSpace.ACTION_GEOFENCE_EVENT") {
            Log.w("GeofenceReceiver", "Unexpected action: ${intent.action}")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent.fromIntent returned null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }
        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: "anonymous"
        for (geofence in triggeringGeofences) {
            val areaId = geofence.requestId
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> updateFirebase(areaId, userId, "IN")
                Geofence.GEOFENCE_TRANSITION_EXIT -> updateFirebase(areaId, userId, "OUT")
                else -> Log.w("GeofenceReceiver", "Unknown transition: $transitionType")
            }
        }
    }

    private fun updateFirebase(areaId: String, userId: String, status: String) {
        val db = FirebaseDatabase.getInstance().reference
        val timestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val logEntry = mapOf(
            "userId" to userId,
            "status" to status,
            "timestamp" to formattedTime
        )

        db.child("logs").child(areaId).push().setValue(logEntry)
            .addOnSuccessListener { Log.d("Firebase", "Logged $status for $userId in $areaId") }
            .addOnFailureListener { e -> Log.e("Firebase", "Failed to log event: ${e.message}") }

        val areaRef = db.child("areas").child(areaId).child("count")
        areaRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                var count = currentData.getValue(Int::class.java) ?: 0
                count = if (status == "IN") count + 1 else if (status == "OUT" && count > 0) count - 1 else 0
                currentData.value = count
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (error != null) Log.e("Firebase", "Failed to update count: ${error.message}")
                else if (committed) Log.d("Firebase", "Area $areaId count updated: ${snapshot?.value}")
            }
        })
    }
}
