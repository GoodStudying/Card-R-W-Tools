# 错误记录：Kotlin编译失败

## 错误类型：Kotlin编译错误

### 错误信息
```
> Task :app:compileReleaseKotlin FAILED
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:18:43 Unresolved reference 'BambuUtils'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:26:1 Class 'BatchBurnActivity' is not abstract and does not implement abstract base class member:
fun processTag(intent: Intent?): Unit
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:28:44 Unresolved reference 'BambuFilamentDao'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:42:28 Unresolved reference 'BambuFilamentDao'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:48:38 Unresolved reference 'getAll'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:102:5 'processTag' overrides nothing. Potential signatures for overriding:
fun processTag(intent: Intent?): Unit
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:102:37 Unresolved reference 'Intent'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:107:27 Unresolved reference 'getParcelableExtra'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:110:27 Unresolved reference 'getParcelableExtra'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:113:14 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:115:44 Unresolved reference 'launch'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:119:33 Unresolved reference 'BambuUtils'.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:143:21 Suspend function 'suspend fun <T> withContext(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T' can only be called from a coroutine or another suspend function.
e: file:///home/runner/work/Card-R-W-Tools/Card-R-W-Tools/app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt:150:21 Suspend function 'suspend fun <T> withContext(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T' can only be called from a coroutine or another suspend function.
```

### 错误原因
- **文件**：`app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt`
- **问题**：
  1. 缺少必要的导入语句
  2. 未正确实现抽象方法
  3. 协程相关的错误

### 解决方案
1. **添加必要的导入语句**：
   - `import android.content.Intent`
   - `import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentDao`
   - `import cn.ratnoumi.bcardtools.drive.bambu.BambuUtils`

2. **确保正确实现抽象方法**：
   - 确保 `processTag` 方法的签名与父类一致

3. **修复协程相关问题**：
   - 确保 `withContext` 调用在协程作用域内

### 正确的代码示例
```kotlin
package cn.ratnoumi.bcardtools

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import cn.ratnoumi.bcardtools.databinding.ActivityBatchBurnBinding
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentDao
import cn.ratnoumi.bcardtools.drive.bambu.BambuUtils
import cn.ratnoumi.bcardtools.drive.mifare.defaultKeys
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyA
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val cards = bambuFilamentDao.getAll()
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

        // 等待用户刷卡
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
                    keys.addAll(BambuUtils.bambuKdf(classic.tag.id))
                    keys.addAll(defaultKeys)

                    if (!classic.isConnected) {
                        classic.connect()
                    }

                    // 写入卡片数据
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
                        // 继续烧写下一张卡片
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
```

### 预防措施
1. **导入检查**：确保所有使用的类和方法都有正确的导入语句
2. **抽象方法实现**：确保正确实现所有抽象方法，签名与父类一致
3. **协程使用**：确保在协程作用域内调用挂起函数
4. **代码审查**：在提交前检查代码的语法和逻辑正确性
5. **构建测试**：在提交前运行构建命令，确保没有编译错误

### 相关文件
- `app/src/main/java/cn/ratnoumi/bcardtools/BatchBurnActivity.kt`

### 修复日期
2026-03-05