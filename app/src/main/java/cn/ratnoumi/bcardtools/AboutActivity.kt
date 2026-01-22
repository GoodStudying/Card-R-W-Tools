package cn.ratnoumi.bcardtools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import cn.ratnoumi.bcardtools.databinding.ActivityAboutBinding
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
            // 这里虽然链接名字叫 githubLink，但实际跳转可以根据需求改，暂时保留原 GitHub 项目主页，或者改为 Gitee
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GoodStudying/Bambu-Card-Tools"))
            startActivity(intent)
        }
        
        binding.checkUpdateBtn.setOnClickListener {
            checkForUpdates(isManual = true)
        }
        
        // Auto check (silent)
        checkForUpdates(isManual = false)

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionText.text = "Version: ${packageInfo.versionName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkForUpdates(isManual: Boolean) {
        if (isManual) {
            Toast.makeText(this, "正在检查更新...", Toast.LENGTH_SHORT).show()
        }
        Thread {
            try {
                // 使用 Gitee API
                val url = URL("https://gitee.com/api/v5/repos/GoodStudying/Bambu-Card-Tools/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
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
                    val htmlUrl = json.getString("html_url") // Gitee release page
                    
                    // 解析下载链接
                    var downloadUrl = ""
                    if (json.has("assets")) {
                        val assets = json.getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            // 优先下载 release.apk 或者带 release 字样的 apk
                            if (name.endsWith(".apk", true)) {
                                downloadUrl = asset.getString("browser_download_url")
                                if (name.contains("release", true)) {
                                    break // 找到 release 包，优先使用
                                }
                            }
                        }
                    }

                    // 获取当前版本
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    val currentVersion = packageInfo.versionName ?: ""
                    
                    if (isNewerVersion(tagName, currentVersion)) {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("发现新版本 $tagName")
                                .setMessage("当前版本: $currentVersion\n最新版本: $tagName\n\n是否下载安装？")
                                .setPositiveButton("立即更新") { _, _ ->
                                    if (downloadUrl.isNotEmpty()) {
                                        downloadApk(downloadUrl)
                                    } else {
                                        // 没找到 apk 下载链接，回退到网页跳转
                                        Toast.makeText(this, "未找到安装包，即将前往网页下载", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl))
                                        startActivity(intent)
                                    }
                                }
                                .setNegativeButton("稍后", null)
                                .show()
                        }
                    } else if (isManual) {
                        runOnUiThread {
                            Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (isManual) {
                     runOnUiThread {
                        Toast.makeText(this, "检查失败: HTTP ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isManual) {
                    runOnUiThread {
                        Toast.makeText(this, "检查失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun downloadApk(url: String) {
        // 创建进度对话框
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL
        ll.setPadding(50, 50, 50, 50)
        
        val tv = TextView(this)
        tv.text = "正在下载..."
        ll.addView(tv)
        
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        pb.isIndeterminate = true
        ll.addView(pb)
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("下载中")
        builder.setView(ll)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread {
                        dialog.dismiss()
                        Toast.makeText(this, "下载服务器错误: ${connection.responseMessage}", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                
                val length = connection.contentLength
                if (length > 0) {
                    runOnUiThread { 
                        pb.isIndeterminate = false
                        pb.max = 100
                    }
                }

                val inputStream = BufferedInputStream(connection.inputStream)
                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                if (file.exists()) file.delete()
                
                val outputStream = FileOutputStream(file)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    outputStream.write(data, 0, count)
                    if (length > 0) {
                        val progress = (total * 100 / length).toInt()
                        runOnUiThread { pb.progress = progress }
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                runOnUiThread {
                    dialog.dismiss()
                    installApk(file)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "下载异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun installApk(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法启动安装: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            // 简单的版本号比较逻辑：移除非数字字符仅保留 . 和 数字 进行比较? 
            // 这里沿用之前的逻辑：移除 'v' 前缀后按点分割
            val v1 = remote.removePrefix("v").split(".")
            val v2 = local.removePrefix("v").split(".") // 处理本地可能带有的 -beta 等后缀
            
            // 注意：这种比较对于 1.0.0-beta 和 1.0.0 可能会有问题，这里简单处理
            // 如果 remote 是 "1.0.0" 而 local 是 "1.0.0-beta"，分割后 v2[2] 可能是 "0-beta" -> toInt 报错
            
            // 优化：只取数字部分
            val cleanV1 = v1.map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val cleanV2 = v2.map { it.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            
            val length = maxOf(cleanV1.size, cleanV2.size)
            for (i in 0 until length) {
                val num1 = if (i < cleanV1.size) cleanV1[i] else 0
                val num2 = if (i < cleanV2.size) cleanV2[i] else 0
                if (num1 > num2) return true
                if (num1 < num2) return false
            }
            // 如果前缀数字都相同，认为没有新版本 (忽略 alpha/beta 后缀的比较复杂度)
        } catch (e: Exception) {
            return false // 解析失败姑且认为没有新版本
        }
        return false
    }
}