@file:Suppress("DEPRECATION")

package com.example.tapestry.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tapestry.R
import com.example.tapestry.databinding.ActivityMainBinding
import com.example.tapestry.ui.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.InternalCoroutinesApi

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private var preferences: SharedPreferences? = null
    private lateinit var binding: ActivityMainBinding

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.elevation = 0.0F

        preferences = getSharedPreferences(SharedPrefFile, Context.MODE_PRIVATE)

        val dark = preferences!!.getBoolean(SettingsFragment.DARK, false)
        if (dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            delegate.applyDayNight()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            delegate.applyDayNight()
        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.fragNavHost)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.subImagesFragment) {
                binding.navView.visibility = View.GONE
            } else {
                binding.navView.visibility = View.VISIBLE
            }
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_favorites,
                R.id.searchFragment,
                R.id.navigation_history,
                R.id.navigation_settings,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragNavHost)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.cacheDir.deleteRecursively()
        Log.e("DESTROY", "main")
    }

    companion object {
        const val SharedPrefFile = "com.example.tapestry"
    }
}