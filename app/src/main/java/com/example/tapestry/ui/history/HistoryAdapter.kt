package com.example.tapestry.ui.history

import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tapestry.R
import com.example.tapestry.objects.HistoryItem
import com.example.tapestry.utils.AppUtils

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    var histories: ArrayList<HistoryItem> = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return histories.size
    }

    fun updateHistories(hists: ArrayList<HistoryItem>) {
        this.histories = hists
        notifyDataSetChanged()
    }

    fun getHistory(position: Int): HistoryItem {
        return histories[position]
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val current = histories[position]
        holder.bindTo(current)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.hist_name)
        private val sourceTv: TextView = itemView.findViewById(R.id.down_set)
        private val dateTv: TextView = itemView.findViewById(R.id.history_date)
        private val img: ImageView = itemView.findViewById(R.id.history_image)

        fun bindTo(history: HistoryItem) {
            val name = history.subName
            val date = history.setDate
            val source = HistoryItem.sources[history.source]

            nameTv.text = name
            sourceTv.text = source
            dateTv.text = AppUtils.convertUTC(date)
            Glide.with(img.context).load(history.imgUrl).fitCenter().into(img)
        }
    }
}