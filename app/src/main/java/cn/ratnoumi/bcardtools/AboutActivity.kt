package cn.ratnoumi.bcardtools

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.ratnoumi.bcardtools.databinding.ActivityAboutBinding
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.appcompat.app.AlertDialog


class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.okBtn.setOnClickListener {
            val sp = getSharedPreferences("app_config", Context.MODE_PRIVATE)
            sp.edit().putBoolean("about_skip", true).apply()
            finish()
        }
        
        binding.githubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GoodStudying/Bambu-Card-Tools"))
            startActivity(intent)
        }
        
        checkForUpdates()
    }

    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/GoodStudying/Bambu-Card-Tools/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val json = JSONObject(response.toString())
                    val tagName = json.getString("tag_name") // e.g., v1.1.0
                    val htmlUrl = json.getString("html_url")
                    
                    // 获取当前版本
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    val currentVersion = packageInfo.versionName ?: ""
                    
                    if (isNewerVersion(tagName, currentVersion)) {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("发现新版本")
                                .setMessage("最新版本: $tagName\n当前版本: $currentVersion\n是否前往 GitHub 下载？")
                                .setPositiveButton("前往下载") { _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl))
                                    startActivity(intent)
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            val v1 = remote.removePrefix("v").split(".")
            val v2 = local.removePrefix("v").split(".")
            
            val length = maxOf(v1.size, v2.size)
            for (i in 0 until length) {
                val num1 = if (i < v1.size) v1[i].toInt() else 0
                val num2 = if (i < v2.size) v2[i].toInt() else 0
                if (num1 > num2) return true
                if (num1 < num2) return false
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}