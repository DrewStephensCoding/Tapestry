package com.example.tapestry.ui.search

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tapestry.R
import com.example.tapestry.databinding.FragmentSearchBinding
import com.example.tapestry.objects.RecyclerListener
import com.example.tapestry.ui.home.HomeFragment
import com.example.tapestry.ui.settings.SettingsFragment
import com.example.tapestry.ui.subsearch.SubAdapter
import com.example.tapestry.ui.subsearch.SubViewModel
import com.example.tapestry.utils.AppUtils
import kotlinx.coroutines.InternalCoroutinesApi

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private var subLoading = false
    private var searched = false

    private lateinit var prefs: SharedPreferences
    private lateinit var subViewModel: SubViewModel

    private lateinit var subAdapter: SubAdapter

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.reddit_sort, menu)
    }

    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("RESET", "search init")
        prefs = AppUtils.getPreferences(requireContext())

        subViewModel = ViewModelProvider(this).get(SubViewModel::class.java)

        val prefs = AppUtils.getPreferences(requireContext())

        subAdapter = SubAdapter()

        binding.subsScroll.adapter = subAdapter

        if (!searched) {

            //binding.errorInfo.text = "Search for subs..."
            Log.e("SUB", "SEARCHED")
            showView(HomeFragment.ERROR_INFO)

            subViewModel.subreddits.observe(viewLifecycleOwner, Observer {
                binding.emptyInfoImg.visibility = View.VISIBLE
                binding.errorInfo.text = "Start searching for subs..."
                if (it.isEmpty() && subLoading) {
                    showView(HomeFragment.LOADING)
                    binding.emptyInfoImg.visibility = View.GONE
                } else if (it.isEmpty() && !subLoading && searched) {
                    showView(HomeFragment.ERROR_INFO)
                    binding.emptyInfoImg.visibility = View.GONE
                    binding.errorInfo.text = getString(R.string.search_error_info_msg)
                } else if (searched) {
                    showView(HomeFragment.SUB_SCROLL)
                    binding.emptyInfoImg.visibility = View.GONE
                    subAdapter.updateSubreddits(it)
                }
            })

            subViewModel.isLoading.observe(viewLifecycleOwner, Observer {
                subLoading = it
            })

            binding.subsScroll.addOnItemTouchListener(
                RecyclerListener(requireContext(),
                    binding.subsScroll, object : RecyclerListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            val subName = subAdapter.subreddits[position].subName.substring(2)
                            val toSubImages =
                                SearchFragmentDirections.actionSearchFragmentToSubImagesFragment(
                                    subName
                                )
                            findNavController().navigate(toSubImages)
                        }

                        override fun onLongItemClick(view: View?, position: Int) {
                            prefs.edit().putString(
                                SettingsFragment.DEFAULT,
                                subAdapter.subreddits[position].subName.substring(2)
                            ).apply()
                            Toast.makeText(requireContext(), "Set as default", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
            )


            binding.search.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val inputManager =
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(
                        binding.root.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                    searched = true
                    subViewModel.searchSubs(binding.search.text.toString())
                    return@OnEditorActionListener true
                }
                false
            })
        }
    }

    private fun showView(view: Int) {

        if (view == HomeFragment.SUB_SCROLL) {
            binding.subsScroll.visibility = View.VISIBLE
        } else {
            binding.subsScroll.visibility = View.GONE
        }

        if (view == HomeFragment.LOADING) {
            binding.loading.visibility = View.VISIBLE
        } else {
            binding.loading.visibility = View.GONE
        }

        if (view == HomeFragment.BOTTOM_LOADING) {
            binding.bottomLoad.visibility = View.VISIBLE
        } else {
            binding.bottomLoad.visibility = View.GONE
        }

        if (view == HomeFragment.ERROR_INFO) {
            binding.errorInfo.visibility = View.VISIBLE
        } else {
            binding.errorInfo.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}