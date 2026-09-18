package com.netspeed.tracker.ui.appusage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.netspeed.tracker.R
import com.netspeed.tracker.data.model.AppUsageItem
import com.netspeed.tracker.databinding.ItemAppUsageBinding
import com.netspeed.tracker.utils.SpeedUtils

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var items: List<AppUsageItem> = emptyList()

    fun submitList(newItems: List<AppUsageItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppUsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AppUsageViewHolder(private val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppUsageItem) {
            binding.tvAppName.text = item.appName
            binding.tvAppMobileData.text = "Mobile: ${SpeedUtils.formatBytes(item.mobileBytes)}"
            binding.tvAppWifiData.text = "Wi-Fi: ${SpeedUtils.formatBytes(item.wifiBytes)}"
            binding.tvAppTotalData.text = SpeedUtils.formatBytes(item.totalBytes)

            if (item.icon != null) {
                binding.ivAppIcon.setImageDrawable(item.icon)
            } else {
                binding.ivAppIcon.setImageResource(R.drawable.ic_apps)
            }

            binding.progressAppUsage.progress = item.percentageOfMax
        }
    }
}
