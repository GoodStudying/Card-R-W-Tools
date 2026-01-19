package cn.ratnoumi.bcardtools.drive.bambu

import android.graphics.Color
import cn.ratnoumi.bcardtools.drive.mifare.MifareCard
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 返回卡片的16个扇区的KeyA
 */
fun bambuKdf(cardId: ByteArray): List<ByteArray> {
    val master = byteArrayOf(
        0x9a.toByte(), 0x75.toByte(), 0x9c.toByte(), 0xf2.toByte(),
        0xc4.toByte(), 0xf7.toByte(), 0xca.toByte(), 0xff.toByte(),
        0x22.toByte(), 0x2c.toByte(), 0xb9.toByte(), 0x76.toByte(),
        0x9b.toByte(), 0x41.toByte(), 0xbc.toByte(), 0x96.toByte()
    )
    val hkdf = HKDFBytesGenerator(SHA256Digest())
    val parameters = HKDFParameters(cardId, master, "RFID-A\u0000".toByteArray())
    hkdf.init(parameters)
    // Generate 6 keys of 16 bytes each, total 96 bytes
    val output = ByteArray(96)
    hkdf.generateBytes(output, 0, output.size)
    // Split into 6 keys of 16 bytes each
    return output.toList().chunked(6).map { it.toByteArray() }
}

fun getBambuFilament(card: MifareCard): BambuFilamentCard {
    val blocks = card.blocks
    return BambuFilamentCard(
        blocks[0].copyOf(4).toHexString().uppercase(),
        bytesToStr(blocks[1].copyOf(8)),
        bytesToStr(blocks[1].take(8).toByteArray()),
        bytesToStr(blocks[2]),
        bytesToStr(blocks[4]),
        Color.argb(
            blocks[5][3].toInt() and 0xFF,
            blocks[5][0].toInt() and 0xFF,
            blocks[5][1].toInt() and 0xFF,
            blocks[5][2].toInt() and 0xFF
        ),
        bytesToInt(blocks[5].copyOfRange(4, 6)),
        bytesToFloat(blocks[5].copyOfRange(8, 16)),
        bytesToInt(blocks[6].copyOfRange(0, 2)),
        bytesToInt(blocks[6].copyOfRange(2, 4)),
        bytesToInt(blocks[6].copyOfRange(6, 8)),
        bytesToInt(blocks[6].copyOfRange(8, 10)),
        bytesToInt(blocks[6].copyOfRange(10, 12)),
        blocks[8].copyOf(12).toHexString(),
        bytesToFloat(blocks[8].copyOfRange(12, 16)),
        blocks[9].toHexString().uppercase(),
        bytesToInt(blocks[10].copyOfRange(4, 6)).toFloat() / 100,
        convertDateTime(bytesToStr(blocks[12])),
        bytesToInt(blocks[14].copyOfRange(4, 6)),
        card
    )
}

fun bytesToFloat(arr: ByteArray): Float {
    return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).float
}

fun bytesToInt(arr: ByteArray, start: Int = 0): Int {
    return ((arr[start + 1].toInt() and 0xFF) shl 8) + (arr[start].toInt() and 0xFF)
}

fun bytesToStr(arr: ByteArray): String {
    val bytes = if (arr.indexOf(0) != -1) {
        arr.copyOf(arr.indexOf(0))
    } else {
        arr
    }
    return String(bytes, Charsets.US_ASCII)
}

fun convertDateTime(original: String): String {
    // 按下划线分割字符串，得到 ["2023", "11", "06", "12", "16"]
    val parts = original.split("_")
    // 校验格式是否正确（必须包含5个部分：年、月、日、时、分）
    if (parts.size != 5) {
        return original // 格式错误时返回原字符串
    }
    // 拼接为目标格式：年/月/日 时:分
    return "${parts[0]}/${parts[1]}/${parts[2]} ${parts[3]}:${parts[4]}"
}