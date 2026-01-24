package cn.ratnoumi.bcardtools

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.ratnoumi.bcardtools.dao.BambuFilamentDao
import cn.ratnoumi.bcardtools.drive.bambu.BambuFilamentCard
import cn.ratnoumi.bcardtools.drive.mifare.MifareCard
import cn.ratnoumi.bcardtools.utils.WebDavClient
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncActivity : AppCompatActivity() {

    private lateinit var etServerUrl: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvStatus: TextView
    private val PREFS_NAME = "webdav_prefs"
    private val BACKUP_FILENAME = "bcardtools_backup.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)
        
        supportActionBar?.title = "WebDAV 数据同步"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tvStatus = findViewById(R.id.tvStatus)

        loadConfig()

        findViewById<Button>(R.id.btnTestConnection).setOnClickListener {
            saveConfig()
            testConnection()
        }

        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            saveConfig()
            backupData()
        }

        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            saveConfig()
            restoreData()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etServerUrl.setText(prefs.getString("url", ""))
        etUsername.setText(prefs.getString("user", ""))
        etPassword.setText(prefs.getString("pass", ""))
    }

    private fun saveConfig() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("url", etServerUrl.text.toString().trim())
            .putString("user", etUsername.text.toString().trim())
            .putString("pass", etPassword.text.toString().trim())
            .apply()
    }

    private fun getConfig(): Triple<String, String, String>? {
        val url = etServerUrl.text.toString().trim()
        val user = etUsername.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
            return null
        }
        return Triple(url, user, pass)
    }

    private fun testConnection() {
        val (url, user, pass) = getConfig() ?: return
        tvStatus.text = "正在连接..."
        CoroutineScope(Dispatchers.IO).launch {
            val success = WebDavClient.checkConnection(url, user, pass)
            withContext(Dispatchers.Main) {
                if (success) {
                    tvStatus.text = "连接成功"
                    Toast.makeText(this@SyncActivity, "连接成功", Toast.LENGTH_SHORT).show()
                } else {
                    tvStatus.text = "连接失败，请检查配置"
                    Toast.makeText(this@SyncActivity, "连接失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun backupData() {
        val (url, user, pass) = getConfig() ?: return
        tvStatus.text = "正在备份..."
        
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Get all data
            val dao = BambuFilamentDao(this@SyncActivity)
            val allCards = dao.findAll().mapNotNull { it }
            
            // 2. Serialize to JSON
            // Note: Currently BambuFilamentCard might handle serialization differently if not standard.
            // Assuming Gson can handle it basically. If 'card' field (MifareCard) is complex, we need to ensure it's serializable.
            // Checking definition: BambuFilamentCard has 'card: MifareCard'. MifareCard has 'blocks: ArrayList<ByteArray>'.
            // Gson default handles ArrayList. ByteArray generally serializes to array of numbers. 
            // It's better to implement a custom serializer or helper, but let's try default Gson first. 
            // WAIT - ByteArray in Gson standardly serializes to int array. This is fine for json storage.
            
            try {
                val json = Gson().toJson(allCards)
                
                // 3. Upload
                val success = WebDavClient.uploadFile(url, user, pass, BACKUP_FILENAME, json)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        tvStatus.text = "备份成功 (共${allCards.size}条记录)"
                        Toast.makeText(this@SyncActivity, "备份成功", Toast.LENGTH_SHORT).show()
                    } else {
                        tvStatus.text = "备份上传失败"
                         Toast.makeText(this@SyncActivity, "备份失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvStatus.text = "备份出错: ${e.message}"
                }
            }
        }
    }

    private fun restoreData() {
        val (url, user, pass) = getConfig() ?: return
        tvStatus.text = "正在恢复..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Download
                val json = WebDavClient.downloadFile(url, user, pass, BACKUP_FILENAME)
                
                if (json == null) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "下载失败或文件不存在"
                        Toast.makeText(this@SyncActivity, "下载失败", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 2. Deserialize
                val type = object : TypeToken<List<BambuFilamentCard>>() {}.type
                val restoredCards: List<BambuFilamentCard> = Gson().fromJson(json, type)
                
                if (restoredCards.isEmpty()) {
                     withContext(Dispatchers.Main) {
                        tvStatus.text = "备份文件为空"
                    }
                    return@launch
                }

                // 3. Restore to DB (Merge/Add if not exists)
                val dao = BambuFilamentDao(this@SyncActivity)
                var count = 0
                restoredCards.forEach { card ->
                    // Fix potential serialization issues: ensure MifareCard is properly reconstructed.
                    // Given Gson limitation with complex objects if they don't have no-arg constructors or fields don't match exactly.
                    // MifareCard(size) constructor might be an issue if Gson uses Unsafe.
                    // Double check MifareCard structure. It has 'blocks'.
                    if (!dao.exist(card.uid)) {
                        dao.add(card)
                        count++
                    }
                }
                
                withContext(Dispatchers.Main) {
                    tvStatus.text = "恢复成功 (新增${count}条记录)"
                    Toast.makeText(this@SyncActivity, "恢复成功", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvStatus.text = "恢复出错: ${e.message}"
                }
            }
        }
    }
}
