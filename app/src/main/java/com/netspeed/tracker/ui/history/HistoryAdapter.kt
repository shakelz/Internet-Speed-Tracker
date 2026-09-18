package com.netspeed.tracker.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.netspeed.tracker.data.db.DailyUsageEntity
import com.netspeed.tracker.databinding.ItemHistoryRowBinding
import com.netspeed.tracker.utils.SpeedUtils

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var items: List<DailyUsageEntity> = emptyList()

    fun submitList(newItems: List<DailyUsageEntity>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(private val binding: ItemHistoryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyUsageEntity) {
            binding.tvHistoryDate.text = item.date
            binding.tvHistoryMobile.text = SpeedUtils.formatBytes(item.mobileBytes)
            binding.tvHistoryWifi.text = SpeedUtils.formatBytes(item.wifiBytes)
            binding.tvHistoryTotal.text = SpeedUtils.formatBytes(item.totalBytes)
        }
    }
}
