package com.example.tapestry.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tapestry.R
import com.example.tapestry.databinding.FragmentHomeBinding
import com.example.tapestry.objects.RecyclerListener
import com.example.tapestry.ui.settings.SettingsFragment
import com.example.tapestry.utils.AppUtils
import com.example.tapestry.utils.RedditAPI
import kotlinx.coroutines.InternalCoroutinesApi

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    lateinit var imageViewModel: ImageViewModel
    private lateinit var imageAdapter: ImageAdapter
    private var isLoading = false
    private var isGridLayout = false


    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("RESET", "home init")

        imageViewModel =
            ViewModelProvider(this).get(ImageViewModel::class.java)

        val prefs = AppUtils.getPreferences(requireContext())
        val defaultSub =
            prefs.getString(SettingsFragment.DEFAULT, SettingsFragment.DEFAULT_SUB).toString()

        val dimensions = AppUtils.getDimensions(requireContext())
        val lowRes = prefs.getBoolean(SettingsFragment.PREVIEW_RES, false)
        imageAdapter = ImageAdapter(dimensions, lowRes)
        binding.imageScroll.adapter = imageAdapter

        if (!AppUtils.networkAvailable(requireContext())) {
            binding.imageScroll.visibility = View.GONE
            binding.errorInfo.visibility = View.VISIBLE
            binding.errorInfo.text = getString(R.string.no_network)
        } else {
            imageViewModel.setQAndSort(defaultSub, RedditAPI.HOT)
        }

        imageViewModel.isLoading.observe(viewLifecycleOwner, Observer {
            isLoading = it
        })

        imageViewModel.wallImages.observe(viewLifecycleOwner, Observer {
            if (it.size == 0 && isLoading) {
                showView(LOADING)
            } else if (it.size == 0 && !isLoading) {
                showView(ERROR_INFO)
            } else {
                showView(IMAGE_SCROLL)
                imageAdapter.updateWallImages(it)
            }
        })

        binding.imageScroll.addOnItemTouchListener(
            RecyclerListener(requireContext(),
                binding.imageScroll, object : RecyclerListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        val selectedImage = imageAdapter.getWallImage(position)
                        AppUtils.startWallActivity(requireContext(), selectedImage)
                    }

                    override fun onLongItemClick(view: View?, position: Int) {
                        isGridLayout = !isGridLayout
                        binding.imageScroll.layoutManager = if (isGridLayout) GridLayoutManager(
                            context,
                            2
                        ) else LinearLayoutManager(context)
                    }
                })
        )

        binding.imageScroll.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    binding.bottomLoad.visibility = View.INVISIBLE
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && isLoading) {
                    binding.bottomLoad.visibility = View.VISIBLE
                } else if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    binding.bottomLoad.visibility = View.VISIBLE
                    imageViewModel.getNextImages()
                } else {
                    binding.bottomLoad.visibility = View.INVISIBLE
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.cacheDir?.deleteRecursively()
        Log.e("DESTROY", "home")
        _binding = null
        imageViewModel.clear()
    }

    private fun showView(view: Int) {
        if (view == IMAGE_SCROLL) {
            binding.imageScroll.visibility = View.VISIBLE
        } else {
            binding.imageScroll.visibility = View.GONE
        }

        if (view == LOADING) {
            binding.loading.visibility = View.VISIBLE
        } else {
            binding.loading.visibility = View.GONE
        }

        if (view == BOTTOM_LOADING) {
            binding.bottomLoad.visibility = View.VISIBLE
        } else {
            binding.bottomLoad.visibility = View.GONE
        }

        if (view == ERROR_INFO) {
            binding.errorInfo.visibility = View.VISIBLE
        } else {
            binding.errorInfo.visibility = View.GONE
        }
    }

    companion object {
        const val IMAGE_SCROLL = 0
        const val LOADING = 1
        const val BOTTOM_LOADING = 2
        const val ERROR_INFO = 3
        const val SUB_SCROLL = 4
    }
}