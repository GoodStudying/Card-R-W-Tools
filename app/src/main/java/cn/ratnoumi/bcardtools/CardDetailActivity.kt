package cn.ratnoumi.bcardtools

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import cn.ratnoumi.bcardtools.databinding.ActivityCardDetailBinding
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.bambu.bambuKdf
import cn.ratnoumi.bcardtools.drive.mifare.defaultKeys
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyA
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyB
import cn.ratnoumi.bcardtools.dao.BambuFilamentDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardDetailActivity : BaseNfcAppCompatActivity() {
    private lateinit var binding: ActivityCardDetailBinding
    var bambuFilamentCard: BambuFilamentCard? = null
    lateinit var bambuFilamentDao: BambuFilamentDao

    override fun processTag(intent: Intent?) {
        if (bambuFilamentCard == null || binding.writeView.visibility != View.VISIBLE) return
        val tag: Tag?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let {
            binding.writeText.text = "正在写入NFC...\n"
            CoroutineScope(Dispatchers.IO).launch {
                bambuFilamentCard?.let { bambuCard ->
                    try {
                        val classic = MifareClassic.get(tag)
                        val keys = mutableListOf<ByteArray>()
                        keys.addAll(bambuKdf(classic.tag.id))
                        keys.addAll(defaultKeys)
                        // 连接卡片
                        if (!classic.isConnected) {
                            classic.connect()
                        }
                        //
                        findMifareSectorKeyA(classic, 0, keys)
                        if (classic.readBlock(0).toHexString() != bambuCard.card.blocks[0].toHexString()) {
                            try {
                                classic.writeBlock(0, bambuCard.card.blocks[0])
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    binding.writeText.text = "请刷要写入的新FUID卡片\n"
                                    Toast.makeText(baseContext, "Block0写入失败!", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                        }
                        // 写入之后的Block
                        println(bambuCard.card.getSectorCount())
                        for (sectorIndex in 0..<bambuCard.card.getSectorCount()) {
                            try {
                                val sectorStart = bambuCard.card.sectorToBlock(sectorIndex)
                                findMifareSectorKeyA(classic, sectorIndex, keys)
                                findMifareSectorKeyB(classic, sectorIndex, keys)
                                for (i in 0..<bambuCard.card.getBlockCountInSector(sectorIndex)) {
                                    println("$sectorIndex - $i - $sectorStart - ${sectorStart + i}")
                                    if ((sectorStart + i) == 0) continue
                                    classic.writeBlock(sectorStart + i, bambuCard.card.blocks[sectorStart + i])
                                }
                                withContext(Dispatchers.Main) {
                                    binding.writeText.text = binding.writeText.text.toString() +
                                            "正在写入: 第${sectorIndex}扇区,共${classic.sectorCount}扇区\n"
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    binding.writeText.text = binding.writeText.text.toString() +
                                            "第${sectorIndex}扇区:写入失败\n"
                                }
                            }

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            binding.writeText.text = "请刷要写入的新FUID卡片\n"
                            Toast.makeText(baseContext, "写入失败!", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.writeText.text = "请刷要写入的新FUID卡片\n"
                    Toast.makeText(baseContext, "写入完成!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bambuFilamentCard = intent.getParcelableExtra("card", BambuFilamentCard::class.java)
        } else {
            // 低版本用旧方法（加抑制警告注解）
            @Suppress("DEPRECATION")
            bambuFilamentCard = intent.getParcelableExtra("card")
        }
        bambuFilamentDao = BambuFilamentDao(baseContext)
        binding.createNewCardBtn.setOnClickListener {
            binding.writeView.visibility = View.VISIBLE
        }
        binding.closeText.setOnClickListener {
            binding.writeView.visibility = View.GONE
        }
        binding.saveBtn.setOnClickListener { view ->
            bambuFilamentCard?.let { item ->
                try {
                    if (bambuFilamentDao.exist(item.uid)) {
                        showDeleteDialog(view.context, item)
                    } else {
                        bambuFilamentDao.add(item)
                        Toast.makeText(baseContext, "保存成功!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    showDeleteDialog(view.context, item)
                }
            }
        }
        updateView()
    }

    // 显示删除确认对话框
    private fun showDeleteDialog(context: Context, bambuCard: BambuFilamentCard) {
        AlertDialog.Builder(context)
            .setTitle("保存提示")
            .setMessage("记录已存在是否覆盖之前的记录？")
            .setPositiveButton("覆盖") { dialog, _ ->
                bambuFilamentDao.delete(bambuCard.uid)
                bambuFilamentDao.add(bambuCard)
                Toast.makeText(baseContext, "保存成功!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true) // 点击外部可取消
            .show()
    }


    fun updateView() {
        bambuFilamentCard?.let { info ->
            binding.UIDText.text = info.uid
            binding.materialIDText.text = "${info.materialID}/${info.materialVariantID}"
            binding.productionDateText.text = info.productionDate
            binding.filamentTypeText.text = info.detailedFilamentType
            binding.filamentColorText.text = "#${Integer.toHexString(info.color).uppercase()}"
            binding.filamentDiameterText.text = "${info.filamentDiameter} mm"
            binding.filamentLengthText.text = "${info.filamentLength} 米"
            binding.spoolWeightText.text = "${info.spoolWeight} g"
            binding.spoolWidthText.text = "${info.spoolWidth} mm"
            binding.bedTemperatureText.text = "${info.bedTemperature} ℃"
            binding.minimumNozzleDiameterText.text = "${info.minimumNozzleDiameter} mm"
            binding.temperatureHotendText.text = "${info.minTemperatureHotend} ℃ - ${info.maxTemperatureHotend} ℃"
            binding.dryingTemperatureText.text = "${info.dryingTemperature} ℃"
            binding.dryingHoursText.text = "${info.dryingHours} H"
            binding.trayUIDText.text = info.trayUID
            binding.filamentColorIndicator.setBackgroundColor(info.color)
        }
    }
}