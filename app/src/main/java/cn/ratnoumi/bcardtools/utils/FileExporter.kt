package cn.ratnoumi.bcardtools.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Path

object FileExporter {
    // 导出多个文件到Download/appname目录
    suspend fun exportFilesToDownload(context: Context, files: List<Pair<Path, ByteArray>>): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                files.forEach { (fileName, content) ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+：通过MediaStore指定子目录
                        exportToSubDirWithMediaStore(context, fileName, content)
                    } else {
                        // Android 9及以下：手动创建子目录
                        exportToSubDirWithFile(context, fileName, content)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // Android 10+：导出到Download/appname（通过MediaStore）
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportToSubDirWithMediaStore(
        context: Context,
        path: Path,
        bytes: ByteArray
    ) {
        // 构建子目录路径：Download/appname
        val subDirPath = "${Environment.DIRECTORY_DOWNLOADS}/${path.parent}"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, path.fileName.toString())
            put(MediaStore.Downloads.MIME_TYPE, getMimeType(path.fileName.toString())) // 自动适配文件类型
            put(MediaStore.Downloads.RELATIVE_PATH, subDirPath) // 关键：指定子目录
            put(MediaStore.Downloads.IS_PENDING, 1) // 标记为"待处理"，避免被媒体扫描干扰
        }

        // 插入文件到MediaStore（自动创建子目录）
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("无法创建文件：${path}")

        // 写入文件内容
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes)
        } ?: throw IOException("无法写入文件：${path}")

        // 取消"待处理"标记，允许媒体扫描
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }

    // Android 9及以下：导出到Download/appname（手动创建子目录）
    private fun exportToSubDirWithFile(
        context: Context,
        path: Path,
        bytes: ByteArray
    ) {
        // 1. 获取Download目录
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // 2. 创建appname子目录
        val subDir = File(downloadDir, path.parent.toString())
        if (!subDir.exists() && !subDir.mkdirs()) { // 递归创建目录（包括父目录）
            throw IOException("无法创建子目录：${subDir.absolutePath}")
        }
        // 3. 在子目录下创建文件
        val file = File(subDir, path.fileName.toString())
        if (file.exists()) {
            file.delete() // 覆盖已有文件（根据需求调整：可提示用户或重命名）
        }
        // 4. 写入内容
        file.writeBytes(bytes)
    }

    // 辅助方法：根据文件名判断MIME类型（更友好的系统识别）
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".mct") -> "text/mct"
            fileName.endsWith(".bin") -> "application/bin"
            else -> "application/octet-stream" // 默认二进制类型
        }
    }
}