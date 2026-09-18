package com.netspeed.tracker.ui.speedtest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.netspeed.tracker.databinding.FragmentSpeedTestBinding
import com.netspeed.tracker.speedtest.SpeedTestEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class SpeedTestFragment : Fragment() {

    private var _binding: FragmentSpeedTestBinding? = null
    private val binding get() = _binding!!

    private val speedTestEngine = SpeedTestEngine()
    private var testJob: Job? = null
    private var isTesting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnActionSpeedTest.setOnClickListener {
            if (isTesting) return@setOnClickListener
            startSpeedTest()
        }
    }

    private fun startSpeedTest() {
        isTesting = true
        binding.btnActionSpeedTest.text = getString(com.netspeed.tracker.R.string.btn_testing)
        binding.btnActionSpeedTest.isEnabled = false

        // Reset results
        binding.tvResultPing.text = "-- ms"
        binding.tvResultJitter.text = "-- ms"
        binding.tvResultPeakDl.text = "-- Mbps"
        binding.tvResultPeakUl.text = "-- Mbps"
        binding.tvCurrentTestSpeed.text = "0.0"
        binding.speedometerView.setSpeed(0f, 1000f)

        val listener = object : SpeedTestEngine.SpeedTestListener {
            override fun onStatusChanged(status: String) {
                activity?.runOnUiThread {
                    binding.tvSpeedTestStatus.text = status
                }
            }

            override fun onPingCompleted(pingMs: Long, jitterMs: Long) {
                activity?.runOnUiThread {
                    binding.tvResultPing.text = "$pingMs ms"
                    binding.tvResultJitter.text = "$jitterMs ms"
                }
            }

            override fun onDownloadProgress(currentMbps: Float, peakMbps: Float) {
                activity?.runOnUiThread {
                    binding.tvCurrentTestSpeed.text = String.format(Locale.US, "%.1f", currentMbps)
                    binding.tvCurrentTestUnit.text = "Mbps DL"
                    binding.tvResultPeakDl.text = String.format(Locale.US, "%.1f Mbps", peakMbps)
                    binding.speedometerView.setSpeed(currentMbps, 200f)
                }
            }

            override fun onUploadProgress(currentMbps: Float, peakMbps: Float) {
                activity?.runOnUiThread {
                    binding.tvCurrentTestSpeed.text = String.format(Locale.US, "%.1f", currentMbps)
                    binding.tvCurrentTestUnit.text = "Mbps UL"
                    binding.tvResultPeakUl.text = String.format(Locale.US, "%.1f Mbps", peakMbps)
                    binding.speedometerView.setSpeed(currentMbps, 100f)
                }
            }

            override fun onTestComplete(result: SpeedTestEngine.SpeedTestResult) {
                activity?.runOnUiThread {
                    binding.tvCurrentTestSpeed.text = String.format(Locale.US, "%.1f", result.downloadMbps)
                    binding.tvCurrentTestUnit.text = "Mbps"
                    binding.speedometerView.setSpeed(result.downloadMbps, 200f)
                    resetButton()
                }
            }

            override fun onError(message: String) {
                activity?.runOnUiThread {
                    binding.tvSpeedTestStatus.text = "Error: $message"
                    resetButton()
                }
            }
        }

        testJob = viewLifecycleOwner.lifecycleScope.launch {
            speedTestEngine.runSpeedTest(listener)
        }
    }

    private fun resetButton() {
        isTesting = false
        binding.btnActionSpeedTest.text = getString(com.netspeed.tracker.R.string.btn_start_test)
        binding.btnActionSpeedTest.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testJob?.cancel()
        _binding = null
    }
}
