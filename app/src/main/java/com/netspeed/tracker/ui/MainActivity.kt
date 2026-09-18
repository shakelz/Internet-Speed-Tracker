package com.netspeed.tracker.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.netspeed.tracker.R
import com.netspeed.tracker.data.pref.PreferenceManager
import com.netspeed.tracker.databinding.ActivityMainBinding
import com.netspeed.tracker.service.SpeedNotificationService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupNavigation()
        requestNotificationPermission()
        startSpeedServiceIfNeeded()
        startStatusDotBlink()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun startSpeedServiceIfNeeded() {
        if (prefManager.isServiceEnabled && !SpeedNotificationService.isRunning) {
            val serviceIntent = Intent(this, SpeedNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun startStatusDotBlink() {
        val blink = AlphaAnimation(1f, 0.2f).apply {
            duration = 900
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.indicatorStatusDot.startAnimation(blink)
    }

    override fun onResume() {
        super.onResume()
        val isRunning = SpeedNotificationService.isRunning
        binding.indicatorStatusDot.visibility = if (isRunning) View.VISIBLE else View.INVISIBLE
    }
}
