package com.netspeed.tracker.ui.dashboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.netspeed.tracker.data.NetworkStatsHelper
import com.netspeed.tracker.data.pref.PreferenceManager
import com.netspeed.tracker.databinding.FragmentDashboardBinding
import com.netspeed.tracker.service.SpeedNotificationService
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PreferenceManager
    private lateinit var networkStatsHelper: NetworkStatsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PreferenceManager(requireContext())
        networkStatsHelper = NetworkStatsHelper(requireContext())

        setupServiceSwitch()
        observeLiveSpeed()
        refreshNetworkDiagnostics()
    }

    private fun setupServiceSwitch() {
        binding.switchService.isChecked = prefManager.isServiceEnabled && SpeedNotificationService.isRunning

        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isServiceEnabled = isChecked
            val serviceIntent = Intent(requireContext(), SpeedNotificationService::class.java)
            if (isChecked) {
                binding.tvServiceStatus.text = "Tracking Active"
                if (!SpeedNotificationService.isRunning) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireContext().startForegroundService(serviceIntent)
                    } else {
                        requireContext().startService(serviceIntent)
                    }
                }
            } else {
                binding.tvServiceStatus.text = "Tracking Paused"
                requireContext().stopService(serviceIntent)
                resetSpeedDisplay()
            }
        }
    }

    private fun observeLiveSpeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            SpeedNotificationService.liveSpeedFlow.collectLatest { speedData ->
                if (!isAdded || _binding == null) return@collectLatest

                val useBits = prefManager.useBitsUnit
                val (dlVal, dlUnit) = SpeedUtils.formatSpeed(speedData.downloadBytesPerSec, useBits)
                val (ulVal, ulUnit) = SpeedUtils.formatSpeed(speedData.uploadBytesPerSec, useBits)

                binding.tvLiveDownloadSpeed.text = dlVal
                binding.tvLiveDownloadUnit.text = dlUnit
                binding.tvLiveUploadSpeed.text = ulVal
                binding.tvLiveUploadUnit.text = ulUnit

                binding.tvTodayMobileUsage.text = SpeedUtils.formatBytes(speedData.todayMobileBytes)
                binding.tvTodayWifiUsage.text = SpeedUtils.formatBytes(speedData.todayWifiBytes)

                // Quota progress bar
                val limitBytes = (prefManager.dailyLimitGb * 1024f * 1024f * 1024f).toLong()
                if (limitBytes > 0) {
                    val pct = ((speedData.todayMobileBytes.toDouble() / limitBytes) * 100).toInt().coerceIn(0, 100)
                    binding.progressMobileQuota.progress = pct
                }
            }
        }
    }

    private fun refreshNetworkDiagnostics() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                if (!isAdded || _binding == null) break
                val info = networkStatsHelper.getNetworkInfo()

                binding.tvSimCarrierName.text = when {
                    info.type == "Wi-Fi" -> "SSID: ${info.name}"
                    else -> "Carrier: ${info.name}"
                }

                binding.tvWifiSsidName.text = when {
                    info.type == "Wi-Fi" -> "SSID: ${info.name}"
                    else -> "Mobile: ${info.name}"
                }

                // Signal strength readable
                val signalText = when {
                    info.signalLevel >= -50 -> "Excellent (${info.signalLevel} dBm)"
                    info.signalLevel >= -60 -> "Good (${info.signalLevel} dBm)"
                    info.signalLevel >= -70 -> "Fair (${info.signalLevel} dBm)"
                    else -> "Weak (${info.signalLevel} dBm)"
                }

                binding.tvDiagSignal.text = signalText
                binding.tvDiagLinkSpeed.text = info.linkSpeed
                binding.tvDiagFrequency.text = info.extra

                delay(10_000L) // Refresh diagnostics every 10 seconds
            }
        }
    }

    private fun resetSpeedDisplay() {
        binding.tvLiveDownloadSpeed.text = "0.0"
        binding.tvLiveDownloadUnit.text = "KB/s"
        binding.tvLiveUploadSpeed.text = "0.0"
        binding.tvLiveUploadUnit.text = "KB/s"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
