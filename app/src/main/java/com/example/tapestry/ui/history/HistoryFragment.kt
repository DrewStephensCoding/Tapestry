package com.example.tapestry.ui.history

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat.*
import androidx.core.content.ContextCompat.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.tapestry.R
import com.example.tapestry.R.*
import com.example.tapestry.activities.WallpaperActivity
import com.example.tapestry.databinding.FragmentHistoryBinding
import com.example.tapestry.objects.HistoryItem
import com.example.tapestry.objects.RecyclerListener
import com.example.tapestry.objects.WallImage
import com.example.tapestry.utils.AppUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.leinardi.android.speeddial.SpeedDialView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private var _binding: FragmentHistoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("RESET", "history init")
        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
        historyAdapter = HistoryAdapter()

        binding.historyScroll.adapter = historyAdapter
        binding.historyScroll.addOnItemTouchListener(
            RecyclerListener(requireContext(),
                binding.historyScroll, object : RecyclerListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        val selectedImage = historyAdapter.getHistory(position)
                        AppUtils.startWallActivity(
                            requireContext(), WallImage(
                                imgUrl = selectedImage.imgUrl,
                                subName = selectedImage.subName,
                                previewUrl = selectedImage.previewUrl,
                                postLink = selectedImage.postLink
                            )
                        )
                    }

                    override fun onLongItemClick(view: View?, position: Int) {}
                })
        )

        historyViewModel.allHist.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                binding.historyScroll.visibility = View.GONE
                binding.emptyInfo.visibility = View.VISIBLE
                binding.emptyInfoImg.visibility = View.VISIBLE
            } else {
                binding.historyScroll.visibility = View.VISIBLE
                binding.emptyInfo.visibility = View.GONE
                binding.emptyInfoImg.visibility = View.GONE
                historyAdapter.updateHistories(it as ArrayList<HistoryItem>)
            }
        })

        val speedView = binding.speedDial
        speedView.inflate(menu.fab_menu)
        speedView.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            val context = requireContext()
            when (actionItem.id) {
                R.id.delete_all -> {
                    val confirmDelete =
                        MaterialAlertDialogBuilder(context, style.MyThemeOverlayAlertDialog).apply {
                            setTitle("Are You Sure?")
                            setMessage("Do you want to clear ${historyAdapter.itemCount} history items?")
                            setPositiveButton("Yes") { _, _ ->
                                historyViewModel.deleteAll()
                                Toast.makeText(context, "Deleted history", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            setNegativeButton("No") { _, _ ->
                                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    confirmDelete.show()
                    return@OnActionSelectedListener false
                }
                R.id.down_all -> {
                    if (checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(
                            activity as Activity,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            WallpaperActivity.WRITE
                        )
                    } else {
                        if (historyAdapter.itemCount == 0) {
                            Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                            return@OnActionSelectedListener false
                        }

                        uiScope.launch {
                            AppUtils.downloadAllImages(
                                context,
                                historyAdapter.histories.map { it.imgUrl } as ArrayList<String>)
                        }
                    }

                    return@OnActionSelectedListener false // false will close it without animation
                }
                R.id.random -> {
                    if (historyAdapter.itemCount == 0) {
                        Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                        return@OnActionSelectedListener false
                    }

                    var randomIndex = (0..historyAdapter.itemCount).random()
                    while (randomIndex >= historyAdapter.itemCount || randomIndex < 0) {
                        randomIndex = (0..historyAdapter.itemCount).random()
                    }

                    val selectedImage = historyAdapter.getHistory(randomIndex)
                    AppUtils.startWallActivity(
                        requireContext(), WallImage(
                            imgUrl = selectedImage.imgUrl, subName = selectedImage.subName,
                            previewUrl = selectedImage.previewUrl, postLink = selectedImage.postLink
                        )
                    )
                    return@OnActionSelectedListener false
                }
            }

            false
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
