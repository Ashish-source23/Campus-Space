package com.pgsl.campusSpace.services

import android.annotation.SuppressLint
import android.util.Log
// Removed "androidx.compose.animation.core.copy" as it's not used and likely an auto-import error.
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener // Kept the correct, single import
import com.google.firebase.firestore.DocumentSnapshot
import com.pgsl.campusSpace.entity.GeofenceArea
import com.pgsl.campusSpace.entity.User
// Removed "com.google.firebase.firestore.FieldValue" as it is not used in this file.
import com.pgsl.campusSpace.utils.FirebaseAuthUtil
import com.pgsl.campusSpace.utils.FirebaseDB
import com.pgsl.campusSpace.utils.FirebaseRTDB
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toArea(): GeofenceArea? {
    // The 'this' keyword refers to the DocumentSnapshot instance.
    return this.toObject(GeofenceArea::class.java)?.copy(id = this.id)
}

/**
 * A singleton service object to handle all Firebase communications.
 * The frontend developer will call these functions from their ViewModels or Services.
 */
object FirebaseService {

    private const val TAG = "FirebaseService"
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseDB.instance
    private val rtdb = FirebaseRTDB.instance
    private val auth = FirebaseAuthUtil.instance

    private var connectionListener: ValueEventListener? = null

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // --- NEW USER PROFILE FUNCTIONS ---

    /**
     * Creates a user profile document in Firestore. Typically called once upon registration.
     * @param user The User object containing profile information.
     */
    suspend fun createUserProfile(user: User) {
        try {
            firestore.collection("users").document(user.uid).set(user).await()
            Log.d(TAG, "Successfully created user profile for ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile for ${user.uid}", e)
        }
    }

    /**
     * Provides a real-time stream of the current user's profile from Firestore.
     * @return A Flow that emits the User object, or null if not found or logged out.
     */
    fun getUserProfileStream(): Flow<User?> {
        val userId = currentUserId
        if (userId == null) {
            // If user is not logged in, return a flow that immediately emits null
            return callbackFlow { trySend(null); close() }
        }

        val userDocRef = firestore.collection("users").document(userId)

        return callbackFlow {
            val listener = userDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "User profile listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null) // Document doesn't exist
                }
            }
            awaitClose { listener.remove() }
        }
    }


    /**
     * Monitors the user's overall connection to Firebase Realtime Database.
     * This should be called once when the user is authenticated. It helps Firebase
     * more reliably trigger onDisconnect() events.
     */
    fun connectPresence() {
        val userId = currentUserId ?: return // Don't run if user is not logged in

        val connectedRef = rtdb.getReference(".info/connected")
        val userStatusRef = rtdb.getReference("users/$userId/status")

        if (connectionListener != null) {
            return
        }

        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userStatusRef.setValue("online")
                    userStatusRef.onDisconnect().setValue("offline")
                    Log.d(TAG, "User presence connected.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Presence connection listener was cancelled.", error.toException())
            }
        }
        connectedRef.addValueEventListener(connectionListener!!)
    }

    /**
     * Removes the connection listener. Call this when the user logs out.
     */
    fun disconnectPresence() {
        connectionListener?.let {
            rtdb.getReference(".info/connected").removeEventListener(it)
            connectionListener = null
            Log.d(TAG, "User presence disconnected.")
        }
        currentUserId?.let { userId ->
            rtdb.getReference("users/$userId/status").setValue("offline")
        }
    }


    /**
     * Fetches the list of all geofence areas from Firestore.
     * This is a one-time read.
     *
     * @return A list of GeofenceArea objects or an empty list if an error occurs.
     */
    suspend fun getAreas(): List<GeofenceArea> {
        return try {
            val snapshot = firestore.collection("areas").get().await()
            snapshot.documents.mapNotNull { it.toArea() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching areas", e)
            emptyList()
        }
    }

    /**
     * Updates the user's presence in a specific area in the Realtime Database.
     * Handles both entering and exiting an area.
     *
     * @param areaId The ID of the area (e.g., "park_A").
     * @param isEntering True if the user is entering, false if exiting.
     */
    suspend fun updateAreaPresence(areaId: String, isEntering: Boolean) {
        val userId = currentUserId ?: run {
            Log.w(TAG, "User not logged in, cannot update presence.")
            return
        }

        val presenceRef = rtdb.getReference("area_presence/$areaId/$userId")

        try {
            if (isEntering) {
                presenceRef.setValue(true).await()
                presenceRef.onDisconnect().removeValue()
                Log.d(TAG, "User $userId ENTERED $areaId")
            } else {
                presenceRef.removeValue().await()
                Log.d(TAG, "User $userId EXITED $areaId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update presence for $areaId", e)
        }
    }

    /**
     * Provides a real-time stream of the user count for a specific area.
     * The frontend can "collect" this stream to get live updates.
     *
     * @param areaId The ID of the area to monitor.
     * @return A Flow that emits the user count (Int) whenever it changes.
     */
    fun getAreaCountStream(areaId: String): Flow<Int> {
        val countRef = rtdb.getReference("area_counts/$areaId")

        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.getValue(Int::class.java) ?: 0
                    trySend(count)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Area count listener cancelled for $areaId", error.toException())
                    close(error.toException())
                }
            }
            countRef.addValueEventListener(listener)
            awaitClose {
                countRef.removeEventListener(listener)
            }
        }
    }

    fun getAreasStream(): Flow<List<GeofenceArea>> = callbackFlow {
        val listener = firestore.collection("areas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val areas = snapshot.documents.mapNotNull { it.toArea() }
                    trySend(areas)
                }
            }
        awaitClose { listener.remove() }
    }
}



