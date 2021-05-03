package com.example.tapestry.ui.favorites

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.tapestry.R
import com.example.tapestry.activities.WallpaperActivity
import com.example.tapestry.databinding.FragmentFavoritesBinding
import com.example.tapestry.objects.RecyclerListener
import com.example.tapestry.objects.WallImage
import com.example.tapestry.ui.home.ImageAdapter
import com.example.tapestry.ui.settings.SettingsFragment
import com.example.tapestry.utils.AppUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var favoritesImageAdapter: ImageAdapter
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        favoritesViewModel =
            ViewModelProvider(this).get(FavoritesViewModel::class.java)

        val dimensions = AppUtils.getDimensions(requireContext())
        val lowRes = AppUtils.getPreferences(requireContext())
            .getBoolean(SettingsFragment.PREVIEW_RES, false)
        favoritesImageAdapter = ImageAdapter(dimensions, lowRes)
        binding.imageScroll.adapter = favoritesImageAdapter

        binding.imageScroll.addOnItemTouchListener(
            RecyclerListener(requireContext(),
                binding.imageScroll, object : RecyclerListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        val selectedImage = favoritesImageAdapter.getWallImage(position)
                        AppUtils.startWallActivity(requireContext(), selectedImage)
                    }

                    override fun onLongItemClick(view: View?, position: Int) {}
                })
        )

        favoritesViewModel.allFav.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                binding.imageScroll.visibility = View.GONE
                binding.emptyInfo.visibility = View.VISIBLE
                binding.emptyInfoImg.visibility = View.VISIBLE
            } else {
                binding.imageScroll.visibility = View.VISIBLE
                binding.emptyInfo.visibility = View.GONE
                binding.emptyInfoImg.visibility = View.GONE
                favoritesImageAdapter.updateWallImages(it as ArrayList<WallImage>)
            }
        })

        val speedView = binding.speedDial
        speedView.inflate(R.menu.fab_menu)
        speedView.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            val context = requireContext()
            when (actionItem.id) {
                R.id.delete_all -> {
                    val confirmDelete =
                        MaterialAlertDialogBuilder(
                            context,
                            R.style.MyThemeOverlayAlertDialog
                        ).apply {
                            setTitle("Are You Sure?")
                            setMessage("Do you want to clear ${favoritesImageAdapter.itemCount} favorite images?")
                            setPositiveButton("Yes") { _, _ ->
                                favoritesViewModel.deleteAll()
                                Toast.makeText(
                                    context,
                                    "Deleted favorite images",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            setNegativeButton("No") { _, _ ->
                                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    confirmDelete.show()
                    return@OnActionSelectedListener false
                }
                R.id.down_all -> {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            activity as Activity,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            WallpaperActivity.WRITE
                        )
                    } else {
                        if (favoritesImageAdapter.itemCount == 0) {
                            Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                            return@OnActionSelectedListener false
                        }

                        uiScope.launch {

                        }
                    }

                    return@OnActionSelectedListener false // false will close it without animation
                }
                R.id.random -> {
                    if (favoritesImageAdapter.itemCount == 0) {
                        Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                        return@OnActionSelectedListener false
                    }

                    var randomIndex = (0..favoritesImageAdapter.itemCount).random()
                    while (randomIndex >= favoritesImageAdapter.itemCount || randomIndex < 0) {
                        randomIndex = (0..favoritesImageAdapter.itemCount).random()
                    }

                    val selectedImage = favoritesImageAdapter.getWallImage(randomIndex)
                    AppUtils.startWallActivity(context, selectedImage)
                    return@OnActionSelectedListener false
                }
            }

            false
        })

        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}