package com.pgsl.campusSpace

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.pgsl.campusSpace.utils.FirebaseAuthUtil
import com.pgsl.campusSpace.utils.FirebaseDB
import com.pgsl.campusSpace.utils.FirebaseRTDB

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuthUtil.instance
    private val db = FirebaseDB.instance
    private val realTimeDb = FirebaseRTDB.instance

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permissions", "Location permission granted.")
                startLocationUpdates()
            } else {
                Log.d("Permissions", "Location permission denied.")
            }
    }
    private fun startLocationUpdates() {
//        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
//            .setWaitForAccurateLocation(true)
//            .setMinUpdateIntervalMillis(5000)
//            .build()
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Log.e("LocationUpdates", "Permission check failed before starting updates.")
//            return
//        }
//
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
//        Toast.makeText(this, "Location tracking started!", Toast.LENGTH_SHORT).show()
    }
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchUserData(currentUser.uid)
        } else {
            Log.d("MainActivity", "No user is signed in.")
        }
    }

    private fun fetchUserData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val email = document.getString("email") ?: "No email"
                } else {
                    Log.d("MainActivity", "No such user document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting user data", exception)
            }
    }
}
