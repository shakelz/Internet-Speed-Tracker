package com.netspeed.tracker.ui.appusage

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.netspeed.tracker.data.NetworkStatsHelper
import com.netspeed.tracker.databinding.FragmentAppUsageBinding
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.launch

class AppUsageFragment : Fragment() {

    private var _binding: FragmentAppUsageBinding? = null
    private val binding get() = _binding!!

    private lateinit var networkStatsHelper: NetworkStatsHelper
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppUsageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkStatsHelper = NetworkStatsHelper(requireContext())
        adapter = AppUsageAdapter()

        binding.rvAppUsage.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppUsage.adapter = adapter

        checkPermissionAndLoad("today")

        binding.toggleGroupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val period = when (checkedId) {
                    binding.btnFilterYesterday.id -> "yesterday"
                    binding.btnFilterMonth.id -> "month"
                    else -> "today"
                }
                loadUsage(period)
            }
        }

        binding.btnGrantUsagePermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission in case user came back from settings
        checkPermissionAndLoad("today")
    }

    private fun checkPermissionAndLoad(period: String) {
        if (networkStatsHelper.hasUsageStatsPermission()) {
            binding.cardPermissionWarning.visibility = View.GONE
            loadUsage(period)
        } else {
            binding.cardPermissionWarning.visibility = View.VISIBLE
        }
    }

    private fun loadUsage(period: String) {
        val startTime = when (period) {
            "yesterday" -> SpeedUtils.getStartOfYesterdayMillis()
            "month" -> SpeedUtils.getStartOfMonthMillis()
            else -> SpeedUtils.getStartOfTodayMillis()
        }
        val endTime = System.currentTimeMillis()

        viewLifecycleOwner.lifecycleScope.launch {
            val items = networkStatsHelper.getAppWiseUsage(startTime, endTime)
            if (items.isEmpty()) {
                binding.rvAppUsage.visibility = View.GONE
                binding.tvEmptyApps.visibility = View.VISIBLE
            } else {
                binding.rvAppUsage.visibility = View.VISIBLE
                binding.tvEmptyApps.visibility = View.GONE
                adapter.submitList(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
