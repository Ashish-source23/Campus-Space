package com.example.campusspace.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusspace.databinding.ActivityShareBinding

// 1. Inherit from AppCompatActivity
class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.shareAppToolbar.setNavigationOnClickListener {
            finish()
        }
    }
}