package com.netspeed.tracker.ui.history

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.netspeed.tracker.data.db.AppDatabase
import com.netspeed.tracker.databinding.FragmentHistoryBinding
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter

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

        database = AppDatabase.getDatabase(requireContext())
        historyAdapter = HistoryAdapter()

        binding.rvHistoryLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistoryLogs.adapter = historyAdapter

        observeData()

        binding.btnExportCsv.setOnClickListener {
            exportToCsv()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe 7-day data for the chart
                launch {
                    database.dailyUsageDao().getLast7Days().collect { sevenDayData ->
                        if (_binding != null) {
                            binding.usageChartView.setUsageData(sevenDayData)
                        }
                    }
                }

                // Observe 30-day data for the table
                launch {
                    database.dailyUsageDao().getLast30Days().collect { thirtyDayData ->
                        if (_binding != null) {
                            historyAdapter.submitList(thirtyDayData)
                        }
                    }
                }

                // Observe all-time total
                launch {
                    database.dailyUsageDao().getTotalAllTime().collect { total ->
                        if (_binding != null) {
                            binding.tvTotalAllTime.text = SpeedUtils.formatBytes(total ?: 0L)
                        }
                    }
                }
            }
        }
    }

    private fun exportToCsv() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val allData = database.dailyUsageDao().getAllHistory()
                if (allData.isEmpty()) {
                    Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val csvContent = buildString {
                    appendLine("Date,Mobile Data (Bytes),Wi-Fi Data (Bytes),Total (Bytes)")
                    allData.forEach { row ->
                        appendLine("${row.date},${row.mobileBytes},${row.wifiBytes},${row.totalBytes}")
                    }
                }

                val filename = "NetSpeedPro_Usage_${SpeedUtils.getTodayDateString()}.csv"

                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        stream.write(csvContent.toByteArray())
                    }
                    Toast.makeText(requireContext(), "Exported to Downloads: $filename", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
