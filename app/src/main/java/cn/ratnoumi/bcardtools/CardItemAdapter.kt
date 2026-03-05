package cn.ratnoumi.bcardtools

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard

data class CardItem(
    val card: BambuFilamentCard,
    val count: Int
)

class CardItemAdapter(
    var items: List<CardItem>,
    val onItemClick: (CardItem) -> Unit,
    val onDelete: (BambuFilamentCard) -> Unit
) :
    RecyclerView.Adapter<CardItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val card = item.card
        
        if (item.count > 1) {
            holder.filamentTypeText.text = "${card.detailedFilamentType} (x${item.count})"
            holder.UIDText.text = "${card.uid}..."
        } else {
            holder.filamentTypeText.text = card.detailedFilamentType
            holder.UIDText.text = card.uid
        }
        
        holder.spoolWeightText.text = "${card.spoolWeight}g"
        holder.filamentLengthText.text = "${card.filamentLength}米"
        holder.productionDate.text = card.productionDate
        holder.colorNameText.text = card.colorName
        holder.filamentColorText.text = "#${Integer.toHexString(card.color).uppercase()}"
        holder.colorIndicator.setBackgroundColor(card.color)
        holder.colorBlock.setBackgroundColor(card.color)
        holder.itemView.setOnClickListener {
            onItemClick(item)
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
                onDelete(items[position].card)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true) // 点击外部可取消
            .show()
    }


    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filamentTypeText: TextView = itemView.findViewById(R.id.filamentTypeText)
        val spoolWeightText: TextView = itemView.findViewById(R.id.spoolWeightText)
        val filamentLengthText: TextView = itemView.findViewById(R.id.filamentLengthText)
        val UIDText: TextView = itemView.findViewById(R.id.UIDText)
        val productionDate: TextView = itemView.findViewById(R.id.productionDateText)
        val filamentColorText: TextView = itemView.findViewById(R.id.filamentColorText)
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        val colorBlock: View = itemView.findViewById(R.id.colorBlock)
        val colorNameText: TextView = itemView.findViewById(R.id.colorNameText)
    }
}