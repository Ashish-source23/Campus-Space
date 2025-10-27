package com.example.campusspace.services

import android.annotation.SuppressLint
import android.util.Log
import com.example.campusspace.entity.GeofenceArea
import com.example.campusspace.entity.User
import com.example.campusspace.utils.FirebaseAuthUtil
import com.example.campusspace.utils.FirebaseDB
import com.example.campusspace.utils.FirebaseRTDB
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toArea(): GeofenceArea? {
    return this.toObject(GeofenceArea::class.java)?.copy(id = this.id)
}
object FirebaseService {

    private const val TAG = "FirebaseService"
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseDB.instance
    private val rtdb = FirebaseRTDB.instance
    private val auth = FirebaseAuthUtil.instance

    private var connectionListener: ValueEventListener? = null

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun createUserProfile(user: User) {
        try {
            firestore.collection("users").document(user.uid).set(user).await()
            Log.d(TAG, "Successfully created user profile for ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile for ${user.uid}", e)
        }
    }

    fun getUserProfileStream(): Flow<User?> {
        val userId = currentUserId
        if (userId == null) {
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
                    trySend(null)
                }
            }
            awaitClose { listener.remove() }
        }
    }

    fun connectPresence() {
        val userId = currentUserId ?: return
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

    suspend fun getAreas(): List<GeofenceArea> {
        return try {
            val snapshot = firestore.collection("areas").get().await()
            snapshot.documents.mapNotNull { it.toArea() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching areas", e)
            emptyList()
        }
    }

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



