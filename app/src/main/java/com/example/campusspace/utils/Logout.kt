package com.example.campusspace.utils

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.example.campusspace.ui.LoginActivity
import com.google.firebase.auth.FirebaseAuth


fun showLogoutDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
