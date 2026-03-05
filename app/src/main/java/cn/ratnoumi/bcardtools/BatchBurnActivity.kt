package cn.ratnoumi.bcardtools

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import cn.ratnoumi.bcardtools.databinding.ActivityBatchBurnBinding
import cn.ratnoumi.bcardtools.dao.BambuFilamentDao
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.bambu.bambuKdf
import cn.ratnoumi.bcardtools.drive.mifare.defaultKeys
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyA
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BatchBurnActivity : BaseNfcAppCompatActivity() {
    private lateinit var binding: ActivityBatchBurnBinding
    private lateinit var bambuFilamentDao: BambuFilamentDao
    private lateinit var cardAdapter: BatchBurnCardAdapter
    private var selectedCards = mutableListOf<BambuFilamentCard>()
    private var currentBurnIndex = 0
    private var isBurning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatchBurnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bambuFilamentDao = BambuFilamentDao(this)
        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        val cards = bambuFilamentDao.findAll().filterNotNull()
        cardAdapter = BatchBurnCardAdapter(cards) {
            updateSelectedCards()
        }
        binding.cardList.adapter = cardAdapter
    }

    private fun setupButtons() {
        binding.selectAllBtn.setOnClickListener {
            cardAdapter.toggleSelectAll()
            updateSelectedCards()
        }

        binding.startBurnBtn.setOnClickListener {
            if (selectedCards.isEmpty()) {
                Toast.makeText(this, "请选择要烧写的卡片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startBatchBurn()
        }

        binding.importClipboardBtn.setOnClickListener {
            importFromClipboard()
        }
    }

    private fun updateSelectedCards() {
        selectedCards = cardAdapter.getSelectedCards()
        binding.statusText.text = "已选择 ${selectedCards.size} 张卡片"
    }

    private fun startBatchBurn() {
        if (selectedCards.isEmpty()) return

        isBurning = true
        currentBurnIndex = 0
        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "准备开始烧写..."
        binding.startBurnBtn.isEnabled = false
        binding.selectAllBtn.isEnabled = false

        burnNextCard()
    }

    private fun burnNextCard() {
        if (currentBurnIndex >= selectedCards.size) {
            finishBatchBurn()
            return
        }

        val currentCard = selectedCards[currentBurnIndex]
        binding.statusText.text = "正在烧写第 ${currentBurnIndex + 1} 张卡片：${currentCard.filamentType} - ${currentCard.colorName}"

        Toast.makeText(this, "请刷入新卡片进行烧写", Toast.LENGTH_LONG).show()
    }

    override fun processTag(intent: Intent?) {
        if (!isBurning || currentBurnIndex >= selectedCards.size) return

        val tag: Tag?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        tag?.let {
            val currentCard = selectedCards[currentBurnIndex]
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val classic = MifareClassic.get(tag)
                    val keys = mutableListOf<ByteArray>()
                    keys.addAll(bambuKdf(classic.tag.id))
                    keys.addAll(defaultKeys)

                    if (!classic.isConnected) {
                        classic.connect()
                    }

                    for (sectorIndex in 0 until classic.sectorCount) {
                        val keyA = findMifareSectorKeyA(classic, sectorIndex, keys)
                        val keyB = findMifareSectorKeyB(classic, sectorIndex, keys)

                        if (keyA != null) {
                            val blockCount = classic.getBlockCountInSector(sectorIndex)
                            for (blockOffset in 0 until blockCount) {
                                val blockIndex = classic.sectorToBlock(sectorIndex) + blockOffset
                                if (blockIndex < currentCard.card.blocks.size) {
                                    classic.authenticateSectorWithKeyA(sectorIndex, keyA)
                                    classic.writeBlock(blockIndex, currentCard.card.blocks[blockIndex])
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BatchBurnActivity, "烧写成功！", Toast.LENGTH_SHORT).show()
                        currentBurnIndex++
                        burnNextCard()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BatchBurnActivity, "烧写失败：${e.message}", Toast.LENGTH_SHORT).show()
                        currentBurnIndex++
                        burnNextCard()
                    }
                }
            }
        }
    }

    private fun finishBatchBurn() {
        isBurning = false
        binding.progressBar.visibility = View.GONE
        binding.statusText.text = "批量烧写完成！"
        binding.startBurnBtn.isEnabled = true
        binding.selectAllBtn.isEnabled = true
        Toast.makeText(this, "所有卡片烧写完成", Toast.LENGTH_LONG).show()
    }

    private fun importFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            return
        }
        val text = clip.getItemAt(0).text?.toString() ?: ""
        if (text.isBlank()) {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            return
        }
        parseAndImportList(text)
    }

    private fun parseAndImportList(text: String) {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            Toast.makeText(this, "没有有效数据", Toast.LENGTH_SHORT).show()
            return
        }
        val colorMap = getColorMap()
        var successCount = 0
        var skipCount = 0
        for (line in lines) {
            val parts = line.split(Regex("[,\t\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 3) {
                skipCount++
                continue
            }
            val filamentType = parts[0]
            val colorName = parts[1]
            val colorHex = parts[2]
            val color = parseColor(colorHex)
            val matchedColorName = colorMap[filamentType to color] ?: colorName
            val cardData = createPlaceholderCard(filamentType, matchedColorName, color)
            if (!bambuFilamentDao.exist(cardData.uid)) {
                bambuFilamentDao.add(cardData)
                successCount++
            } else {
                skipCount++
            }
        }
        setupRecyclerView()
        Toast.makeText(this, "导入成功: $successCount, 跳过: $skipCount", Toast.LENGTH_LONG).show()
    }

    private fun getColorMap(): Map<Pair<String, Int>, String> {
        val map = mutableMapOf<Pair<String, Int>, String>()
        try {
            val inputStream = assets.open("color_map.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val type = parts[0].trim()
                    val name = parts[1].trim()
                    val hex = parts[2].trim().removePrefix("#")
                    val color = parseColor(hex)
                    map[type to color] = name
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun parseColor(hex: String): Int {
        return try {
            val cleanHex = hex.removePrefix("#")
            when (cleanHex.length) {
                6 -> Integer.parseInt(cleanHex, 16) or 0xFF000000.toInt()
                8 -> Integer.parseInt(cleanHex, 16)
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun createPlaceholderCard(filamentType: String, colorName: String, color: Int): BambuFilamentCard {
        val uid = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val materialID = when {
            filamentType.contains("PLA", ignoreCase = true) -> "GFA01"
            filamentType.contains("PETG", ignoreCase = true) -> "GFG01"
            filamentType.contains("ABS", ignoreCase = true) -> "GFB01"
            filamentType.contains("TPU", ignoreCase = true) -> "GFT01"
            filamentType.contains("ASA", ignoreCase = true) -> "GFS01"
            filamentType.contains("PA", ignoreCase = true) -> "GFK01"
            filamentType.contains("PC", ignoreCase = true) -> "GFZ01"
            else -> "GFA01"
        }
        val mifareCard = cn.ratnoumi.bcardtools.drive.mifare.MifareCard(MifareClassic.SIZE_1K)
        return BambuFilamentCard(
            uid = uid,
            materialVariantID = "",
            materialID = materialID,
            filamentType = filamentType.substringBefore(" ").ifEmpty { "PLA" },
            detailedFilamentType = filamentType,
            color = color,
            colorName = colorName,
            spoolWeight = 1000,
            filamentDiameter = 1.75f,
            dryingTemperature = 55,
            dryingHours = 8,
            bedTemperature = 45,
            maxTemperatureHotend = 230,
            minTemperatureHotend = 200,
            xCamInfo = "",
            minimumNozzleDiameter = 0.4f,
            trayUID = "",
            spoolWidth = 0f,
            productionDate = dateStr,
            filamentLength = 0,
            card = mifareCard
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class BatchBurnCardAdapter(
    private var cards: List<BambuFilamentCard>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<BatchBurnCardAdapter.ViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_burn_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = cards[position]
        holder.filamentTypeText.text = card.detailedFilamentType
        holder.colorNameText.text = card.colorName
        holder.uidText.text = "UID: ${card.uid}"
        holder.colorIndicator.setBackgroundColor(card.color)
        holder.checkBox.isChecked = selectedPositions.contains(position)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPositions.add(position)
            } else {
                selectedPositions.remove(position)
            }
            onSelectionChanged()
        }
    }

    override fun getItemCount(): Int = cards.size

    fun toggleSelectAll() {
        if (selectedPositions.size == cards.size) {
            selectedPositions.clear()
        } else {
            selectedPositions.clear()
            for (i in cards.indices) {
                selectedPositions.add(i)
            }
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun getSelectedCards(): MutableList<BambuFilamentCard> {
        return selectedPositions.map { cards[it] }.toMutableList()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.selectCheckBox)
        val filamentTypeText: TextView = itemView.findViewById(R.id.filamentTypeText)
        val colorNameText: TextView = itemView.findViewById(R.id.colorNameText)
        val uidText: TextView = itemView.findViewById(R.id.uidText)
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
    }
}