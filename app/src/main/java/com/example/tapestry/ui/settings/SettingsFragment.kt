package com.example.tapestry.ui.settings

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.example.tapestry.BuildConfig
import com.example.tapestry.R
import com.example.tapestry.databinding.FragmentSettingsBinding
import com.example.tapestry.ui.home.HomeFragment
import com.example.tapestry.utils.AppUtils
import com.example.tapestry.utils.ChangeWallpaper
import com.google.android.gms.oss.licenses.OssLicensesActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var preferences: SharedPreferences
    private var dark = false
    private var grid = false
    private var alarmChanged = false
    private var stateChanged = false
    private var wallAlarm: AlarmManager? = null
    private var wallChangeIntent: Intent? = null
    private var pending: PendingIntent? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("RESET", "settings init")
        val context = requireContext()
        preferences = AppUtils.getPreferences(context)

        // Display Version code in Settings
        version.text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        wallChangeIntent = Intent(context, ChangeWallpaper::class.java)
        wallAlarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        wallChangeIntent?.action = "CHANGE_WALL"
        pending = PendingIntent.getBroadcast(
            context,
            2,
            wallChangeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        binding.changeIntervalTitle.visibility = View.GONE

        binding.previewRes.isChecked = preferences.getBoolean(PREVIEW_RES, false)
        binding.downloadOrigin.isChecked = preferences.getBoolean(DOWNLOAD_ORIGIN, false)
        binding.randomSwitch.isChecked = preferences.getBoolean(RANDOM_ENABLED, false)
        showRandomSettings(binding.randomSwitch.isChecked)

        binding.intervalSeek.progress = preferences.getInt(RANDOM_INTERVAL, 0)
        binding.intervalCount.text = (binding.intervalSeek.progress + 1).toString() + " hrs"

        // Google Play Services
        // Open Source Licenses Used in Project
        binding.ossLicense.setOnClickListener {
            val intent = Intent(activity, OssLicensesMenuActivity::class.java)
            activity?.startActivity(intent)
        }

        binding.randomSwitch.setOnCheckedChangeListener { _, b ->
            preferences.edit().putBoolean(RANDOM_ENABLED, b).apply()
            alarmChanged = b
            showRandomSettings(b)
        }
        binding.downloadOrigin.setOnCheckedChangeListener { _, b ->
            preferences.edit().putBoolean(DOWNLOAD_ORIGIN, b).apply()
        }
        binding.previewRes.setOnCheckedChangeListener { _, b ->
            preferences.edit().putBoolean(PREVIEW_RES, b).apply()
        }

        binding.intervalSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                alarmChanged = true
                if (i > 0) {
                    binding.intervalCount.text = (i + 1).toString() + " hrs"
                } else {
                    binding.intervalCount.text = "1 hrs"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        val randButton = binding.randomLocationButton
        when (preferences.getInt(RANDOM_LOCATION, 2)) {
            0 -> {
                randButton.text = "HOME"
            }

            1 -> {
                randButton.text = "LOCK"
            }

            2 -> {
                randButton.text = "BOTH"
            }
        }

        val dimensions = AppUtils.getWallDimensions(context)
        binding.widthEdit.setText(dimensions[0].toString() + "")
        binding.heightEdit.setText(dimensions[1].toString() + "")

        // Default Subreddit Selection
        // Default Sub listed in companion objects
        val defaultSub = preferences.getString(DEFAULT, DEFAULT_SUB)
        binding.defaultEdit.setText(defaultSub)

        // Toggles Light and Dark Theme
        dark = preferences.getBoolean(DARK, false)
        binding.darkSwitch.isChecked = dark
        binding.darkSwitch.setOnCheckedChangeListener { _, b ->
            dark = b
            stateChanged = true
            preferences.edit().putBoolean(DARK, dark).apply()
            if (b) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                (activity as AppCompatActivity).delegate.applyDayNight()

            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                (activity as AppCompatActivity).delegate.applyDayNight()
            }
        }

        // Save Settings
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.randomLocationButton.setOnClickListener {
            val but = binding.randomLocationButton
            val builder = MaterialAlertDialogBuilder(context, R.style.MyThemeOverlayAlertDialog)
            builder.setTitle("Set Where?")
                .setItems(R.array.location_options) { _, i ->
                    when (i) {
                        0 -> {
                            but.text = "HOME"
                            preferences.edit()?.putInt(RANDOM_LOCATION, HOME)?.apply()
                        }

                        1 -> {
                            but.text = "LOCK"
                            preferences.edit()?.putInt(RANDOM_LOCATION, LOCK)?.apply()
                        }

                        2 -> {
                            but.text = "BOTH"
                            preferences.edit()?.putInt(RANDOM_LOCATION, BOTH)?.apply()
                        }

                        else -> {
                            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            builder.create().show()
        }
    }

    private fun showRandomSettings(show: Boolean) {
        val setting = if (show) View.VISIBLE else View.GONE
        binding.refreshLocationSetting.visibility = setting
        binding.randomSeekSection.visibility = setting
        binding.changeIntervalTitle.visibility = setting
    }

    private fun saveSettings() {
        val context = requireContext()
        var valid = true
        val preferenceEditor = preferences.edit()

        preferenceEditor.putInt(RANDOM_INTERVAL, binding.intervalSeek.progress)
        val setRefresh = preferences.getBoolean(RANDOM_ENABLED, false) && alarmChanged

        if (setRefresh) {
            val interval = (binding.intervalSeek.progress + 1) * 60 * 60 * 1000
            val triggerTime = SystemClock.elapsedRealtime() + interval
            if (wallAlarm != null) {
                Toast.makeText(context, "Random refresh enabled", Toast.LENGTH_SHORT).show()
                wallAlarm!!.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    interval.toLong(),
                    pending
                )
            }
        } else {
            if (wallAlarm != null && !preferences.getBoolean(RANDOM_ENABLED, false)) {
                Toast.makeText(context, "Random refresh disabled", Toast.LENGTH_SHORT).show()
                wallAlarm!!.cancel(pending!!)
            }
        }

        val dims = AppUtils.getWallDimensions(context)
        var width = dims[0]
        var height = dims[1]
        try {
            width = Integer.parseInt(binding.widthEdit.text.toString())
            height = Integer.parseInt(binding.heightEdit.text.toString())
        } catch (e: NumberFormatException) {
            valid = false
            Toast.makeText(context, "You didn't enter a valid number", Toast.LENGTH_SHORT).show()
        }

        if (valid) {
            preferenceEditor.putInt(IMG_WIDTH, width)
            preferenceEditor.putInt(IMG_HEIGHT, height)
        }

        val defaultSub = binding.defaultEdit.text.toString().replace(" ", "")
        preferenceEditor.putString(DEFAULT, defaultSub)
        preferenceEditor.apply()
        Toast.makeText(context, "Saved Settings", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        //pref keys
        const val DEFAULT_SUB = "earthporn"
        const val PREVIEW_RES = "PREVIEWRES"
        const val SHOW_INFO = "INFOCARD"
        const val SORT_METHOD = "SORTIMG"
        const val IMG_WIDTH = "WIDTH"
        const val IMG_HEIGHT = "HEIGHT"
        const val DEFAULT = "DEFAULT"
        const val LOAD_SCALE = "LOAD"
        const val LOAD_GIF = "LOADGIF"
        const val DARK = "DARK"
        const val GRID = "GRID"
        const val DOWNLOAD_ORIGIN = "DOWNLOAD_ORIGINAL"
        const val RANDOM_ENABLED = "SWITCHING_ENABLED"
        const val RANDOM_INTERVAL = "INTERVAL"
        const val RANDOM_LOCATION = "RAND_LOCATION"
        const val HOME = 0
        const val LOCK = 1
        const val BOTH = 2
    }
}
