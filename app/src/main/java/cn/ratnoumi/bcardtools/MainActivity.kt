package cn.ratnoumi.bcardtools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import cn.ratnoumi.bcardtools.databinding.ActivityMainBinding
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.bambu.bambuKdf
import cn.ratnoumi.bcardtools.drive.bambu.getBambuFilament
import cn.ratnoumi.bcardtools.drive.mifare.MifareCard
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyA
import cn.ratnoumi.bcardtools.drive.mifare.findMifareSectorKeyB
import cn.ratnoumi.bcardtools.drive.mifare.readMifareSector
import cn.ratnoumi.bcardtools.utils.FileExporter
import cn.ratnoumi.bcardtools.dao.BambuFilamentDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : BaseNfcAppCompatActivity() {
    private val cards = mutableListOf<BambuFilamentCard>()
    private lateinit var cardItemAdapter: CardItemAdapter
    private lateinit var binding: ActivityMainBinding

    lateinit var bambuFilamentDao: BambuFilamentDao

    private val filteredCards = mutableListOf<BambuFilamentCard>() // 用于搜索和筛选的列表
    private var currentCategory: String? = null // 当前选中的分类
    private lateinit var searchText: String // 当前搜索文本


    // 新增：分类标签点击事件
    private val onCategoryClick = View.OnClickListener { view ->
        // 重置所有标签样式
        binding.categoryContainer.children.forEach {
            it.setBackgroundResource(R.drawable.category_unselected_bg)
            (it as TextView).setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            (it as TextView).setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        }

        // 设置当前标签样式
        view.setBackgroundResource(R.drawable.category_selected_bg)
        (view as TextView).setTextColor(resources.getColor(android.R.color.white, theme))

        // 更新当前分类并筛选
        currentCategory = view.tag as String
        filterCards()
    }

    // 新增：初始化搜索框
    private fun initSearchView() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s.toString().trim()
                filterCards()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 新增：筛选卡片列表
    @SuppressLint("NotifyDataSetChanged")
    private fun filterCards() {
        filteredCards.clear()

        val result = cards.filter { card ->
            // 分类筛选
            val categoryMatch = currentCategory == null || currentCategory == "全部" ||
                    card.detailedFilamentType == currentCategory

            // 搜索筛选
            val searchMatch = searchText.isEmpty() ||
                    card.detailedFilamentType.contains(searchText, ignoreCase = true) ||
                    card.uid.contains(searchText, ignoreCase = true) ||
                    card.materialID.contains(searchText, ignoreCase = true) ||
                    // 新增：颜色筛选（文本匹配）
                    Integer.toHexString(card.color).contains(searchText, ignoreCase = true) ||
                    // 新增：耗材重量筛选（数值转字符串后匹配，支持搜索重量数字）
                    card.spoolWeight.toString().contains(searchText, ignoreCase = true) ||
                    card.filamentLength.toString().contains(searchText, ignoreCase = true) ||
                    card.productionDate.contains(searchText, ignoreCase = true)
            categoryMatch && searchMatch
        }

        filteredCards.addAll(result)
        cardItemAdapter.notifyDataSetChanged()
        binding.nullView.visibility = if (filteredCards.isEmpty()) View.VISIBLE else View.GONE
    }

    // 重写：更新列表并生成分类
    @SuppressLint("NotifyDataSetChanged")
    fun updateList() {
        cards.clear()
        for (filament in bambuFilamentDao.findAll()) {
            filament?.let { cards.add(it) }
        }

        // 生成分类标签
        generateCategories()

        // 重置筛选条件并刷新列表
        currentCategory = "全部"
        searchText = ""
        binding.searchEditText.setText("")
        filterCards()
    }

    // 新增：生成分类标签
    private fun generateCategories() {
        binding.categoryContainer.removeAllViews()

        // 收集所有不重复的耗材类型
        val categories = mutableSetOf<String>()
        categories.add("全部") // 添加"全部"选项
        cards.forEach { categories.add(it.detailedFilamentType) }

        // 动态创建分类标签
        categories.forEach { category ->
            val textView = TextView(this).apply {
                text = category
                textSize = 13f
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                setBackgroundResource(if (category == "全部") R.drawable.category_selected_bg else R.drawable.category_unselected_bg)
                setTextColor(
                    if (category == "全部")
                        resources.getColor(android.R.color.white, theme)
                    else
                        resources.getColor(android.R.color.darker_gray, theme)
                )
                tag = category // 存储分类值
                setOnClickListener(onCategoryClick)
                setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
            }
            binding.categoryContainer.addView(textView)
        }
    }

    // 新增：扩展函数，dp转px
    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    // 新增：设置视图 margins
    private fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(left, top, right, bottom)
        }
        this.layoutParams = params
    }


    // 检查是否需要请求权限
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // API 28及以下
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val needRequest = permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (needRequest) {
                // 有未授予的权限，发起申请
                requestMultiplePermissions.launch(permissions)
                return false
            } else {
                exportFilesToDownload()
                return true
            }
        } else {
            exportFilesToDownload()
            true // API 29+ 无需权限
        }
    }

    // 2. 注册多个权限申请的启动器
    val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            // 3. 处理权限申请结果（Map 中 key 是权限名，value 是是否授予）
            val allGranted = permissions.all { it.value } // 检查是否所有权限都被授予
            if (allGranted) {
                exportFilesToDownload()
            } else {
                // 部分或全部权限被拒绝，提示用户
                val deniedPermissions = permissions.filter { !it.value }.keys
                Toast.makeText(
                    this,
                    "需要以下权限：${deniedPermissions.joinToString()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    fun exportFilesToDownload() {
        CoroutineScope(Dispatchers.Main).launch {
            // 定义格式化器，指定Locale.US
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
            val currentTime = LocalDateTime.now().format(formatter)
            val path = Paths.get("BCardTools/${currentTime}/")
            // 要导出的文件列表（示例）
            val filesToExport = createExportFileList(path)

            val success = FileExporter.exportFilesToDownload(baseContext, filesToExport)
            if (success) {
                Toast.makeText(
                    baseContext,
                    "文件已导出到：Download/$path",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(baseContext, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(this, "正在导出数据", Toast.LENGTH_SHORT).show()
    }

    fun createExportFileList(subDir: Path): MutableList<Pair<Path, ByteArray>> {
        val fileList: MutableList<Pair<Path, ByteArray>> = mutableListOf()
        bambuFilamentDao.findAll().forEach { bambuCard ->
            bambuCard?.let {
                val fileName = "${it.detailedFilamentType}-#${Integer.toHexString(it.color).uppercase()}-${it.uid}"
                fileList.add(Paths.get(subDir.toString(), "bin/${fileName}-dump.bin") to getBinBytes(it))
                fileList.add(Paths.get(subDir.toString(), "mct/${fileName}-dump.mct") to getMctBytes(it))
                fileList.add(Paths.get(subDir.toString(), "keys/${fileName}.keys") to getKeysBytes(it))
            }
        }
        return fileList
    }

    fun getBinBytes(bambuCard: BambuFilamentCard): ByteArray {
        val buffer = ByteBuffer.allocate(1024)
        bambuCard.card.blocks.forEach {
            buffer.put(it)
        }
        return buffer.array()
    }

    fun getMctBytes(bambuCard: BambuFilamentCard): ByteArray {
        val buffer = StringBuffer()
        for (sectorIndex in 0..<bambuCard.card.getSectorCount()) {
            buffer.append("+Sector: ${sectorIndex}\n")
            val sectorStart = bambuCard.card.sectorToBlock(sectorIndex)
            for (i in 0..<bambuCard.card.getBlockCountInSector(sectorIndex)) {
                buffer.append("${bambuCard.card.blocks[sectorStart + i].toHexString().uppercase()}\n")
            }
        }
        return buffer.toString().toByteArray()
    }

    fun getKeysBytes(bambuCard: BambuFilamentCard): ByteArray {
        val keys = mutableSetOf<String>()
        for (sectorIndex in 0..<bambuCard.card.getSectorCount()) {
            keys.add(bambuCard.card.getKeyA(sectorIndex).toHexString().uppercase())
            keys.add(bambuCard.card.getKeyB(sectorIndex).toHexString().uppercase())
        }
        return keys.joinToString("\n").toByteArray()
    }

    // 卡片详情页面返回处理
    private val requestCardDetail =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Toast.makeText(this, "刷新数据", Toast.LENGTH_SHORT).show()
            updateList()
        }

    // 跳转到卡片详情页面
    fun startActivityCardDetail(card: BambuFilamentCard) {
        val intent = Intent(this, CardDetailActivity::class.java)
        intent.putExtra("card", card)
        requestCardDetail.launch(intent)
    }

    // 跳转到卡片详情页面
    fun startActivityAbout() {
        val intent = Intent(this, AboutActivity::class.java)
        requestCardDetail.launch(intent)
    }

    override fun processTag(intent: Intent?) {
        val tag: Tag?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let {
            binding.loadingText.text = "正在读取NFC"
            binding.loadingView.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val classic = MifareClassic.get(tag)
                    val keys = bambuKdf(classic.tag.id)
                    // 连接卡片
                    if (!classic.isConnected) {
                        classic.connect()
                    }
                    //
                    val card = MifareCard(classic.size)
                    for (sectorIndex in 0..<classic.sectorCount) {
                        withContext(Dispatchers.Main) {
                            binding.loadingText.text =
                                "正在读取卡片: 第${sectorIndex}扇区,共${classic.sectorCount}扇区"
                        }
                        val keyA = findMifareSectorKeyA(classic, sectorIndex, keys)
                        val keyB = findMifareSectorKeyB(classic, sectorIndex, keys)
                        readMifareSector(card, classic, sectorIndex, keyA, keyB)
                    }
                    startActivityCardDetail(getBambuFilament(card))
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(baseContext, "非某竹卡片,读取失败!", Toast.LENGTH_SHORT).show()
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.loadingView.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        // 初始化搜索框
        initSearchView()

        val sp = getSharedPreferences("app_config", Context.MODE_PRIVATE)
        if (!sp.getBoolean("about_skip", false)) {
            startActivityAbout()
        }
        bambuFilamentDao = BambuFilamentDao(baseContext)

        // 初始化适配器，使用筛选后的列表
        cardItemAdapter = CardItemAdapter(
            filteredCards,
            onItemClick = { startActivityCardDetail(it) },
            onDelete = {
                bambuFilamentDao.delete(it.uid)
                updateList()
            })

        updateList()
        binding.cardRecyclerView.adapter = cardItemAdapter
        binding.cardRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.toolbar.setOnClickListener {
            updateList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_refresh -> {
                updateList()
                true
            }

            R.id.action_about -> {
                startActivityAbout()
                true
            }

            R.id.action_import -> {
                selectCardFile()
                true
            }

            R.id.action_export -> {
                checkStoragePermission()
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    // 选择固件文件 - 支持多选
    private fun selectCardFile() {
        val intent: Intent
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                // 添加多选支持
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        } else {
            intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "application/octet-stream", 
                        "application/bin", 
                        "application/mct", 
                        "*/mct", 
                        "*/bin",
                        "application/zip",
                        "application/x-zip-compressed"
                    )
                ) // 优先二进制文件
                // 添加多选支持
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        requestSelectCardFile.launch(Intent.createChooser(intent, "选择固件文件（.bin）"))
    }

    // 初始化文件选择回调（在 onCreate 中）
    private val requestSelectCardFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    // 存储选中的所有文件 Uri
                    val selectedUris = mutableListOf<Uri>()
                    // 处理多选情况（通过 ClipData 获取多个 Uri）
                    if (data.clipData != null) {
                        val clipData = data.clipData!!
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            selectedUris.add(uri)
                        }
                    }
                    // 处理单选情况
                    else if (data.data != null) {
                        val uri = data.data!!
                        selectedUris.add(uri)
                    }
                    // 打印选中的文件数量和 Uri（根据需求处理）
                    Log.d("FileSelect", "选中 ${selectedUris.size} 个文件")
                    selectedUris.forEach { uri ->
                        // 在这里处理每个文件（如获取路径、读取内容等）
                        contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                            null,
                            null
                        )
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    // 1. 获取列索引，并检查有效性
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                                    // 2. 只有索引有效时才获取值，否则用默认值或提示
                                    val binName = if (nameIndex != -1) {
                                        cursor.getString(nameIndex)
                                    } else {
                                        Log.w("FileSelect", "未找到 DISPLAY_NAME 列")
                                        "unknown_name" // 默认名称
                                    }

                                    val binLength = if (sizeIndex != -1) {
                                        cursor.getLong(sizeIndex)
                                    } else {
                                        Log.w("FileSelect", "未找到 SIZE 列")
                                        -1L // 标记为无效大小
                                    }
//                                    val binName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
//                                    val binLength = it.getLong(it.getColumnIndex(OpenableColumns.SIZE))
                                    Log.d("FileSelect", "文件名称: ${binName} 文件大小: ${binLength} 文件Uri: $uri")
                                    if (binName.endsWith(".bin")) {
                                        if (binLength != 1024.toLong()) {
                                            Toast.makeText(
                                                baseContext,
                                                "bin文件大小不为1024字节!",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        } else {
                                            handeBinCardFile(binName, binLength, uri)
                                        }
                                    } else if (binName.endsWith(".mct")) {
                                        handeMctCardFile(binName, binLength, uri)
                                    } else if (binName.endsWith(".zip")) {
                                        handleZipFile(uri)
                                    } else {
                                        Toast.makeText(baseContext, "不支持的文件类型!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                    }
                }
            }
        }

    fun handeBinCardFile(fileName: String, size: Long, uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            val output = ByteArrayOutputStream()
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            processBinData(output.toByteArray())
        }
    }

    private fun processBinData(binBytes: ByteArray) {
        try {
            val card = MifareCard(binBytes.size)
            for (blockIndex in 0..<card.getBlockCount()) {
                // Determine block size based on card type or assume standard if not clear,
                // but here we just slice the array.
                // Safety check to avoid index out of bounds if file is truncated
                val start = blockIndex * MifareClassic.BLOCK_SIZE
                val end = (blockIndex + 1) * MifareClassic.BLOCK_SIZE
                if (end <= binBytes.size) {
                    card.blocks.add(
                        blockIndex,
                        binBytes.copyOfRange(start, end)
                    )
                }
            }
            val bambuCard = getBambuFilament(card)
            addBambuFilament(bambuCard)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(baseContext, "BIN文件解析失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun handeMctCardFile(fileName: String, size: Long, uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            processMctStream(inputStream)
        }
    }

    private fun processMctStream(inputStream: java.io.InputStream) {
        try {
            val card = MifareCard(MifareClassic.SIZE_1K)
            // Use CloseShieldInputStream if we were using a library that closes it,
            // but here we just wrap it. Responsibility to close prompt stream lies with caller
            // IF caller opened it. But BufferedReader closes underlying stream.
            // For ZIP, wrapping ZipInputStream in BufferedReader and closing BufferedReader will close ZipInputStream!
            // So we must NOT close the reader if it comes from ZipInputStream.
            // SOLUTION: for ZIP, we read entry to bytes/string first, then parse.
            
            // For consistency and safety with ZipInputStream, let's read everything to String first.
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines() // Read all lines
            
            var sectorIndex = 0
            var blockIndex = 0
            
            for (line in lines) {
                Log.d("FileContent", "行内容: $line")
                if (line.startsWith("+Sector: ")) {
                    sectorIndex = line.substring(9).toInt()
                    blockIndex = card.sectorToBlock(sectorIndex)
                } else if (line.length >= 32) { // Basic validation for hex line
                     try {
                        card.blocks.add(blockIndex++, hexToByteArray(line))
                     } catch (e: Exception) {
                         // Ignore invalid lines (headers etc)
                     }
                }
            }
            val bambuCard = getBambuFilament(card)
            addBambuFilament(bambuCard)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(baseContext, "MCT文件解析失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun addBambuFilament(bambuCard: BambuFilamentCard) {
        val keys = bambuKdf(bambuCard.card.getId())
        for (i in 0..<keys.size) {
            if (!keys[i].contentEquals(bambuCard.card.getKeyA(i))) {
                Toast.makeText(baseContext, "非某竹卡片,导入失败!", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (!bambuFilamentDao.exist(bambuCard.uid)) {
            bambuFilamentDao.add(bambuCard)
        }
        updateList()
    }

    fun handleZipFile(uri: Uri) {
        Thread {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.util.zip.ZipInputStream(inputStream).use { zipInputStream ->
                        var entry = zipInputStream.nextEntry
                        var successCount = 0
                        var failCount = 0
                        
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val name = entry.name
                                Log.d("ZipImport", "Processing entry: $name")
                                
                                // Read entry content to byte array to avoid closing the ZipInputStream via wrappers
                                val buffer = ByteArrayOutputStream()
                                val data = ByteArray(1024)
                                var count: Int
                                while (zipInputStream.read(data).also { count = it } != -1) {
                                    buffer.write(data, 0, count)
                                }
                                val bytes = buffer.toByteArray()
                                
                                runCatching {
                                    if (name.endsWith(".bin", ignoreCase = true)) {
                                        if (bytes.size.toLong() == 1024L || bytes.size.toLong() == 4096L) { // Basic size check
                                             processBinData(bytes)
                                             successCount++
                                        }
                                    } else if (name.endsWith(".mct", ignoreCase = true)) {
                                        // Wrap bytes in stream for Mct processor
                                        processMctStream(java.io.ByteArrayInputStream(bytes))
                                        successCount++
                                    }
                                }.onFailure {
                                    failCount++
                                    it.printStackTrace()
                                }
                            }
                            zipInputStream.closeEntry()
                            entry = zipInputStream.nextEntry
                        }
                        
                        runOnUiThread {
                             Toast.makeText(baseContext, "ZIP导入完成: 成功 $successCount, 失败 $failCount", Toast.LENGTH_LONG).show()
                             updateList()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(baseContext, "ZIP读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    /**
     * 将十六进制字符串转换为ByteArray
     * @param hex 十六进制字符串（支持大小写，长度必须为偶数）
     * @return 转换后的ByteArray
     */
    fun hexToByteArray(hex: String): ByteArray {
        // 校验：十六进制字符串长度必须为偶数（2个字符对应1个字节）
        require(hex.length % 2 == 0) { "十六进制字符串长度必须为偶数" }

        return hex.chunked(2) // 按每2个字符分割（如"53C9" → ["53", "C9"]）
            .map { chunk ->
                // 将每个2字符子串解析为16进制整数，再转为Byte
                chunk.toInt(16).toByte()
            }
            .toByteArray()
    }
}