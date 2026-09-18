package com.netspeed.tracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.netspeed.tracker.data.pref.PreferenceManager
import com.netspeed.tracker.databinding.FragmentSettingsBinding
import com.netspeed.tracker.service.FloatingWidgetService

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PreferenceManager(requireContext())
        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        binding.etDailyLimit.setText(prefManager.dailyLimitGb.toString())
        binding.switchAlert80.isChecked = prefManager.isAlert80Enabled
        binding.switchAlert90.isChecked = prefManager.isAlert90Enabled
        binding.switchAlert100.isChecked = prefManager.isAlert100Enabled
        binding.switchFloatingBubble.isChecked = prefManager.isFloatingBubbleEnabled
        binding.switchAutoStartBoot.isChecked = prefManager.isAutoStartOnBootEnabled
        binding.switchHideLockscreen.isChecked = prefManager.isHideOnLockscreen
        binding.switchAmoledTheme.isChecked = prefManager.isAmoledTheme
        if (prefManager.useBitsUnit) {
            binding.rbUnitBits.isChecked = true
        } else {
            binding.rbUnitBytes.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.switchAlert80.setOnCheckedChangeListener { _, checked ->
            prefManager.isAlert80Enabled = checked
        }
        binding.switchAlert90.setOnCheckedChangeListener { _, checked ->
            prefManager.isAlert90Enabled = checked
        }
        binding.switchAlert100.setOnCheckedChangeListener { _, checked ->
            prefManager.isAlert100Enabled = checked
        }

        binding.switchAutoStartBoot.setOnCheckedChangeListener { _, checked ->
            prefManager.isAutoStartOnBootEnabled = checked
        }

        binding.switchHideLockscreen.setOnCheckedChangeListener { _, checked ->
            prefManager.isHideOnLockscreen = checked
        }

        binding.switchAmoledTheme.setOnCheckedChangeListener { _, checked ->
            prefManager.isAmoledTheme = checked
        }

        binding.rgSpeedUnits.setOnCheckedChangeListener { _, checkedId ->
            prefManager.useBitsUnit = (checkedId == binding.rbUnitBits.id)
        }

        // Daily limit: save on text change
        binding.etDailyLimit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveDailyLimit()
        }

        // Floating Speed Bubble toggle
        binding.switchFloatingBubble.setOnCheckedChangeListener { _, checked ->
            prefManager.isFloatingBubbleEnabled = checked
            if (checked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
                    Toast.makeText(
                        requireContext(),
                        "Please grant 'Display over other apps' permission for the floating bubble.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivity(intent)
                    binding.switchFloatingBubble.isChecked = false
                    prefManager.isFloatingBubbleEnabled = false
                } else {
                    if (!FloatingWidgetService.isRunning) {
                        val serviceIntent = Intent(requireContext(), FloatingWidgetService::class.java)
                        requireContext().startService(serviceIntent)
                    }
                }
            } else {
                if (FloatingWidgetService.isRunning) {
                    val serviceIntent = Intent(requireContext(), FloatingWidgetService::class.java)
                    requireContext().stopService(serviceIntent)
                }
            }
        }
    }

    private fun saveDailyLimit() {
        val text = binding.etDailyLimit.text?.toString()?.trim()
        val gb = text?.toFloatOrNull()
        if (gb != null && gb > 0f) {
            prefManager.dailyLimitGb = gb
        } else {
            binding.etDailyLimit.setText(prefManager.dailyLimitGb.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveDailyLimit()
        _binding = null
    }
}
