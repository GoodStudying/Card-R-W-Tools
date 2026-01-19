package cn.ratnoumi.bcardtools

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard

class CardItemAdapter(
    val cards: List<BambuFilamentCard>,
    val onItemClick: (BambuFilamentCard) -> Unit,
    val onDelete: (BambuFilamentCard) -> Unit
) :
    RecyclerView.Adapter<CardItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.filamentTypeText.text = cards[position].detailedFilamentType
        holder.spoolWeightText.text = "${cards[position].spoolWeight}g"
        holder.filamentLengthText.text = "${cards[position].filamentLength}米"
        holder.UIDText.text = cards[position].uid
        holder.productionDate.text = cards[position].productionDate
        holder.filamentColorText.text = "#${Integer.toHexString(cards[position].color).uppercase()}"
        holder.filamentColorIndicator.setBackgroundColor(cards[position].color)
        holder.itemView.setOnClickListener {
            onItemClick(cards[position])
        }
        holder.itemView.setOnLongClickListener {
            showDeleteDialog(holder.itemView.context, position)
            true
        }
    }

    // 显示删除确认对话框
    private fun showDeleteDialog(context: Context, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("删除提示")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { dialog, _ ->
                notifyItemRemoved(position)
                // 通知外部处理删除逻辑
                onDelete(cards[position])
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true) // 点击外部可取消
            .show()
    }


    override fun getItemCount() = cards.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filamentTypeText: TextView = itemView.findViewById(R.id.filamentTypeText)
        val spoolWeightText: TextView = itemView.findViewById(R.id.spoolWeightText)
        val filamentLengthText: TextView = itemView.findViewById(R.id.filamentLengthText)
        val UIDText: TextView = itemView.findViewById(R.id.UIDText)
        val productionDate: TextView = itemView.findViewById(R.id.productionDateText)
        val filamentColorText: TextView = itemView.findViewById(R.id.filamentColorText)
        val filamentColorIndicator: TextView = itemView.findViewById(R.id.filamentColorIndicator)
    }
}